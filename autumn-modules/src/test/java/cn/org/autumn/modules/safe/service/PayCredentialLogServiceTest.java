package cn.org.autumn.modules.safe.service;

import cn.org.autumn.model.PayCredentialConfig;
import org.junit.Assert;
import org.junit.Test;

public class PayCredentialLogServiceTest {

    @Test
    public void deleteOlderThanDaysRejectsNonPositive() {
        PayCredentialLogService service = new PayCredentialLogService();
        Assert.assertEquals(0, service.deleteOlderThanDays(0));
        Assert.assertEquals(0, service.deleteOlderThanDays(-1));
    }

    @Test
    public void configDefaults() {
        PayCredentialConfig config = new PayCredentialConfig();
        Assert.assertTrue(config.isAuditLogEnabled());
        Assert.assertEquals(180, config.getLogRetentionDays());
    }
}
