package cn.org.autumn.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SitePortalConfigTest {

    @Test
    public void validateAndFixTrimsFilingsAndRemovesBlankNumber() {
        SitePortalConfig config = new SitePortalConfig();
        ComplianceFilingItem item = new ComplianceFilingItem();
        item.setType("telecom");
        item.setPrefix(" 增值电信业务经营许可证: ");
        item.setNumber(" 川B2-20231966 ");
        item.setSuffix(" 号 ");
        item.setUrl("https://www.miit.gov.cn/");
        config.getFilings().add(item);
        config.getFilings().add(new ComplianceFilingItem());
        config.validateAndFix();
        assertEquals(1, config.getFilings().size());
        assertEquals("增值电信业务经营许可证:", config.getFilings().get(0).getPrefix());
        assertEquals("川B2-20231966", config.getFilings().get(0).getNumber());
        assertEquals("号", config.getFilings().get(0).getSuffix());
    }

    @Test
    public void validateAndFixClearsShowIconForNonPsb() {
        SitePortalConfig config = new SitePortalConfig();
        ComplianceFilingItem item = new ComplianceFilingItem();
        item.setType("icp");
        item.setNumber("蜀ICP备202100200303号-01");
        item.setShowIcon(true);
        config.getFilings().add(item);
        config.validateAndFix();
        assertEquals(false, config.getFilings().get(0).isShowIcon());
    }

    @Test
    public void extractPsbRecordCodeFromNumber() {
        assertEquals("51010802000202", SitePortalConfig.extractPsbRecordCode("川公网安备51010802000202号"));
    }

    @Test
    public void buildCopyrightLineWithRange() {
        SitePortalMeta meta = new SitePortalMeta();
        meta.setCopyrightHolder("示例科技");
        meta.setCopyrightYearStart("2020");
        meta.setCopyrightYearEnd("2026");
        String line = SitePortalConfig.buildCopyrightLine(meta);
        assertTrue(line.contains("©"));
        assertTrue(line.contains("2020-2026"));
        assertTrue(line.contains("示例科技"));
    }

    @Test
    public void buildCopyrightLineEmptyWhenOnlyVersionLabel() {
        SitePortalMeta meta = new SitePortalMeta();
        meta.setVersionLabel("v2.0.0");
        assertEquals("", SitePortalConfig.buildCopyrightLine(meta));
    }
}
