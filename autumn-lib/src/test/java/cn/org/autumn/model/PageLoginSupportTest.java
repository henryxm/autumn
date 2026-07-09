package cn.org.autumn.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageLoginSupportTest {

    @Test
    void flags() {
        assertFalse(PageLoginSupport.showTab(PageLoginSupport.NONE));
        assertFalse(PageLoginSupport.showQr(PageLoginSupport.NONE));
        assertTrue(PageLoginSupport.showTab(PageLoginSupport.TAB));
        assertFalse(PageLoginSupport.showQr(PageLoginSupport.TAB));
        assertFalse(PageLoginSupport.showTab(PageLoginSupport.QR));
        assertTrue(PageLoginSupport.showQr(PageLoginSupport.QR));
        assertTrue(PageLoginSupport.showTab(PageLoginSupport.TAB_AND_QR));
        assertTrue(PageLoginSupport.showQr(PageLoginSupport.TAB_AND_QR));
        assertEquals(PageLoginSupport.NONE, PageLoginSupport.parse(99));
    }
}
