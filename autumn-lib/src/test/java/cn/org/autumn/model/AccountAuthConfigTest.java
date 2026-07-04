package cn.org.autumn.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountAuthConfigTest {

    @Test
    void validateAndFix_clearsInvalidPostLoginRedirect() {
        AccountAuthConfig config = new AccountAuthConfig();
        config.setPostLoginRedirect("/module/account/me");

        assertEquals(1, config.validateAndFix().size());
        assertNull(config.getPostLoginRedirect());
    }

    @Test
    void validateAndFix_keepsValidPostLoginRedirect() {
        AccountAuthConfig config = new AccountAuthConfig();
        config.setPostLoginRedirect("/main.html");

        assertTrue(config.validateAndFix().isEmpty());
        assertEquals("/main.html", config.getPostLoginRedirect());
    }

    @Test
    void validateAndFix_trimsValidPostLoginRedirect() {
        AccountAuthConfig config = new AccountAuthConfig();
        config.setPostLoginRedirect("  /index.html  ");

        assertTrue(config.validateAndFix().isEmpty());
        assertEquals("/index.html", config.getPostLoginRedirect());
    }
}
