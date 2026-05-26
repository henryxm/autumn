package cn.org.autumn.modules.bot.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.modules.bot.dao.RobotDao;
import cn.org.autumn.modules.bot.dto.RobotCreateResult;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.entity.User;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.service.UuidNamespaceService;
import cn.org.autumn.modules.bot.support.RobotHookEvents;
import cn.org.autumn.site.AccountFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RobotService extends ModuleService<RobotDao, RobotEntity> {

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
        if (StringUtils.isBlank(uuid))
            return null;
        return baseMapper.getByUuid(uuid);
    }

    public List<RobotEntity> listByOwner(String owner) {
        if (StringUtils.isBlank(owner))
            return null;
        return baseMapper.listByOwnerManaged(owner);
    }

    public int countByOwner(String owner) {
        if (StringUtils.isBlank(owner))
            return 0;
        return baseMapper.countByOwner(owner);
    }

    public User toUser(RobotEntity robot) {
        if (robot == null)
            return null;
        User user = new User();
        user.setUuid(robot.getUuid());
        user.setNickname(robot.getName());
        user.setIcon(robot.getIcon());
        user.setStatus(robot.getStatus());
        user.setRobot(true);
        return user;
    }

    public void assertOwner(RobotEntity robot, String loginUuid) throws Exception {
        if (robot == null)
            throw new CodeException("机器人不存在");
        if (StringUtils.isBlank(loginUuid) || !loginUuid.equals(robot.getOwner()))
            throw new CodeException("无权操作该机器人");
    }

    public void assertOwnerActive(String ownerUuid) throws Exception {
        SysUserEntity owner = sysUserService.getByUuid(ownerUuid);
        if (owner == null || owner.getStatus() < 1)
            throw new CodeException("用户不可用");
    }

    @Transactional(rollbackFor = Exception.class)
    public RobotCreateResult create(String owner, String name, String description, String icon, Integer tokenExpireDays, String access) throws Exception {
        assertOwnerActive(owner);
        robotQuotaService.assertRobotQuota(owner);
        if (StringUtils.isBlank(name))
            throw new CodeException("名称不能为空");
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
        RobotEntity robot = getByUuid(robotUuid);
        assertOwner(robot, loginUuid);
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
        RobotEntity robot = getByUuid(robotUuid);
        assertOwner(robot, loginUuid);
        if (robot.getStatus() == RobotEntity.STATUS_DESTROYED || robot.getStatus() == RobotEntity.STATUS_DELETED)
            throw new CodeException("机器人已删除或已销毁");
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
        if (robot.getStatus() == RobotEntity.STATUS_DESTROYED)
            throw new CodeException("机器人已销毁");
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
    public void destroy(String robotUuid, String loginUuid) throws Exception {
        RobotEntity robot = getByUuid(robotUuid);
        assertOwner(robot, loginUuid);
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

    @Transactional(rollbackFor = Exception.class)
    public String createToken(String robotUuid, String loginUuid, Integer tokenExpireDays) throws Exception {
        RobotEntity robot = getByUuid(robotUuid);
        assertOwner(robot, loginUuid);
        if (!robot.isActive())
            throw new CodeException("机器人未启用");
        assertOwnerActive(robot.getOwner());
        return robotTokenService.createToken(robotUuid, tokenExpireDays);
    }

    /**
     * 轮换令牌：表记录数达上限时删除最旧一条后再签发。
     */
    @Transactional(rollbackFor = Exception.class)
    public String rotateToken(String robotUuid, String loginUuid, Integer tokenExpireDays) throws Exception {
        RobotEntity robot = getByUuid(robotUuid);
        assertOwner(robot, loginUuid);
        if (!robot.isActive())
            throw new CodeException("机器人未启用");
        assertOwnerActive(robot.getOwner());
        return robotTokenService.rotateIssue(robotUuid, tokenExpireDays);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(String robotUuid, String loginUuid, String name, String description, String icon, String access, Boolean black) throws Exception {
        RobotEntity robot = getByUuid(robotUuid);
        assertOwner(robot, loginUuid);
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
        if (robot == null)
            return;
        robot.setLastUsedTime(new Date());
        robot.setUpdateTime(robot.getLastUsedTime());
        updateById(robot);
    }

    private void dispatchHook(RobotEntity robot, String event) {
        if (robot == null || StringUtils.isBlank(event))
            return;
        Map<String, Object> payload = new HashMap<>();
        payload.put("owner", robot.getOwner());
        payload.put("name", robot.getName());
        payload.put("status", robot.getStatus());
        robotHookDispatcher.dispatch(robot.getUuid(), event, payload);
    }
}