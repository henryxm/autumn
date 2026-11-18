package cn.org.autumn.modules.sys.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.database.runtime.WrapperColumns;
import cn.org.autumn.modules.sys.dao.PasswordChangeLogDao;
import cn.org.autumn.modules.sys.dto.PasswordChangeLogView;
import cn.org.autumn.modules.sys.entity.PasswordChangeLogEntity;
import cn.org.autumn.modules.sys.shiro.PasswordHashSupport;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.Uuid;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 登录口令变更审计：落库 previousHash（非明文），并支持历史口令匹配 / 防重用。
 */
@Service
@Slf4j
public class PasswordChangeLogService extends ModuleService<PasswordChangeLogDao, PasswordChangeLogEntity> {

    public static final String ACTION_CHANGE = "CHANGE";
    public static final String ACTION_RESET = "RESET";
    public static final String ACTION_ADMIN_SET = "ADMIN_SET";
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_CLEAR = "CLEAR";
    public static final String ACTION_UPGRADE = "UPGRADE";

    public static final String CHANNEL_SELF = "SELF";
    public static final String CHANNEL_ADMIN = "ADMIN";
    public static final String CHANNEL_FORGOT = "FORGOT";
    public static final String CHANNEL_REGISTER = "REGISTER";
    public static final String CHANNEL_BOOTSTRAP = "BOOTSTRAP";
    public static final String CHANNEL_OAUTH_PROVISION = "OAUTH_PROVISION";
    public static final String CHANNEL_SYSTEM = "SYSTEM";

    /**
     * 自助改密默认检查最近历史条数
     */
    public static final int DEFAULT_HISTORY_LIMIT = 5;

    @Override
    public String ico() {
        return "fa-key";
    }

    @Override
    protected String[][] getMenuItemsInternal() {
        return new String[][]{};
    }

    /**
     * 写入审计行；失败仅 warn，不抛出。
     */
    public void record(String targetUserUuid,
                       String operatorUserUuid,
                       String action,
                       String channel,
                       String previousPasswordHash,
                       String previousSalt,
                       boolean success,
                       String remark) {
        if (StringUtils.isBlank(targetUserUuid) || StringUtils.isBlank(action)) {
            return;
        }
        try {
            PasswordChangeLogEntity row = new PasswordChangeLogEntity();
            row.setUuid(Uuid.uuid());
            row.setTargetUserUuid(targetUserUuid.trim());
            row.setOperatorUserUuid(StringUtils.trimToNull(operatorUserUuid));
            row.setAction(action);
            row.setChannel(StringUtils.defaultIfBlank(channel, CHANNEL_SYSTEM));
            row.setPreviousPasswordHash(StringUtils.trimToNull(previousPasswordHash));
            row.setPreviousSalt(StringUtils.trimToNull(previousSalt));
            row.setSuccess(success);
            row.setRemark(StringUtils.trimToNull(remark));
            row.setCreateTime(new Date());
            fillClientMeta(row);
            insert(row);
        } catch (Exception e) {
            log.warn("Password change audit record failed target={} action={} err={}",
                    targetUserUuid, action, e.getMessage());
        }
    }

