package cn.org.autumn.modules.sys.service;

import cn.org.autumn.model.UserAccountProbe;
import cn.org.autumn.model.UserAccountState;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.service.RobotService;
import cn.org.autumn.modules.sys.dao.SysUserDao;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 按业务 uuid 判定账号状态，区分禁用/注销/不存在，供 video/im 等模块显式调用。
 */
@Service
public class UserAccountProbeService implements UserAccountProbe {

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    @Lazy
    private SysUserDao sysUserDao;

    @Autowired
    @Lazy
    private RobotService robotService;

    @Override
    public UserAccountState resolve(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return UserAccountState.NOT_FOUND;
        }
        String id = uuid.trim();
        SysUserEntity user = sysUserService.getCache(id);
        if (user != null) {
            if (user.getStatus() >= 1) {
                return UserAccountState.ACTIVE;
            }
            if (user.getStatus() == 0) {
                return UserAccountState.DISABLED;
            }
        }
        SysUserEntity archived = sysUserDao.getForDelete(id);
        if (archived != null && archived.getStatus() == -1) {
            return UserAccountState.CANCELLED;
        }
        RobotEntity robot = robotService.getCache(id);
        if (robot != null && robot.isActive()) {
            return UserAccountState.ROBOT_ACTIVE;
        }
        return UserAccountState.NOT_FOUND;
    }
}
