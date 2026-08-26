package cn.org.autumn.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientSourceContextTest {

    @Test
    void emptySourceForLogIsUnknown() {
        assertEquals("unknown", ClientSourceContext.empty().sourceForLog());
    }

    @Test
    void parsesPlatformFeatureSubfeature() {
        ClientSourceContext ctx = ClientSourceContext.of("flutter.chat_list.prefetch", true);
        assertEquals("flutter.chat_list.prefetch", ctx.getRaw());
        assertEquals("flutter", ctx.getPlatform());
        assertEquals("chat_list", ctx.getFeature());
        assertEquals("prefetch", ctx.getSubfeature());
        assertTrue(ctx.isBatch());
        assertEquals("flutter.chat_list.prefetch", ctx.sourceForLog());
    }

    @Test
    void parsesTwoSegments() {
        ClientSourceContext ctx = ClientSourceContext.of("pc.conversation_open", false);
        assertEquals("pc", ctx.getPlatform());
        assertEquals("conversation_open", ctx.getFeature());
        assertEquals("", ctx.getSubfeature());
        assertFalse(ctx.isBatch());
    }

    @Test
    void blankRawIsEmpty() {
        ClientSourceContext ctx = ClientSourceContext.of("  ", false);
        assertEquals("", ctx.getRaw());
        assertEquals("unknown", ctx.sourceForLog());
    }

    @Test
    void noArgConstructorForDubboDeserialization() {
        ClientSourceContext ctx = new ClientSourceContext();
        assertEquals("", ctx.getRaw());
        assertFalse(ctx.isBatch());
        ctx.setRaw("im.conversation_open");
        ctx.setBatch(true);
        assertEquals("im.conversation_open", ctx.getRaw());
        assertTrue(ctx.isBatch());
    }
}
