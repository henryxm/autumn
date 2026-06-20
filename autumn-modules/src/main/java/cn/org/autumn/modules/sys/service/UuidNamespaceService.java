package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.bot.dao.RobotDao;
import cn.org.autumn.modules.sys.dao.SysUserDao;
import cn.org.autumn.utils.Uuid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 在 {@code sys_user} 与 {@code bot_robot} 间分配全局唯一 {@code uuid}，保证两类调用主体 uuid <strong>互不相同</strong>。
 * <p>
 * 真人注册、机器人创建等写入前须经本服务分配或校验；业务表 {@code user} 列可引用任一侧 uuid，见 {@code docs/AI_DUAL_KEY.md} §1.1。
 */
@Service
public class UuidNamespaceService {

    private static final int ALLOCATE_MAX_RETRY = 32;

    @Autowired
    @Lazy
    private SysUserDao sysUserDao;

    @Autowired
    @Lazy
    private RobotDao robotDao;

    public boolean existsInUser(String uuid) {
        if (StringUtils.isBlank(uuid))
            return false;
        return sysUserDao.getByUuid(uuid) != null;
    }

    public boolean existsInRobot(String uuid) {
        if (StringUtils.isBlank(uuid))
            return false;
        return robotDao.countByUuid(uuid) > 0;
    }

    public boolean existsAny(String uuid) {
        return existsInUser(uuid) || existsInRobot(uuid);
    }

    public String allocate() {
        for (int i = 0; i < ALLOCATE_MAX_RETRY; i++) {
            String candidate = Uuid.uuid();
            if (!existsAny(candidate))
                return candidate;
        }
        throw new IllegalStateException("无法分配全局唯一uuid");
    }

    public String allocateIfBlank(String uuid) {
        if (StringUtils.isBlank(uuid))
            return allocate();
        if (existsAny(uuid))
            throw new IllegalArgumentException("uuid已被用户或机器人占用");
        return uuid;
    }
}
