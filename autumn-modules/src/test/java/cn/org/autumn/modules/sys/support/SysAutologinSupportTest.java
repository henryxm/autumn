package cn.org.autumn.modules.sys.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.org.autumn.utils.R;
import org.junit.jupiter.api.Test;

class SysAutologinSupportTest {

    @Test
    void skippedWhenLogoutCookieMarked() {
        R r = SysAutologinSupport.buildAutologinResponse(true, false, false, true, true, "/");
        assertFalse(Integer.valueOf(0).equals(r.get("code")));
        assertEquals(SysAutologinSupport.REASON_SKIPPED, r.get("reason"));
    }

    @Test
    void autologinDisabledNeverRedirectsEvenWhenAuthenticated() {
        R r = SysAutologinSupport.buildAutologinResponse(false, true, true, false, false, "/index.html");
        assertFalse(Integer.valueOf(0).equals(r.get("code")));
        assertEquals(SysAutologinSupport.REASON_SESSION_ACTIVE, r.get("reason"));
        assertTrue(Boolean.TRUE.equals(r.get("sessionActiveHint")));
        assertNull(r.get("data"));
    }

    @Test
    void autologinEnabledStillBlocksAuthenticatedRedirect() {
        R r = SysAutologinSupport.buildAutologinResponse(false, true, true, true, true, "/index.html");
        assertFalse(Integer.valueOf(0).equals(r.get("code")));
        assertEquals(SysAutologinSupport.REASON_SESSION_ACTIVE, r.get("reason"));
        assertNull(r.get("data"));
    }

    @Test
    void rememberMePrincipalDoesNotAutoRedirect() {
        R r = SysAutologinSupport.buildAutologinResponse(false, false, true, true, false, "/");
        assertFalse(Integer.valueOf(0).equals(r.get("code")));
        assertEquals(SysAutologinSupport.REASON_REMEMBER_ME_BLOCKED, r.get("reason"));
        assertTrue(Boolean.TRUE.equals(r.get("rememberMeHint")));
    }

    @Test
    void devProbeOnlyWhenAutologinEnabled() {
        R enabled = SysAutologinSupport.buildAutologinResponse(false, false, false, true, true, "/");
        assertEquals(0, enabled.get("code"));
        assertTrue(Boolean.TRUE.equals(enabled.get("devProbe")));
        assertEquals(SysAutologinSupport.REASON_DEV_PROBE, enabled.get("reason"));

        R disabled = SysAutologinSupport.buildAutologinResponse(false, false, false, true, false, "/");
        assertFalse(Integer.valueOf(0).equals(disabled.get("code")));
        assertEquals(SysAutologinSupport.REASON_AUTOLOGIN_DISABLED, disabled.get("reason"));
    }
}
