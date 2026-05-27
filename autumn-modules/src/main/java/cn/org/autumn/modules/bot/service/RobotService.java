package cn.org.autumn.modules.bot.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.modules.bot.dao.RobotDao;
import cn.org.autumn.modules.bot.dto.RobotCreateResult;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.support.RobotHookEvents;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.entity.User;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.service.UuidNamespaceService;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.site.AccountFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RobotService extends ModuleService<RobotDao, RobotEntity> implements LoopJob.OneDay {

    @Autowired
    @Lazy
    private UuidNamespaceService uuidNamespaceService;

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    @Lazy
    private RobotTokenService robotTokenService;

    @Autowired
    @Lazy
    private AccountFactory accountFactory;

    @Autowired
    @Lazy
    private RobotQuotaService robotQuotaService;

    @Autowired
    @Lazy
    private RobotHookDispatcher robotHookDispatcher;

    @Autowired
    @Lazy
    private RobotHookService robotHookService;

    @Override
    public String ico() {
        return "fa-android";
    }

    public RobotEntity getByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        return baseMapper.getByUuid(uuid);
    }

    public List<RobotEntity> listByOwner(String owner) {
        if (StringUtils.isBlank(owner)) {
            return null;
        }
        return baseMapper.listByOwnerManaged(owner);
    }

    public List<RobotEntity> listAllByOwner(String owner) {
        if (StringUtils.isBlank(owner)) {
            return Collections.emptyList();
        }
        List<RobotEntity> robots = baseMapper.listByOwner(owner);
        return robots == null ? Collections.emptyList() : robots;
    }

    public List<String> listSoftDeletedUuids(String owner) {
        if (StringUtils.isBlank(owner)) {
            return Collections.emptyList();
        }
        List<String> uuids = baseMapper.listUuidsSoftDeletedByOwner(owner);
        return uuids == null ? Collections.emptyList() : uuids;
    }

    public List<String> listUuidsDeletedBefore(Date beforeTime) {
        if (beforeTime == null) {
            return Collections.emptyList();
        }
        List<String> uuids = baseMapper.listUuidsDeletedBefore(beforeTime);
        return uuids == null ? Collections.emptyList() : uuids;
    }

    public int countByOwnerForQuota(String owner) {
        if (StringUtils.isBlank(owner)) {
            return 0;
        }
        return baseMapper.countByOwnerForQuota(owner);
    }

    public int countSoftDeletedByOwner(String owner) {
        if (StringUtils.isBlank(owner)) {
            return 0;
        }
        return baseMapper.countSoftDeletedByOwner(owner);
    }

    public User toUser(RobotEntity robot) {
        if (robot == null) {
            return null;
        }
        User user = new User();
        user.setUuid(robot.getUuid());
        user.setNickname(robot.getName());
        user.setIcon(robot.getIcon());
        user.setStatus(robot.getStatus());
        user.setRobot(true);
        return user;
    }

    public void assertOwner(RobotEntity robot, String loginUuid) throws Exception {
        if (robot == null) {
            throw new CodeException("机器人不存在");
        }
        if (StringUtils.isBlank(loginUuid) || !loginUuid.equals(robot.getOwner())) {
            throw new CodeException("无权操作该机器人");
        }
    }

    public void assertUserManageable(RobotEntity robot) throws Exception {
        if (robot == null) {
            throw new CodeException("机器人不存在");
        }
        if (robot.getStatus() == RobotEntity.STATUS_DELETED) {
            throw new CodeException("机器人已删除，不可恢复");
        }
        if (robot.getStatus() == RobotEntity.STATUS_DESTROYED) {
            throw new CodeException("机器人已销毁");
        }
    }

    public void assertOwnerActive(String ownerUuid) throws Exception {
        SysUserEntity owner = sysUserService.getByUuid(ownerUuid);
        if (owner == null || owner.getStatus() < 1) {
            throw new CodeException("用户不可用");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public RobotCreateResult create(String owner, String name, String description, String icon, Integer tokenExpireDays, String access) throws Exception {
        assertOwnerActive(owner);
        robotQuotaService.assertRobotQuota(owner);
        if (StringUtils.isBlank(name)) {
            throw new CodeException("名称不能为空");
        }
        RobotEntity draft = new RobotEntity();
        draft.setOwner(owner);
        draft.setName(name);
        accountFactory.creating(draft);
        Date now = new Date();
        RobotEntity robot = new RobotEntity();
        robot.setUuid(uuidNamespaceService.allocate());
        robot.setOwner(owner);
        robot.setName(name);
        robot.setDescription(description);
        robot.setIcon(icon);
        robot.setStatus(RobotEntity.STATUS_ACTIVE);
        robot.setAccess(normalizeAccess(access));
        robot.setBlack(false);
        robot.setCreateTime(now);
        robot.setUpdateTime(now);
        insert(robot);
        String plainToken = robotTokenService.insertToken(robot.getUuid(), tokenExpireDays);
        accountFactory.created(robot);
        dispatchHook(robot, RobotHookEvents.ROBOT_CREATED);
        return RobotCreateResult.of(robot, toUser(robot), plainToken);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disable(String robotUuid, String loginUuid) throws Exception {
        RobotEntity robot = requireManageable(robotUuid, loginUuid);
        accountFactory.disabling(robot);
        robot.setStatus(RobotEntity.STATUS_DISABLED);
        robot.setUpdateTime(new Date());
        updateById(robot);
        robotTokenService.revokeByRobot(robotUuid);
        accountFactory.disabled(robot);
        dispatchHook(robot, RobotHookEvents.ROBOT_DISABLED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enable(String robotUuid, String loginUuid) throws Exception {
        RobotEntity robot = requireManageable(robotUuid, loginUuid);
        assertOwnerActive(robot.getOwner());
        robot.setStatus(RobotEntity.STATUS_ACTIVE);
        robot.setUpdateTime(new Date());
        updateById(robot);
        accountFactory.enabling(robot);
        accountFactory.enabled(robot);
        dispatchHook(robot, RobotHookEvents.ROBOT_ENABLED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String robotUuid, String loginUuid) throws Exception {
        RobotEntity robot = getByUuid(robotUuid);
        assertOwner(robot, loginUuid);
        if (robot.getStatus() == RobotEntity.STATUS_DESTROYED) {
            throw new CodeException("机器人已销毁");
        }
        if (robot.getStatus() == RobotEntity.STATUS_DELETED) {
            throw new CodeException("机器人已删除");
        }
        accountFactory.deleting(robot);
        Date now = new Date();
        robot.setStatus(RobotEntity.STATUS_DELETED);
        robot.setDeleteTime(now);
        robot.setUpdateTime(now);
        updateById(robot);
        robotTokenService.revokeByRobot(robotUuid);
        robotHookService.deleteByRobot(robotUuid);
        accountFactory.deleted(robot);
        dispatchHook(robot, RobotHookEvents.ROBOT_DELETED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void destroyByAdministrator(String robotUuid) throws Exception {
        RobotEntity robot = getByUuid(robotUuid);
        if (robot == null) {
            throw new CodeException("机器人不存在");
        }
        if (robot.getStatus() == RobotEntity.STATUS_DESTROYED) {
            throw new CodeException("机器人已销毁");
        }
        if (robot.getStatus() != RobotEntity.STATUS_DELETED) {
            throw new CodeException("仅可销毁已删除的机器人");
        }
        accountFactory.destroying(robot);
        Date now = new Date();
        robot.setStatus(RobotEntity.STATUS_DESTROYED);
        robot.setDestroyTime(now);
        robot.setUpdateTime(now);
        robot.setName("destroyed-" + robot.getUuid());
        updateById(robot);
        robotTokenService.revokeByRobot(robotUuid);
        dispatchHook(robot, RobotHookEvents.ROBOT_DESTROYED);
        robotHookService.deleteByRobot(robotUuid);
        accountFactory.destroyed(robot);
    }

    @Override
    public void onOneDay() {
        try {
            int retentionDays = robotQuotaService.effectiveDeletedRetentionDays();
            int n = purgeExpiredDeletedRobots(retentionDays);
            if (n > 0) {
                log.info("机器人定时销毁：已处理软删除超过 {} 天的记录 {} 条", retentionDays, n);
            }
        } catch (Exception e) {
            log.error("机器人定时销毁任务失败", e);
        }
    }

    public void purgeSoftDeletedForOwner(String owner) {
        if (StringUtils.isBlank(owner)) {
            return;
        }
        for (String uuid : listSoftDeletedUuids(owner)) {
            if (StringUtils.isBlank(uuid)) {
                continue;
            }
            try {
                destroyByAdministrator(uuid);
            } catch (Exception e) {
                log.warn("清理软删机器人失败 uuid={}", uuid, e);
            }
        }
    }

    public void purgeAllRobotsForOwner(String owner) {
        if (StringUtils.isBlank(owner)) {
            return;
        }
        for (RobotEntity robot : listAllByOwner(owner)) {
            if (robot == null || StringUtils.isBlank(robot.getUuid())) {
                continue;
            }
            if (robot.getStatus() == RobotEntity.STATUS_DESTROYED) {
                continue;
            }
            try {
                if (robot.getStatus() == RobotEntity.STATUS_DELETED) {
                    destroyByAdministrator(robot.getUuid());
                } else {
                    delete(robot.getUuid(), owner);
                    destroyByAdministrator(robot.getUuid());
                }
            } catch (Exception e) {
                log.warn("清理机器人失败 uuid={}", robot.getUuid(), e);
            }
        }
        purgeSoftDeletedForOwner(owner);
    }

    public int purgeExpiredDeletedRobots(int retentionDays) {
        if (retentionDays < 1) {
            retentionDays = robotQuotaService.effectiveDeletedRetentionDays();
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -retentionDays);
        Date beforeTime = cal.getTime();
        int n = 0;
        for (String uuid : listUuidsDeletedBefore(beforeTime)) {
            if (StringUtils.isBlank(uuid)) {
                continue;
            }
            try {
                destroyByAdministrator(uuid);
                n++;
            } catch (Exception e) {
                log.warn("定时销毁机器人失败 uuid={}", uuid, e);
            }
        }
        return n;
    }

    @Transactional(rollbackFor = Exception.class)
    public String createToken(String robotUuid, String loginUuid, Integer tokenExpireDays) throws Exception {
        RobotEntity robot = requireActiveManageable(robotUuid, loginUuid);
        assertOwnerActive(robot.getOwner());
        return robotTokenService.createToken(robotUuid, tokenExpireDays);
    }

    @Transactional(rollbackFor = Exception.class)
    public String rotateToken(String robotUuid, String loginUuid, Integer tokenExpireDays) throws Exception {
        RobotEntity robot = requireActiveManageable(robotUuid, loginUuid);
        assertOwnerActive(robot.getOwner());
        return robotTokenService.rotateIssue(robotUuid, tokenExpireDays);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(String robotUuid, String loginUuid, String name, String description, String icon, String access, Boolean black) throws Exception {
        RobotEntity robot = requireManageable(robotUuid, loginUuid);
        if (StringUtils.isNotBlank(name)) {
            robot.setName(name);
        }
        if (description != null) {
            robot.setDescription(description);
        }
        if (icon != null) {
            robot.setIcon(icon);
        }
        if (access != null) {
            robot.setAccess(normalizeAccess(access));
        }
        if (black != null) {
            robot.setBlack(black);
        }
        robot.setUpdateTime(new Date());
        updateById(robot);
    }

    public static String normalizeAccess(String mode) {
        if (StringUtils.isBlank(mode)) {
            return RobotEntity.ACCESS_PRIVATE;
        }
        String m = mode.trim().toLowerCase();
        if (RobotEntity.ACCESS_PUBLIC.equals(m) || RobotEntity.ACCESS_SUBSCRIBE.equals(m)) {
            return m;
        }
        return RobotEntity.ACCESS_PRIVATE;
    }

    public void touchLastUsed(RobotEntity robot) {
        if (robot == null) {
            return;
        }
        robot.setLastUsedTime(new Date());
        robot.setUpdateTime(robot.getLastUsedTime());
        updateById(robot);
    }

    private RobotEntity requireManageable(String robotUuid, String loginUuid) throws Exception {
        RobotEntity robot = getByUuid(robotUuid);
        assertOwner(robot, loginUuid);
        assertUserManageable(robot);
        return robot;
    }

    private RobotEntity requireActiveManageable(String robotUuid, String loginUuid) throws Exception {
        RobotEntity robot = requireManageable(robotUuid, loginUuid);
        if (!robot.isActive()) {
            throw new CodeException("机器人未启用");
        }
        return robot;
    }

    private void dispatchHook(RobotEntity robot, String event) {
        if (robot == null || StringUtils.isBlank(event)) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("owner", robot.getOwner());
        payload.put("name", robot.getName());
        payload.put("status", robot.getStatus());
        robotHookDispatcher.dispatch(robot.getUuid(), event, payload);
    }
}
