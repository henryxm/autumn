package cn.org.autumn.modules.wall.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CorsOriginSupportTest {

    @Test
    void sameSiteAllowed() {
        assertTrue(CorsOriginSupport.isAllowed(
                "https://example.com", "https://example.com/", List.of()));
    }

    @Test
    void foreignOriginRejected() {
        assertFalse(CorsOriginSupport.isAllowed(
                "https://evil.com", "https://example.com", List.of()));
    }

    @Test
    void allowlistExact() {
        assertTrue(CorsOriginSupport.isAllowed(
                "https://app.example.com", "https://api.example.com",
                List.of("https://app.example.com")));
    }
}
