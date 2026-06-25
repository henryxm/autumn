package cn.org.autumn.modules.safe.service;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.safe.site.SafeConfig;
import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PayCredentialTokenStoreTest {

    private PayCredentialTokenStore store;

    @Before
    public void setUp() throws Exception {
        store = new PayCredentialTokenStore();
        SafeConfig configService = Mockito.mock(SafeConfig.class);
        PayCredentialConfig config = new PayCredentialConfig();
        config.setVerifyTokenBindAmount(true);
        Mockito.when(configService.get()).thenReturn(config);
        Field f = PayCredentialTokenStore.class.getDeclaredField("safeConfig");
        f.setAccessible(true);
        f.set(store, configService);
        Field redisOpen = PayCredentialTokenStore.class.getDeclaredField("redisOpen");
        redisOpen.setAccessible(true);
        redisOpen.setBoolean(store, false);
    }

    @Test
    public void verifyTokenBindsAmount() throws Exception {
        String token = store.issueVerifyToken("u1", "PIN", 1000L, "ord-1");
        try {
            store.consumeVerifyToken("u1", token, 2000L, "ord-1");
            Assert.fail("expected mismatch");
        } catch (CodeException ignored) {
        }
        String valid = store.issueVerifyToken("u1", "PIN", 1000L, "ord-1");
        store.consumeVerifyToken("u1", valid, 1000L, "ord-1");
    }

    @Test
    public void verifyTokenSingleUse() throws Exception {
        String token = store.issueVerifyToken("u1", "BIO", 500L, "");
        store.consumeVerifyToken("u1", token, 500L, null);
        try {
            store.consumeVerifyToken("u1", token, 500L, null);
            Assert.fail("expected invalid");
        } catch (CodeException ignored) {
        }
    }
}
