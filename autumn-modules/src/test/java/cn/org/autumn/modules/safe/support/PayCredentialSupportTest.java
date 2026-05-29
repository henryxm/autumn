package cn.org.autumn.modules.safe.support;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.PayCredentialConfig;
import org.junit.Assert;
import org.junit.Test;

public class PayCredentialSupportTest {

    @Test
    public void validatePinFormat_ok() throws Exception {
        PayCredentialConfig config = new PayCredentialConfig();
        PayCredentialSupport.validatePinFormat("135790", config);
    }

    @Test(expected = CodeException.class)
    public void validatePinFormat_weak() throws Exception {
        PayCredentialSupport.validatePinFormat("123456", new PayCredentialConfig());
    }

    @Test
    public void canonicalizeGesture_ok() throws Exception {
        String canonical = PayCredentialSupport.canonicalizeGesture(new int[]{0, 1, 2, 5}, new PayCredentialConfig());
        Assert.assertEquals("0-1-2-5", canonical);
    }

    @Test(expected = CodeException.class)
    public void canonicalizeGesture_repeatPoint() throws Exception {
        PayCredentialSupport.canonicalizeGesture(new int[]{0, 0, 1, 2}, new PayCredentialConfig());
    }

    @Test
    public void hashPin_deterministic() {
        String salt = "testsalt123456789012";
        String h1 = PayCredentialSupport.hashPin("135790", salt);
        String h2 = PayCredentialSupport.hashPin("135790", salt);
        Assert.assertEquals(h1, h2);
        Assert.assertNotEquals(h1, PayCredentialSupport.hashPin("135791", salt));
    }
}
