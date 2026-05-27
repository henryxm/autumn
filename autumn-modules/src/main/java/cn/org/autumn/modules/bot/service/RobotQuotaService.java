package cn.org.autumn.modules.bot.service;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.RobotQuotaConfig;
import cn.org.autumn.modules.bot.entity.RobotConfigEntity;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class RobotQuotaService {

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    @Autowired
    @Lazy
    private RobotConfigService robotConfigService;

    @Autowired
    @Lazy
    private RobotService robotService;

    @Autowired
    @Lazy
    private RobotHookService robotHookService;

    public RobotQuotaConfig getGlobal() {
        RobotQuotaConfig config = sysConfigService.getConfigObjectValidate(RobotQuotaConfig.CONFIG_KEY, RobotQuotaConfig.class);
        return config == null ? new RobotQuotaConfig() : config;
    }

    public int effectiveMaxSoftDeletePending() {
        return getGlobal().getMaxSoftDeletePending();
    }

    public int effectiveDeletedRetentionDays() {
        return getGlobal().getDeletedRetentionDays();
    }

    public int effectiveMaxRobotsPerUser(String userUuid) {
        RobotConfigEntity config = configOfUser(userUuid);
        if (config != null && config.getMaxRobots() != RobotConfigEntity.INHERIT) {
            return config.getMaxRobots();
        }
        return getGlobal().getMaxRobotsPerUser();
    }

    public int effectiveMaxTokensPerRobot(String userUuid) {
        RobotConfigEntity config = configOfUser(userUuid);
        if (config != null && config.getMaxTokens() != RobotConfigEntity.INHERIT) {
            return config.getMaxTokens();
        }
        return getGlobal().getMaxTokensPerRobot();
    }

    public int effectiveMaxHooksPerRobot(String userUuid) {
        RobotConfigEntity config = configOfUser(userUuid);
        if (config != null && config.getMaxHooks() != RobotConfigEntity.INHERIT) {
            return config.getMaxHooks();
        }
        return getGlobal().getMaxHooksPerRobot();
    }

    public int effectiveMaxTokens(String robotUuid) {
        String owner = ownerOfRobot(robotUuid);
        if (StringUtils.isBlank(owner)) {
            return getGlobal().getMaxTokensPerRobot();
        }
        return effectiveMaxTokensPerRobot(owner);
    }

    public int effectiveMaxHooks(String robotUuid) {
        String owner = ownerOfRobot(robotUuid);
        if (StringUtils.isBlank(owner)) {
            return getGlobal().getMaxHooksPerRobot();
        }
        return effectiveMaxHooksPerRobot(owner);
    }

    public void assertRobotQuota(String owner) throws Exception {
        if (StringUtils.isBlank(owner)) {
            return;
        }
        assertSoftDeleteCreateAllowed(owner);
        int max = effectiveMaxRobotsPerUser(owner);
        int count = robotService.countByOwnerForQuota(owner);
        if (count >= max) {
            throw new CodeException("已达机器人数量上限:" + max);
        }
    }

    public void assertSoftDeleteCreateAllowed(String owner) throws Exception {
        if (StringUtils.isBlank(owner)) {
            return;
        }
        int limit = effectiveMaxSoftDeletePending();
        int pending = robotService.countSoftDeletedByOwner(owner);
        if (pending > limit) {
            throw softDeleteGateException(pending, limit);
        }
    }

    public boolean isSoftDeleteCreateBlocked(String owner) {
        if (StringUtils.isBlank(owner)) {
            return false;
        }
        return robotService.countSoftDeletedByOwner(owner) > effectiveMaxSoftDeletePending();
    }

    public void assertHookQuota(String robotUuid) throws Exception {
        int max = effectiveMaxHooks(robotUuid);
        int count = robotHookService.countByRobot(robotUuid);
        if (count >= max) {
            throw new CodeException("已达Hook数量上限:" + max);
        }
    }

    private CodeException softDeleteGateException(int pending, int limit) {
        int retention = effectiveDeletedRetentionDays();
        String msg = "软删除中的机器人过多(" + pending + "个)，暂不可创建。请将软删数量降至" + limit + "个及以下（联系管理员物理销毁或等待" + retention + "天自动清理）";
        return new CodeException(msg);
    }

    private RobotConfigEntity configOfUser(String userUuid) {
        return robotConfigService.getByUser(userUuid);
    }

    private String ownerOfRobot(String robotUuid) {
        RobotEntity robot = robotService.getByUuid(robotUuid);
        return robot == null ? null : robot.getOwner();
    }
}
