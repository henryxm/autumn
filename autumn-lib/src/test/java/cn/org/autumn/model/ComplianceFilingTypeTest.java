package cn.org.autumn.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ComplianceFilingTypeTest {

    @Test
    public void resolveUrlPrefersConfiguredUrl() {
        assertEquals("https://example.com/icp", ComplianceFilingType.icp.resolveUrl("", "https://example.com/icp"));
    }

    @Test
    public void resolveUrlUsesDefaultForIcp() {
        assertEquals(ComplianceFilingType.DEFAULT_ICP_URL, ComplianceFilingType.icp.resolveUrl("蜀ICP备1号", ""));
    }

    @Test
    public void resolveUrlBuildsPsbRecordCodeLink() {
        String url = ComplianceFilingType.psb.resolveUrl("川公网安备51010802000202号", "");
        assertTrue(url.contains("recordcode=51010802000202"));
    }

    @Test
    public void resolveUrlUsesDefaultForFoodLicense() {
        assertEquals(ComplianceFilingType.DEFAULT_FOOD_LICENSE_URL, ComplianceFilingType.food_license.resolveUrl("", ""));
    }

    @Test
    public void defaultUrlTemplateCoversNonPsbTypes() {
        for (ComplianceFilingType type : ComplianceFilingType.values()) {
            if (type == ComplianceFilingType.psb) {
                continue;
            }
            assertEquals(type.defaultUrlTemplate(), type.resolveUrl("", ""));
        }
    }
}
