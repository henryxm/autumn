package cn.org.autumn.modules.usr.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.exception.AException;
import cn.org.autumn.model.IP;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.usr.dao.UserLoginLogDao;
import cn.org.autumn.modules.usr.entity.UserLoginLogEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Slf4j
@Service
public class UserLoginLogService extends ModuleService<UserLoginLogDao, UserLoginLogEntity> implements LoopJob.OneDay {
    @Override
    public String ico() {
        return "fa-sun-o";
    }

    /**
     * 分页查询，支持按 uuid、ip、host、way、allow、logout、limit、createStart/createEnd 筛选
     * <p>
     * - uuid、ip、account、host、reason、agent、session、path：支持模糊（like）
     * - way：精确匹配
     * - allow、logout、limit：传入 "true"/"false" 或 1/0 时作为布尔条件
     * - createStart、createEnd：日期范围，格式 yyyy-MM-dd 或时间戳
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        try {
            Page<UserLoginLogEntity> _page = new Query<UserLoginLogEntity>(params).getPage();
            EntityWrapper<UserLoginLogEntity> ew = new EntityWrapper<>();
            String uuid = mapStr(params, "uuid");
            String ip = mapStr(params, "ip");
            String account = mapStr(params, "account");
            String way = mapStr(params, "way");
            String host = mapStr(params, "host");
            String session = mapStr(params, "session");
            String path = mapStr(params, "path");
            String reason = mapStr(params, "reason");
            String agent = mapStr(params, "agent");
            Object allowObj = params.get("allow");
            Object logoutObj = params.get("logout");
            Object whiteObj = params.get("white");
            String createStart = mapStr(params, "createStart");
            String createEnd = mapStr(params, "createEnd");
            ew.like(StringUtils.isNotBlank(uuid), "uuid", uuid);
            ew.like(StringUtils.isNotBlank(ip), "ip", ip);
            ew.like(StringUtils.isNotBlank(account), "account", account);
            ew.eq(StringUtils.isNotBlank(way), "way", way);
            ew.like(StringUtils.isNotBlank(host), "host", host);
            ew.like(StringUtils.isNotBlank(session), "session", session);
            ew.like(StringUtils.isNotBlank(path), "path", path);
            ew.like(StringUtils.isNotBlank(reason), "reason", reason);
            ew.like(StringUtils.isNotBlank(agent), "agent", agent);
            if (allowObj != null && StringUtils.isNotBlank(allowObj.toString())) {
                boolean allow = "true".equalsIgnoreCase(allowObj.toString()) || "1".equals(allowObj.toString());
                ew.eq("allow", allow);
            }
            if (logoutObj != null && StringUtils.isNotBlank(logoutObj.toString())) {
                boolean logout = "true".equalsIgnoreCase(logoutObj.toString()) || "1".equals(logoutObj.toString());
                ew.eq("logout", logout);
            }
            if (whiteObj != null && StringUtils.isNotBlank(whiteObj.toString())) {
                boolean limit = "true".equalsIgnoreCase(whiteObj.toString()) || "1".equals(whiteObj.toString());
                ew.eq("`white`", limit);
            }
            if (StringUtils.isNotBlank(createStart)) {
                try {
                    Date start = parseDate(createStart);
                    if (start != null) ew.ge("`create`", start);
                } catch (Exception ignored) {
                }
            }
            if (StringUtils.isNotBlank(createEnd)) {
                try {
                    Date end = parseDate(createEnd);
                    if (end != null) ew.le("`create`", end);
                } catch (Exception ignored) {
                }
            }
            ew.orderBy("`create`", false);
            Page<UserLoginLogEntity> page = selectPage(_page, ew);
            page.setTotal(baseMapper.selectCount(ew));
            return new PageUtils(page);
        } catch (Exception e) {
            log.error("查询错误:{}", e.getMessage());
            return null;
        }
    }

    private static String mapStr(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString().trim() : "";
    }

    private static Date parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        s = s.trim();
        try {
            if (s.matches("^\\d+$")) return new Date(Long.parseLong(s));
            return java.sql.Date.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- 以下方法供其它模块按 uuid、ip 查询或统计，便于复用 ----------

    /**
     * 按用户 uuid 分页查询，按创建时间倒序
     */
    public PageUtils queryByUuid(String uuid, int page, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("uuid", uuid);
        params.put("page", String.valueOf(page));
        params.put("limit", String.valueOf(limit));
        return queryPage(params);
    }

    /**
     * 按 IP 分页查询，按创建时间倒序
     */
    public PageUtils queryByIp(String ip, int page, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("ip", ip);
        params.put("page", String.valueOf(page));
        params.put("limit", String.valueOf(limit));
        return queryPage(params);
    }

