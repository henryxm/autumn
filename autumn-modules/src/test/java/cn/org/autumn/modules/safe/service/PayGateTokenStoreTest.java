package cn.org.autumn.modules.safe.service;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.safe.entity.PayGateAttemptEntity;
import cn.org.autumn.modules.safe.site.SafeConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

public class PayGateTokenStoreTest {

    private PayGateTokenStore store;

    @Before
    public void setUp() throws Exception {
        store = new PayGateTokenStore();
        SafeConfig configService = Mockito.mock(SafeConfig.class);
        PayCredentialConfig config = new PayCredentialConfig();
        Mockito.when(configService.get()).thenReturn(config);
        Field f = PayGateTokenStore.class.getDeclaredField("safeConfig");
        f.setAccessible(true);
        f.set(store, configService);
        Field redisOpen = PayGateTokenStore.class.getDeclaredField("redisOpen");
        redisOpen.setAccessible(true);
        redisOpen.setBoolean(store, false);
    }

    @Test
    public void gateTokenConsumesOnceAndMatchesAmount() throws Exception {
        String token = store.issueGateToken("u1", 1000L, "o1", PayGateAttemptEntity.AUTH_PASSWORD_REQUIRED);
        PayGateTokenStore.GateEntry entry = store.consumeGateToken("u1", token, 1000L);
        Assert.assertEquals("o1", entry.orderId);
        try {
            store.consumeGateToken("u1", token, 1000L);
            Assert.fail("expected invalid");
        } catch (CodeException ignored) {
        }
    }

    @Test(expected = CodeException.class)
    public void gateTokenAmountMismatch() throws Exception {
        String token = store.issueGateToken("u1", 1000L, "", PayGateAttemptEntity.AUTH_PASSWORD_REQUIRED);
        store.consumeGateToken("u1", token, 2000L);
    }
}
