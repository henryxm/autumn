package cn.org.autumn.modules.safe.spi;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import java.lang.reflect.Field;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PayCredentialResetOrchestratorTest {

    private PayCredentialResetOrchestrator orchestrator;
    private LoginPasswordPayResetVerifier loginVerifier;

    @Before
    public void setUp() throws Exception {
        orchestrator = new PayCredentialResetOrchestrator();
        loginVerifier = Mockito.mock(LoginPasswordPayResetVerifier.class);
        setField("loginPasswordPayResetVerifier", loginVerifier);
        setField("verifiers", Collections.emptyList());
    }

    private void setField(String name, Object value) throws Exception {
        Field field = PayCredentialResetOrchestrator.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(orchestrator, value);
    }

    @Test
    public void verify_delegatesLoginPassword() throws Exception {
        PayResetContext ctx = new PayResetContext();
        ctx.setUserUuid("u1");
        ctx.setLoginPassword("secret");
        Mockito.when(loginVerifier.supports(ctx)).thenReturn(true);
        orchestrator.verify(ctx);
        Mockito.verify(loginVerifier).verifyReset(ctx);
    }

    @Test
    public void verify_customSpiFirst() throws Exception {
        PayCredentialResetVerifier custom = Mockito.mock(PayCredentialResetVerifier.class);
        PayResetContext ctx = new PayResetContext();
        ctx.setUserUuid("u1");
        ctx.setSmsCode("123456");
        Mockito.when(custom.supports(ctx)).thenReturn(true);
        setField("verifiers", Collections.singletonList(custom));
        orchestrator.verify(ctx);
        Mockito.verify(custom).verifyReset(ctx);
        Mockito.verify(loginVerifier, Mockito.never()).verifyReset(ctx);
    }

    @Test
    public void verify_noneSupported() {
        PayResetContext ctx = new PayResetContext();
        ctx.setUserUuid("u1");
        Mockito.when(loginVerifier.supports(ctx)).thenReturn(false);
        try {
            orchestrator.verify(ctx);
            Assert.fail("expected CodeException");
        } catch (CodeException e) {
            Assert.assertEquals(Error.OPERATION_NOT_ALLOWED.getCode(), e.getCode());
        }
    }
}
