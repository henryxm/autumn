package cn.org.autumn.modules.opc.support;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class OpenOAuthStateSupportTest {

    @Test
    public void bindAndConsume() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        OpenOAuthStateSupport.bindState(request, "app1", "state-abc");
        assertTrue(OpenOAuthStateSupport.consumeState(request, "app1", "state-abc"));
        assertFalse(OpenOAuthStateSupport.consumeState(request, "app1", "state-abc"));
    }

    @Test
    public void rejectMismatchedState() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        OpenOAuthStateSupport.bindState(request, "app1", "expected");
        assertFalse(OpenOAuthStateSupport.consumeState(request, "app1", "wrong"));
    }
}
