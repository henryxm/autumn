package cn.org.autumn.modules.safe.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.safe.dao.PayGateAttemptDao;
import cn.org.autumn.modules.safe.entity.PayGateAttemptEntity;
import cn.org.autumn.utils.Uuid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Slf4j
@Service
public class PayGateAttemptService extends ModuleService<PayGateAttemptDao, PayGateAttemptEntity> implements LoopJob.OneDay {

    @Override
    public String ico() {
        return "fa-filter";
    }

    public int countAuthorizedSameAmountSince(String userUuid, long amountCent, Date since) {
        return baseMapper.countSameAmountSince(userUuid, amountCent, since);
    }

    public int countByOrderIdSince(String userUuid, String orderId, Date since) {
        if (StringUtils.isBlank(orderId))
            return 0;
        return baseMapper.countByOrderIdSince(userUuid, orderId, since);
    }

    public int countPasswordlessSince(String userUuid, Date since) {
        return baseMapper.countPasswordlessSince(userUuid, since);
    }

    public long sumPasswordlessAmountSince(String userUuid, Date since) {
        return baseMapper.sumPasswordlessAmountSince(userUuid, since);
    }

    public Date startOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public void record(PayGateAttemptEntity entity) {
        if (entity.getUuid() == null)
            entity.setUuid(Uuid.uuid());
        if (entity.getCreateTime() == null)
            entity.setCreateTime(new Date());
        insert(entity);
    }

    @Override
    public void onOneDay() {
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -30);
            int n = baseMapper.deleteOlderThan(cal.getTime());
            if (n > 0)
                log.info("支付闸门评估记录清理：删除 {} 条", n);
        } catch (Exception e) {
            log.error("支付闸门评估记录清理失败", e);
        }
    }
}
