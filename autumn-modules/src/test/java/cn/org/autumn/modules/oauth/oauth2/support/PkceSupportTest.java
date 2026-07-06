package cn.org.autumn.modules.oauth.oauth2.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PkceSupportTest {

    @Test
    public void s256ChallengeDeterministic() {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String challenge = PkceSupport.s256Challenge(verifier);
        assertEquals(challenge, PkceSupport.s256Challenge(verifier));
        PkceSupport.validateVerifier(challenge, PkceSupport.METHOD_S256, verifier);
    }

    @Test
    public void rejectInvalidVerifier() {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String challenge = PkceSupport.s256Challenge(verifier);
        try {
            PkceSupport.validateVerifier(challenge, PkceSupport.METHOD_S256, verifier + "x");
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertEquals("code_verifier无效", e.getMessage());
        }
    }

    @Test
    public void skipWhenNoChallenge() {
        PkceSupport.validateVerifier(null, null, null);
    }
}