    /**
     * 候选明文是否命中该用户最近 {@code limit} 条历史口令（按 previousHash 校验）。
     */
    public boolean matchesHistory(String userUuid, String plainPassword, int limit) {
        if (StringUtils.isBlank(userUuid) || StringUtils.isBlank(plainPassword) || limit <= 0) {
            return false;
        }
        List<PasswordChangeLogEntity> rows = listRecent(userUuid, limit);
        for (PasswordChangeLogEntity row : rows) {
            if (row == null || StringUtils.isBlank(row.getPreviousPasswordHash())) {
                continue;
            }
            if (PasswordHashSupport.matches(plainPassword, row.getPreviousPasswordHash(), row.getPreviousSalt())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 新口令不得与当前口令或最近历史相同；命中则抛 {@link IllegalArgumentException}。
     *
     * @param currentPasswordHash 当前 {@code sys_user.password}（可空则跳过当前口令比对）
     * @param currentSalt         当前 {@code sys_user.salt}（遗留 SHA 用）
     */
    public void assertNotRecentPassword(String userUuid,
                                        String plainPassword,
                                        String currentPasswordHash,
                                        String currentSalt) {
        assertNotRecentPassword(userUuid, plainPassword, currentPasswordHash, currentSalt, DEFAULT_HISTORY_LIMIT);
    }

    public void assertNotRecentPassword(String userUuid,
                                        String plainPassword,
                                        String currentPasswordHash,
                                        String currentSalt,
                                        int limit) {
        if (StringUtils.isNotBlank(currentPasswordHash)
                && PasswordHashSupport.matches(plainPassword, currentPasswordHash, currentSalt)) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }
        if (matchesHistory(userUuid, plainPassword, limit)) {
            throw new IllegalArgumentException("新密码不能与最近使用过的密码相同");
        }
    }

    public List<PasswordChangeLogEntity> listRecent(String userUuid, int limit) {
        if (StringUtils.isBlank(userUuid) || limit <= 0) {
            return List.of();
        }
        int size = Math.min(limit, 100);
        QueryWrapper<PasswordChangeLogEntity> qw = new QueryWrapper<>();
        qw.eq(WrapperColumns.columnInWrapper("target_user_uuid"), userUuid.trim());
        // 只取有 previousHash 的行，避免 CREATE 等空哈希占满「最近 N 次」窗口
        qw.isNotNull(WrapperColumns.columnInWrapper("previous_password_hash"));
        qw.ne(WrapperColumns.columnInWrapper("previous_password_hash"), "");
        qw.orderByDesc(WrapperColumns.columnInWrapper("create_time"));
        // 方言安全分页：勿用 QueryWrapper.last("LIMIT …")
        Page<PasswordChangeLogEntity> page = page(new Page<>(1, size), qw);
        return page.getRecords() == null ? List.of() : page.getRecords();
    }

    /**
     * 管理端分页列表；视图不含哈希/盐。
     */
    public Page<PasswordChangeLogView> queryPageViews(Map<String, Object> params) {
        String targetUserUuid = params == null ? null : (String) params.get("targetUserUuid");
        long page = 1L;
        long limit = 10L;
        if (params != null) {
            Object p = params.get("page");
            Object l = params.get("limit");
            if (p != null) {
                try {
                    page = Long.parseLong(String.valueOf(p));
                } catch (NumberFormatException ignored) {
                }
            }
            if (l != null) {
                try {
                    limit = Long.parseLong(String.valueOf(l));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (page < 1) {
            page = 1;
        }
        if (limit < 1) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }
        QueryWrapper<PasswordChangeLogEntity> qw = new QueryWrapper<>();
        if (StringUtils.isNotBlank(targetUserUuid)) {
            qw.eq(WrapperColumns.columnInWrapper("target_user_uuid"), targetUserUuid.trim());
        }
        qw.orderByDesc(WrapperColumns.columnInWrapper("create_time"));
        Page<PasswordChangeLogEntity> entityPage = page(new Page<>(page, limit), qw);
        Page<PasswordChangeLogView> viewPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<PasswordChangeLogView> views = new ArrayList<>();
        for (PasswordChangeLogEntity e : entityPage.getRecords()) {
            views.add(toView(e));
        }
        viewPage.setRecords(views);
        return viewPage;
    }

    public PasswordChangeLogView toView(PasswordChangeLogEntity e) {
        PasswordChangeLogView v = new PasswordChangeLogView();
        if (e == null) {
            return v;
        }
        v.setUuid(e.getUuid());
        v.setTargetUserUuid(e.getTargetUserUuid());
        v.setOperatorUserUuid(e.getOperatorUserUuid());
        v.setAction(e.getAction());
        v.setChannel(e.getChannel());
        v.setSuccess(e.isSuccess());
        v.setIp(e.getIp());
        v.setUserAgent(e.getUserAgent());
        v.setRemark(e.getRemark());
        v.setCreateTime(e.getCreateTime());
        return v;
    }

    private void fillClientMeta(PasswordChangeLogEntity row) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            if (request == null) {
                return;
            }
            row.setIp(IPUtils.getIp(request));
            String ua = request.getHeader("User-Agent");
            if (StringUtils.isNotBlank(ua) && ua.length() > 256) {
                ua = ua.substring(0, 256);
            }
            row.setUserAgent(ua);
        } catch (Exception e) {
            // 无 HTTP 上下文时忽略
        }
    }
}
