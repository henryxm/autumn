package cn.org.autumn.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class AuthContextsTest {

    @Test
    public void bindAndGet() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AuthContext ctx = AuthContext.of(AuthSource.OAUTH_TOKEN).accessKey("AkDemo").userUuid("u1");
        AuthContexts.bind(request, ctx);
        AuthContext got = AuthContexts.get(request);
        assertNotNull(got);
        assertEquals(AuthSource.OAUTH_TOKEN, got.getSource());
        assertEquals("AkDemo", got.getAccessKey());
        assertEquals("u1", got.getUserUuid());
    }

    @Test
    public void tokenPrefixTruncatesLongOnly() {
        assertEquals("12345678", AuthContexts.tokenPrefix("1234567890abcdef"));
        assertEquals("", AuthContexts.tokenPrefix("short"));
        assertEquals("", AuthContexts.tokenPrefix("123456789012345"));
        assertTrue(AuthContexts.tokenPrefix("1234567890123456").length() == 8);
        assertEquals("", AuthContexts.tokenPrefix(null));
    }

    @Test
    public void getNullRequest() {
        assertNull(AuthContexts.get(null));
    }

    @Test
    public void bindHelper() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AuthContexts.bind(request, AuthSource.OAUTH_TOKEN, "u1", null, "Ak1", "Authorization", "1234567890abcdefzz", null);
        AuthContext got = AuthContexts.get(request);
        assertNotNull(got);
        assertEquals("Ak1", got.getAccessKey());
        assertEquals("Authorization", got.getCredentialHeader());
        assertEquals("12345678", got.getTokenPrefix());
    }
}
