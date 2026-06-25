package cn.org.autumn.modules.safe.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.safe.dao.PayCredentialLogDao;
import cn.org.autumn.modules.safe.entity.PayCredentialLogEntity;
import cn.org.autumn.modules.safe.site.SafeConfig;
import cn.org.autumn.modules.safe.support.PayCredentialVerifyMethods;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.Uuid;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class PayCredentialLogService extends ModuleService<PayCredentialLogDao, PayCredentialLogEntity> implements LoopJob.OneDay {

    public static final String METHOD_PIN = PayCredentialVerifyMethods.PIN;
    public static final String METHOD_GESTURE = PayCredentialVerifyMethods.GESTURE;
    public static final String METHOD_BIO = PayCredentialVerifyMethods.BIO;
    public static final String METHOD_GATE = PayCredentialVerifyMethods.GATE;

    @Autowired
    private SafeConfig safeConfig;

    @Override
    public String ico() {
        return "fa-shield";
    }

    public void append(String userUuid, String action, String method, boolean success, String remark, HttpServletRequest request) {
        if (StringUtils.isBlank(userUuid))
            return;
        PayCredentialConfig config = safeConfig.get();
        if (!config.isAuditLogEnabled())
            return;
        try {
            PayCredentialLogEntity row = new PayCredentialLogEntity();
            row.setUuid(Uuid.uuid());
            row.setUserUuid(userUuid);
            row.setAction(action);
            row.setMethod(method);
            row.setSuccess(success);
            row.setRemark(remark);
            row.setCreateTime(new Date());
            if (request != null) {
                row.setIp(IPUtils.getIp(request));
                row.setUserAgent(request.getHeader("User-Agent"));
            }
            insert(row);
        } catch (Exception ignored) {
            // 审计失败不阻断主流程
        }
    }

    public void appendGateAssess(String userUuid, boolean authorized, String authMode, long amountCent, String orderId, List<String> reasons, HttpServletRequest request) {
        PayCredentialConfig config = safeConfig.get();
        if (!config.isAuditGateEnabled() || !config.isAuditLogEnabled())
            return;
        String remark = "mode=" + authMode + ",amount=" + amountCent;
        if (StringUtils.isNotBlank(orderId))
            remark = remark + ",order=" + orderId;
        if (reasons != null && !reasons.isEmpty())
            remark = remark + ",reasons=" + StringUtils.join(reasons, ";");
        append(userUuid, authorized ? "ASSESS" : "DENY", METHOD_GATE, authorized, remark, request);
    }

    public int deleteOlderThanDays(int days) {
        if (days <= 0)
            return 0;
        long cutoff = System.currentTimeMillis() - days * 24L * 3600 * 1000;
        Date before = new Date(cutoff);
        EntityWrapper<PayCredentialLogEntity> ew = new EntityWrapper<>();
        ew.lt(columnInWrapper("create_time"), before);
        int n = baseMapper.delete(ew);
        if (n > 0)
            log.debug("支付凭证操作日志清理：删除 {} 天以前的记录 {} 条", days, n);
        return n;
    }

    @Override
    public void onOneDay() {
        try {
            PayCredentialConfig config = safeConfig.get();
            int days = config.getLogRetentionDays();
            if (days <= 0)
                return;
            deleteOlderThanDays(days);
        } catch (Exception e) {
            log.error("支付凭证操作日志定时清理失败", e);
        }
    }
}