    /**
     * 按 uuid 与 ip 组合分页查询
     */
    public PageUtils queryByUuidAndIp(String uuid, String ip, int page, int limit) {
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.isNotBlank(uuid)) params.put("uuid", uuid);
        if (StringUtils.isNotBlank(ip)) params.put("ip", ip);
        params.put("page", String.valueOf(page));
        params.put("limit", String.valueOf(limit));
        return queryPage(params);
    }

    /**
     * 某用户最近若干条登录/登出记录，按创建时间倒序
     */
    public List<UserLoginLogEntity> listRecentByUuid(String uuid, int limit) {
        EntityWrapper<UserLoginLogEntity> ew = new EntityWrapper<>();
        ew.eq("uuid", uuid).orderBy("`create`", false);
        Page<UserLoginLogEntity> p = new Page<>(1, Math.max(1, Math.min(limit, 500)));
        return selectPage(p, ew).getRecords();
    }

    /**
     * 某 IP 最近若干条记录，按创建时间倒序
     */
    public List<UserLoginLogEntity> listRecentByIp(String ip, int limit) {
        EntityWrapper<UserLoginLogEntity> ew = new EntityWrapper<>();
        ew.eq("ip", ip).orderBy("`create`", false);
        Page<UserLoginLogEntity> p = new Page<>(1, Math.max(1, Math.min(limit, 500)));
        return selectPage(p, ew).getRecords();
    }

    /**
     * 统计某用户的登录/登出记录数
     */
    public long countByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) return 0;
        EntityWrapper<UserLoginLogEntity> ew = new EntityWrapper<>();
        ew.eq("uuid", uuid);
        return baseMapper.selectCount(ew);
    }

    /**
     * 统计某 IP 的记录数
     */
    public long countByIp(String ip) {
        if (StringUtils.isBlank(ip)) return 0;
        EntityWrapper<UserLoginLogEntity> ew = new EntityWrapper<>();
        ew.eq("ip", ip);
        return baseMapper.selectCount(ew);
    }

    // ---------- 清理：按条件删除、按天数定时清理 ----------

    /**
     * 按条件删除，条件与 queryPage 一致（uuid、ip、account、host、way、reason、agent、allow、logout、createStart/createEnd）。
     * 至少需指定一项条件，否则返回 0 不执行删除。
     *
     * @return 删除条数
     */
    public int deleteByParams(Map<String, Object> params) {
        EntityWrapper<UserLoginLogEntity> ew = new EntityWrapper<>();
        String uuid = mapStr(params, "uuid");
        String ip = mapStr(params, "ip");
        String account = mapStr(params, "account");
        String way = mapStr(params, "way");
        String host = mapStr(params, "host");
        String session = mapStr(params, "session");
        String path = mapStr(params, "path");
        String reason = mapStr(params, "reason");
        String agent = mapStr(params, "agent");
        Object allowObj = params.get("allow");
        Object logoutObj = params.get("logout");
        Object limitObj = params.get("limit");
        String createStart = mapStr(params, "createStart");
        String createEnd = mapStr(params, "createEnd");
        ew.like(StringUtils.isNotBlank(uuid), "uuid", uuid);
        ew.like(StringUtils.isNotBlank(ip), "ip", ip);
        ew.like(StringUtils.isNotBlank(account), "account", account);
        ew.eq(StringUtils.isNotBlank(way), "way", way);
        ew.like(StringUtils.isNotBlank(host), "host", host);
        ew.like(StringUtils.isNotBlank(session), "session", session);
        ew.like(StringUtils.isNotBlank(path), "path", path);
        ew.like(StringUtils.isNotBlank(reason), "reason", reason);
        ew.like(StringUtils.isNotBlank(agent), "agent", agent);
        if (allowObj != null && StringUtils.isNotBlank(allowObj.toString())) {
            boolean allow = "true".equalsIgnoreCase(allowObj.toString()) || "1".equals(allowObj.toString());
            ew.eq("allow", allow);
        }
        if (logoutObj != null && StringUtils.isNotBlank(logoutObj.toString())) {
            boolean logout = "true".equalsIgnoreCase(logoutObj.toString()) || "1".equals(logoutObj.toString());
            ew.eq("logout", logout);
        }
        if (limitObj != null && StringUtils.isNotBlank(limitObj.toString())) {
            boolean limit = "true".equalsIgnoreCase(limitObj.toString()) || "1".equals(limitObj.toString());
            ew.eq("`limit`", limit);
        }
        if (StringUtils.isNotBlank(createStart)) {
            try {
                Date start = parseDate(createStart);
                if (start != null) ew.ge("`create`", start);
            } catch (Exception ignored) {
            }
        }
        if (StringUtils.isNotBlank(createEnd)) {
            try {
                Date end = parseDate(createEnd);
                if (end != null) ew.le("`create`", end);
            } catch (Exception ignored) {
            }
        }
        boolean hasCondition = StringUtils.isNotBlank(uuid) || StringUtils.isNotBlank(ip) || StringUtils.isNotBlank(account)
                || StringUtils.isNotBlank(way) || StringUtils.isNotBlank(host) || StringUtils.isNotBlank(session) || StringUtils.isNotBlank(path)
                || StringUtils.isNotBlank(reason) || StringUtils.isNotBlank(agent)
                || (allowObj != null && StringUtils.isNotBlank(allowObj.toString()))
                || (logoutObj != null && StringUtils.isNotBlank(logoutObj.toString()))
                || (limitObj != null && StringUtils.isNotBlank(limitObj.toString()))
                || StringUtils.isNotBlank(createStart) || StringUtils.isNotBlank(createEnd);
        if (!hasCondition) {
            return 0;
        }
        return baseMapper.delete(ew);
    }

    /**
     * 删除创建时间早于指定天数的记录。用于定时清理过期日志。
     *
     * @param days 天数，创建时间早于 (now - days) 的记录将被删除
     * @return 删除条数
     */
    public int deleteOlderThanDays(int days) {
        if (days <= 0) return 0;
        long cutoff = System.currentTimeMillis() - days * 24L * 3600 * 1000;
        Date before = new Date(cutoff);
        EntityWrapper<UserLoginLogEntity> ew = new EntityWrapper<>();
        ew.lt("`create`", before);
        int n = baseMapper.delete(ew);
        if (n > 0 && log.isInfoEnabled()) {
            log.info("定时清理登录日志：删除 {} 天以前的记录 {} 条", days, n);
        }
        return n;
    }

    @Override
    public void onOneDay() {
        deleteOlderThanDays(30);
    }

    public void login(String uuid, String account, String way, String reason, HttpServletRequest request) {
        login(uuid, account, true, way, reason, request);
    }

    public void login(String uuid, String account, boolean allow, String way, String reason, HttpServletRequest request) {
        String ip = "";
        String agent = "";
        String host = "";
        String session = "";
        String path = "";
        if (null != request) {
            ip = IP.getIp(request);
            agent = request.getHeader("user-agent");
            host = request.getHeader("host");
            session = request.getSession().getId();
            path = request.getServletPath();
        }
        login(uuid, account, allow, way, reason, host, ip, session, path, agent);
    }

    public void login(String uuid, String account, boolean allow, String way, String reason, String host, String ip, String session, String path, String agent) {
        try {
            if (!isIpAllowed(uuid, ip))
                allow = false;
            UserLoginLogEntity entity = new UserLoginLogEntity();
            entity.setUuid(uuid);
            entity.setAccount(account);
            entity.setLogout(false);
            entity.setAllow(allow);
            entity.setWhite(false);
            entity.setHost(host);
            entity.setIp(ip);
            entity.setPath(path);
            entity.setSession(session);
            entity.setAgent(agent);
            entity.setWay(way);
            entity.setReason(reason);
            entity.setCreate(new Date());
            insert(entity);
        } catch (Throwable e) {
            log.error("登录错误:{}, 账号:{}, 允许:{}, 方式:{}, 原因:{}, IP:{}, 代理:{}", uuid, account, allow, way, reason, ip, agent);
        }
        if (!allow) {
            throw new AException("登录限制");
        }
    }

    /**
     * 按用户 uuid 与 limit=true 查询出允许登录的 IP 列表。列表为空表示不限制；不为空表示仅这些 IP 可登录。
     */
    public Set<String> getAllowedIpsByUserUuid(String uuid) {
        if (StringUtils.isBlank(uuid))
            return new HashSet<>();
        EntityWrapper<UserLoginLogEntity> ew = new EntityWrapper<>();
        ew.eq("uuid", uuid).eq("`white`", true);
        List<UserLoginLogEntity> list = selectList(ew);
        Set<String> ips = new HashSet<>();
        for (UserLoginLogEntity e : list) {
            if (e.getIp() != null && !e.getIp().trim().isEmpty()) ips.add(e.getIp().trim());
        }
        return ips;
    }

    /**
     * 当前 IP 是否允许该用户登录。根据 uuid 与 limit=true 查询允许的 IP 列表：空表示不限制；否则当前 IP 须在列表中。
     */
    public boolean isIpAllowed(String uuid, String ip) {
        Set<String> allowed = getAllowedIpsByUserUuid(uuid);
        if (allowed.isEmpty())
            return true;
        if (ip == null || ip.trim().isEmpty())
            return false;
        return allowed.contains(ip.trim());
    }

    public void login(UserProfileEntity userProfileEntity, String way, String reason, HttpServletRequest request) {
        login(userProfileEntity.getUuid(), "", true, way, reason, request);
    }

    public void logout(String uuid, HttpServletRequest request) {
        String ip = null != request ? IP.getIp(request) : "";
        String agent = null != request ? request.getHeader("user-agent") : "";
        String host = null != request ? request.getHeader("host") : "";
        String session = null != request ? request.getSession().getId() : "";
        logout(uuid, host, ip, session, agent);
    }

    public void logout(String uuid, String host, String ip, String session, String agent) {
        try {
            UserLoginLogEntity entity = new UserLoginLogEntity();
            entity.setUuid(uuid);
            entity.setCreate(new Date());
            entity.setAllow(true);
            entity.setWhite(false);
            entity.setLogout(true);
            entity.setHost(host);
            entity.setIp(ip);
            entity.setSession(session);
            entity.setAgent(agent);
            insert(entity);
        } catch (Throwable e) {
            log.error("登录错误:{}, IP:{}, 代理:{}", uuid, ip, agent);
        }
    }
}
