package cn.org.autumn.modules.safe.service;

import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.safe.dto.PayGateAssessRequest;
import cn.org.autumn.modules.safe.dto.PayGateAssessResult;
import cn.org.autumn.modules.safe.entity.PayGateAttemptEntity;
import cn.org.autumn.modules.safe.site.SafeConfig;
import cn.org.autumn.modules.safe.support.PayCredentialVerifyMethods;
import cn.org.autumn.service.DistributedLockService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

public class PayGateServiceTest {

    private PayGateService payGateService;

    @Before
    public void setUp() throws Exception {
        payGateService = new PayGateService();
        SafeConfig safeConfig = Mockito.mock(SafeConfig.class);
        PayUserSecuritySettingService payUserSecuritySettingService = Mockito.mock(PayUserSecuritySettingService.class);
        PayUserPinService payUserPinService = Mockito.mock(PayUserPinService.class);
        PayUserTrustedDeviceService payUserTrustedDeviceService = Mockito.mock(PayUserTrustedDeviceService.class);
        PayUserTrustedIpService payUserTrustedIpService = Mockito.mock(PayUserTrustedIpService.class);
        PayGateAttemptService payGateAttemptService = Mockito.mock(PayGateAttemptService.class);
        PayGateTokenStore payGateTokenStore = Mockito.mock(PayGateTokenStore.class);
        PayCredentialLogService payCredentialLogService = Mockito.mock(PayCredentialLogService.class);
        inject("safeConfig", safeConfig);
        inject("payUserSecuritySettingService", payUserSecuritySettingService);
        inject("payUserPinService", payUserPinService);
        inject("payUserTrustedDeviceService", payUserTrustedDeviceService);
        inject("payUserTrustedIpService", payUserTrustedIpService);
        inject("payGateAttemptService", payGateAttemptService);
        inject("payGateTokenStore", payGateTokenStore);
        inject("payCredentialLogService", payCredentialLogService);
        DistributedLockService distributedLockService = Mockito.mock(DistributedLockService.class);
        Mockito.when(distributedLockService.withLock(Mockito.anyString(), Mockito.any(Callable.class))).thenAnswer(inv -> ((Callable<?>) inv.getArgument(1)).call());
        inject("distributedLockService", distributedLockService);
        PayCredentialConfig config = new PayCredentialConfig();
        Mockito.when(safeConfig.get()).thenReturn(config);
        PayUserSecuritySettingService.UserSecurityEffective effective = new PayUserSecuritySettingService.UserSecurityEffective();
        Mockito.when(payUserSecuritySettingService.resolveEffective(Mockito.anyString())).thenReturn(effective);
        Mockito.when(payUserTrustedDeviceService.isTrustedDevice(Mockito.anyString(), Mockito.any())).thenReturn(true);
        Mockito.when(payUserTrustedIpService.isTrustedIp(Mockito.anyString(), Mockito.any())).thenReturn(true);
        Mockito.when(payGateAttemptService.countAuthorizedSameAmountSince(Mockito.anyString(), Mockito.anyLong(), Mockito.any())).thenReturn(0);
        Mockito.when(payGateTokenStore.issueGateToken(Mockito.anyString(), Mockito.anyLong(), Mockito.any(), Mockito.any())).thenReturn("gate-token");
    }

    private void inject(String name, Object value) throws Exception {
        Field field = PayGateService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(payGateService, value);
    }

    @Test
    public void assessHighAmountRequiresPassword() throws Exception {
        PayUserSecuritySettingService payUserSecuritySettingService = (PayUserSecuritySettingService) getField("payUserSecuritySettingService");
        PayUserSecuritySettingService.UserSecurityEffective effective = new PayUserSecuritySettingService.UserSecurityEffective();
        effective.setHighAmountThresholdCent(10000L);
        Mockito.when(payUserSecuritySettingService.resolveEffective("u1")).thenReturn(effective);
        PayGateAssessRequest req = new PayGateAssessRequest();
        req.setAmountCent(20000L);
        PayGateAssessResult result = payGateService.assess("u1", req, null);
        Assert.assertTrue(result.isAuthorized());
        Assert.assertEquals(PayGateAttemptEntity.AUTH_PASSWORD_REQUIRED, result.getAuthMode());
        Assert.assertTrue(result.isNeedPassword());
    }

    @Test
    public void assessIncludesGestureWhenEnabled() throws Exception {
        PayUserSecuritySettingService payUserSecuritySettingService = (PayUserSecuritySettingService) getField("payUserSecuritySettingService");
        PayUserSecuritySettingService.UserSecurityEffective effective = new PayUserSecuritySettingService.UserSecurityEffective();
        effective.setGesturePaymentEnabled(true);
        Mockito.when(payUserSecuritySettingService.resolveEffective("u1")).thenReturn(effective);
        PayGateAssessRequest req = new PayGateAssessRequest();
        req.setAmountCent(500L);
        PayGateAssessResult result = payGateService.assess("u1", req, null);
        Assert.assertTrue(result.getAllowedVerifyMethods().contains(PayCredentialVerifyMethods.GESTURE));
        Assert.assertTrue(result.getAllowedVerifyMethods().contains(PayCredentialVerifyMethods.PIN));
    }

    private Object getField(String name) throws Exception {
        Field field = PayGateService.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(payGateService);
    }
}
