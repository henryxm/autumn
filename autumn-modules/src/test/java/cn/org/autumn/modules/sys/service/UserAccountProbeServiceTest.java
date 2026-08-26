package cn.org.autumn.modules.sys.service;

import cn.org.autumn.model.UserAccountState;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.service.RobotService;
import cn.org.autumn.modules.sys.dao.SysUserDao;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class UserAccountProbeServiceTest {

    @InjectMocks
    private UserAccountProbeService probeService;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysUserDao sysUserDao;

    @Mock
    private RobotService robotService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void activeUser() {
        SysUserEntity user = new SysUserEntity();
        user.setStatus(1);
        when(sysUserService.getCache("u1")).thenReturn(user);
        assertEquals(UserAccountState.ACTIVE, probeService.resolve("u1"));
    }

    @Test
    public void disabledUser() {
        SysUserEntity user = new SysUserEntity();
        user.setStatus(0);
        when(sysUserService.getCache("u1")).thenReturn(user);
        assertEquals(UserAccountState.DISABLED, probeService.resolve("u1"));
    }

    @Test
    public void cancelledUser() {
        when(sysUserService.getCache("u1")).thenReturn(null);
        SysUserEntity archived = new SysUserEntity();
        archived.setStatus(-1);
        when(sysUserDao.getForDelete("u1")).thenReturn(archived);
        assertEquals(UserAccountState.CANCELLED, probeService.resolve("u1"));
    }

    @Test
    public void activeRobot() {
        when(sysUserService.getCache("r1")).thenReturn(null);
        when(sysUserDao.getForDelete("r1")).thenReturn(null);
        RobotEntity robot = new RobotEntity();
        robot.setStatus(RobotEntity.STATUS_ACTIVE);
        when(robotService.getCache("r1")).thenReturn(robot);
        assertEquals(UserAccountState.ROBOT_ACTIVE, probeService.resolve("r1"));
    }

    @Test
    public void notFound() {
        when(sysUserService.getCache("x")).thenReturn(null);
        when(sysUserDao.getForDelete("x")).thenReturn(null);
        when(robotService.getCache("x")).thenReturn(null);
        assertEquals(UserAccountState.NOT_FOUND, probeService.resolve("x"));
    }
}
