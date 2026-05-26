package cn.org.autumn.modules.bot.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.modules.bot.dao.RobotConfigDao;
import cn.org.autumn.modules.bot.dto.RobotConfigResult;
import cn.org.autumn.modules.bot.entity.RobotConfigEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.service.CacheService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class RobotConfigService extends ModuleService<RobotConfigDao, RobotConfigEntity> {

    private static final CacheConfig USER_CONFIG_CACHE = CacheConfig.builder()
            .name("bot_robot_config_user")
            .key(String.class)
            .value(RobotConfigEntity.class)
            .expire(10)
            .unit(TimeUnit.MINUTES)
            .redis(30)
            .build();

    @Autowired
    @Lazy
    private RobotQuotaService robotQuotaService;

    @Autowired
    @Lazy
    private RobotService robotService;

    @Autowired
    @Lazy
    private SysUserRoleService sysUserRoleService;

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    private CacheService cacheService;

    @Override
    public String ico() {
        return "fa-sliders";
    }

    public RobotConfigEntity getByUser(String userUuid) {
        if (StringUtils.isBlank(userUuid))
            return null;
        return cacheService.compute(userUuid, () -> baseMapper.getByUuid(userUuid), USER_CONFIG_CACHE);
    }

    public RobotConfigResult getEffective(String userUuid, String operatorUuid) throws Exception {
        String target = resolveTargetUser(userUuid, operatorUuid);
        assertViewPermission(target, operatorUuid);
        return buildResult(target);
    }

    public void assertAdministrator(String operatorUuid) throws Exception {
        if (StringUtils.isBlank(operatorUuid))
            throw new CodeException("请登录", -10000);
        if (!sysUserRoleService.isSystemAdministrator(operatorUuid))
            throw new CodeException("需要系统管理员权限");
    }

    @Transactional(rollbackFor = Exception.class)
    public RobotConfigResult save(String operatorUuid, String userUuid, Integer maxRobots, Integer maxTokens, Integer maxHooks) throws Exception {
        assertAdministrator(operatorUuid);
        if (StringUtils.isBlank(userUuid))
            throw new CodeException("用户uuid不能为空");
        SysUserEntity user = sysUserService.getByUuid(userUuid);
        if (user == null)
            throw new CodeException("用户不存在");
        if (maxRobots == null && maxTokens == null && maxHooks == null)
            throw new CodeException("请至少指定一项配额");
        Date now = new Date();
        RobotConfigEntity config = baseMapper.getByUuid(userUuid);
        if (config == null) {
            int robots = maxRobots == null ? RobotConfigEntity.INHERIT : maxRobots;
            int tokens = maxTokens == null ? RobotConfigEntity.INHERIT : maxTokens;
            int hooks = maxHooks == null ? RobotConfigEntity.INHERIT : maxHooks;
            assertQuotaValue("机器人数", robots);
            assertQuotaValue("令牌数", tokens);
            assertQuotaValue("Hook数", hooks);
            config = new RobotConfigEntity();
            config.setUuid(userUuid);
            config.setMaxRobots(robots);
            config.setMaxTokens(tokens);
            config.setMaxHooks(hooks);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            insert(config);
        } else {
            if (maxRobots != null) {
                assertQuotaValue("机器人数", maxRobots);
                config.setMaxRobots(maxRobots);
            }
            if (maxTokens != null) {
                assertQuotaValue("令牌数", maxTokens);
                config.setMaxTokens(maxTokens);
            }
            if (maxHooks != null) {
                assertQuotaValue("Hook数", maxHooks);
                config.setMaxHooks(maxHooks);
            }
            config.setUpdateTime(now);
            updateById(config);
        }
        evictUser(userUuid);
        return buildResult(userUuid);
    }

    public void evictUser(String userUuid) {
        if (StringUtils.isBlank(userUuid))
            return;
        cacheService.remove(USER_CONFIG_CACHE.getName(), userUuid);
    }

    private void assertQuotaValue(String label, int value) throws CodeException {
        if (value != RobotConfigEntity.INHERIT && value <= 0)
            throw new CodeException(label + "须大于0或-1继承全局");
    }

    private String resolveTargetUser(String userUuid, String operatorUuid) throws CodeException {
        if (StringUtils.isNotBlank(userUuid))
            return userUuid;
        if (StringUtils.isBlank(operatorUuid))
            throw new CodeException("请登录", -10000);
        return operatorUuid;
    }

    private void assertViewPermission(String targetUserUuid, String operatorUuid) throws Exception {
        if (StringUtils.isBlank(operatorUuid))
            throw new CodeException("请登录", -10000);
        if (targetUserUuid.equals(operatorUuid))
            return;
        if (!sysUserRoleService.isSystemAdministrator(operatorUuid))
            throw new CodeException("无权查看该用户配额");
    }

    private RobotConfigResult buildResult(String userUuid) {
        RobotConfigEntity config = getByUser(userUuid);
        RobotConfigResult result = new RobotConfigResult();
        result.setUuid(userUuid);
        result.setMaxRobots(config == null ? RobotConfigEntity.INHERIT : config.getMaxRobots());
        result.setMaxTokens(config == null ? RobotConfigEntity.INHERIT : config.getMaxTokens());
        result.setMaxHooks(config == null ? RobotConfigEntity.INHERIT : config.getMaxHooks());
        result.setUsedRobots(robotService.countByOwner(userUuid));
        result.setEffectiveMaxRobots(robotQuotaService.effectiveMaxRobotsPerUser(userUuid));
        result.setEffectiveMaxTokens(robotQuotaService.effectiveMaxTokensPerRobot(userUuid));
        result.setEffectiveMaxHooks(robotQuotaService.effectiveMaxHooksPerRobot(userUuid));
        return result;
    }
}
