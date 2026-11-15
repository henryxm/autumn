package cn.org.autumn.modules.bot.service;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.site.BotHandlerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RobotIconUrlHelperTest {

    @Mock
    BotHandlerFactory botHandlerFactory;

    @InjectMocks
    RobotIconUrlHelper helper;

    @Test
    void resolveDelegatesToFactory() {
        when(botHandlerFactory.resolveIconUrl("x")).thenReturn("");
        assertEquals("", helper.resolveAccessUrl("x"));
        when(botHandlerFactory.resolveIconUrl("y")).thenReturn("https://cdn/y");
        assertEquals("https://cdn/y", helper.resolveAccessUrl("y"));
    }

    @Test
    void requireFailsWithoutHandler() {
        when(botHandlerFactory.hasIconUrlHandler()).thenReturn(false);
        assertFalse(helper.hasResolver());
        CodeException ex = assertThrows(CodeException.class, () -> helper.requireAccessUrl("abc"));
        assertEquals(RobotIconUrlHelper.NO_OSS_MSG, ex.getMessage());
    }

    @Test
    void requireUsesHandlerUrl() throws Exception {
        when(botHandlerFactory.hasIconUrlHandler()).thenReturn(true);
        when(botHandlerFactory.resolveIconUrl("oid")).thenReturn("https://cdn.example/a.png");
        assertEquals("https://cdn.example/a.png", helper.requireAccessUrl("oid"));
    }
}
