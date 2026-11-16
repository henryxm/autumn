package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.sys.dao.PasswordChangeLogDao;
import cn.org.autumn.modules.sys.dto.PasswordChangeLogView;
import cn.org.autumn.modules.sys.entity.PasswordChangeLogEntity;
import cn.org.autumn.modules.sys.shiro.PasswordHashSupport;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.sys.support.PasswordChangeAuditContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordChangeLogServiceTest {

    @Mock
    private PasswordChangeLogDao baseMapper;

    private PasswordChangeLogService service;

    @BeforeEach
    void setUp() {
        service = spy(new PasswordChangeLogService());
        ReflectionTestUtils.setField(service, "baseMapper", baseMapper);
    }

    @Test
    void recordPersistsPreviousHashAndSalt() {
        when(baseMapper.insert(any(PasswordChangeLogEntity.class))).thenReturn(1);

        service.record("target-1", "op-1",
                PasswordChangeLogService.ACTION_CHANGE,
                PasswordChangeLogService.CHANNEL_SELF,
                "old-hash-value", "old-salt-value", true, null);

        ArgumentCaptor<PasswordChangeLogEntity> captor = ArgumentCaptor.forClass(PasswordChangeLogEntity.class);
        verify(baseMapper).insert(captor.capture());
        PasswordChangeLogEntity row = captor.getValue();
        assertEquals("target-1", row.getTargetUserUuid());
        assertEquals("op-1", row.getOperatorUserUuid());
        assertEquals(PasswordChangeLogService.ACTION_CHANGE, row.getAction());
        assertEquals(PasswordChangeLogService.CHANNEL_SELF, row.getChannel());
        assertEquals("old-hash-value", row.getPreviousPasswordHash());
        assertEquals("old-salt-value", row.getPreviousSalt());
        assertTrue(row.isSuccess());
        assertNotNull(row.getUuid());
        assertNotNull(row.getCreateTime());
    }

    @Test
    void matchesHistoryLegacySha256() {
        String salt = "abcdefghijklmnopqrst";
        String legacy = ShiroUtils.sha256("OnceUsed#1", salt);
        PasswordChangeLogEntity row = new PasswordChangeLogEntity();
        row.setPreviousPasswordHash(legacy);
        row.setPreviousSalt(salt);
        doReturn(List.of(row)).when(service).listRecent("u-sha", 5);

        assertTrue(service.matchesHistory("u-sha", "OnceUsed#1", 5));
        assertFalse(service.matchesHistory("u-sha", "other", 5));
    }

    @Test
    void matchesHistoryPbkdf2() {
        String stored = PasswordHashSupport.hashNew("OnceUsed#2");
        PasswordChangeLogEntity row = new PasswordChangeLogEntity();
        row.setPreviousPasswordHash(stored);
        row.setPreviousSalt(null);
        doReturn(List.of(row)).when(service).listRecent("u-pbk", 5);

        assertTrue(service.matchesHistory("u-pbk", "OnceUsed#2", 5));
        assertFalse(service.matchesHistory("u-pbk", "other", 5));
    }

    @Test
    void assertNotRecentPasswordRejectsReuse() {
        String stored = PasswordHashSupport.hashNew("ReuseMe");
        PasswordChangeLogEntity row = new PasswordChangeLogEntity();
        row.setPreviousPasswordHash(stored);
        doReturn(List.of(row)).when(service).listRecent("u-reuse", PasswordChangeLogService.DEFAULT_HISTORY_LIMIT);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.assertNotRecentPassword("u-reuse", "ReuseMe", null, null));
        assertTrue(ex.getMessage().contains("最近"));
    }

    @Test
    void assertNotRecentPasswordRejectsCurrent() {
        String current = PasswordHashSupport.hashNew("SameAsNow");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.assertNotRecentPassword("u-cur", "SameAsNow", current, null));
        assertTrue(ex.getMessage().contains("当前"));
        verify(service, never()).listRecent(anyString(), anyInt());
    }

    @Test
    void passwordChangeAuditContextRestoresNested() {
        PasswordChangeAuditContext.run("OUTER", "op-outer", () -> {
            assertEquals("OUTER", PasswordChangeAuditContext.channelOrDefault(null));
            PasswordChangeAuditContext.run("INNER", "op-inner", () -> {
                assertEquals("INNER", PasswordChangeAuditContext.channelOrDefault(null));
                assertEquals("op-inner", PasswordChangeAuditContext.operatorOrNull());
            });
            assertEquals("OUTER", PasswordChangeAuditContext.channelOrDefault(null));
            assertEquals("op-outer", PasswordChangeAuditContext.operatorOrNull());
        });
        assertNull(PasswordChangeAuditContext.channelOrDefault(null));
        assertNull(PasswordChangeAuditContext.operatorOrNull());
    }

    @Test
    void toViewStripsHashFields() {
        PasswordChangeLogEntity e = new PasswordChangeLogEntity();
        e.setUuid("log-1");
        e.setTargetUserUuid("t1");
        e.setOperatorUserUuid("o1");
        e.setAction(PasswordChangeLogService.ACTION_CHANGE);
        e.setChannel(PasswordChangeLogService.CHANNEL_SELF);
        e.setPreviousPasswordHash("secret-hash");
        e.setPreviousSalt("secret-salt");
        e.setSuccess(true);
        e.setIp("1.2.3.4");

        PasswordChangeLogView v = service.toView(e);
        assertEquals("log-1", v.getUuid());
        assertEquals("t1", v.getTargetUserUuid());
        assertEquals("1.2.3.4", v.getIp());
        assertNull(getFieldIfPresent(v, "previousPasswordHash"));
        assertNull(getFieldIfPresent(v, "previousSalt"));
    }

    /** View 类本身无哈希字段；用反射确认类型上也不暴露同名属性。 */
    private static Object getFieldIfPresent(Object target, String name) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
            return null;
        }
    }
}
