package cn.org.autumn.modules.sys.service;

import cn.org.autumn.crypto.FieldEncryptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 单机启动：env=false 不冲掉 sys_config 一键开启的写开关。
 */
class FieldEncryptRuntimeStandaloneReconcileTest {

    private FieldEncryptRuntimeService runtime;
    private FieldEncryptService fieldEncryptService;
    private SysConfigService sysConfigService;
    private AtomicReference<String> stored;

    @BeforeEach
    void setUp() {
        runtime = new FieldEncryptRuntimeService();
        fieldEncryptService = mock(FieldEncryptService.class);
        sysConfigService = mock(SysConfigService.class);
        stored = new AtomicReference<>();
        ReflectionTestUtils.setField(runtime, "fieldEncryptService", fieldEncryptService);
        ReflectionTestUtils.setField(runtime, "sysConfigService", sysConfigService);

        when(sysConfigService.hasKey(anyString())).thenReturn(true);
        org.mockito.Mockito.doAnswer(inv -> {
            stored.set(inv.getArgument(1));
            return null;
        }).when(sysConfigService).updateValueByKey(anyString(), anyString());
    }

    @Test
    void envFalse_keepsSysConfigTrue() {
        when(fieldEncryptService.isConfigWriteEncryptEnabled()).thenReturn(false);
        when(sysConfigService.getValue(FieldEncryptRuntimeService.RUNTIME_WRITE_ENABLED_KEY)).thenReturn("true");

        ReflectionTestUtils.invokeMethod(runtime, "reconcileStandaloneWriteSwitchWithEnvironment");

        verify(sysConfigService, never()).updateValueByKey(eq(FieldEncryptRuntimeService.RUNTIME_WRITE_ENABLED_KEY), eq("false"));
        assertEquals(null, stored.get());
    }

    @Test
    void envTrue_forceOpensSysConfigFalse() {
        when(fieldEncryptService.isConfigWriteEncryptEnabled()).thenReturn(true);
        when(sysConfigService.getValue(FieldEncryptRuntimeService.RUNTIME_WRITE_ENABLED_KEY)).thenReturn("false");

        ReflectionTestUtils.invokeMethod(runtime, "reconcileStandaloneWriteSwitchWithEnvironment");

        verify(sysConfigService).updateValueByKey(FieldEncryptRuntimeService.RUNTIME_WRITE_ENABLED_KEY, "true");
        assertEquals("true", stored.get());
    }
}
