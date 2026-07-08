package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.dao.WebAuthenticationDao;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebAuthenticationServiceIconTest {

    @Mock
    private WebAuthenticationDao webAuthenticationDao;

    @InjectMocks
    private WebAuthenticationService webAuthenticationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webAuthenticationService, "baseMapper", webAuthenticationDao);
    }

    @Test
    void using_returnsTrueWhenHashReferenced() {
        when(webAuthenticationDao.countByHashInUse("abc123")).thenReturn(1);
        assertTrue(webAuthenticationService.using("abc123"));
        assertTrue(webAuthenticationService.isIconHashInUse("abc123"));
    }

    @Test
    void using_returnsFalseWhenBlank() {
        assertFalse(webAuthenticationService.using(""));
        assertFalse(webAuthenticationService.using(null));
    }
}
