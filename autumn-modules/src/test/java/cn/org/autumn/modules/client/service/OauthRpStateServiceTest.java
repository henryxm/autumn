package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.dto.OauthRpStatePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OauthRpStateServiceTest {

    private OauthRpStateService oauthRpStateService;

    @BeforeEach
    void setUp() {
        oauthRpStateService = new OauthRpStateService();
    }

    @Test
    void issueAndConsumeState_withClientId() {
        String state = oauthRpStateService.issueState("https://app.example.com/after", "client-a");

        OauthRpStatePayload peek = oauthRpStateService.peekStatePayload(state);
        assertEquals("https://app.example.com/after", peek.getCallback());
        assertEquals("client-a", peek.getClientId());

        OauthRpStatePayload consumed = oauthRpStateService.consumeStatePayload(state);
        assertEquals("client-a", consumed.getClientId());
        assertNull(oauthRpStateService.peekStatePayload(state));
    }

    @Test
    void legacyCallbackOnlyState() {
        String state = oauthRpStateService.issueState("https://legacy.example.com/home");

        OauthRpStatePayload payload = oauthRpStateService.consumeStatePayload(state);
        assertEquals("https://legacy.example.com/home", payload.getCallback());
        assertNull(payload.getClientId());
    }
}
