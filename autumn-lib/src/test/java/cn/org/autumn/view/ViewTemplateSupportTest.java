package cn.org.autumn.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import cn.org.autumn.view.ViewTemplateSupport.ResolvedView;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class ViewTemplateSupportTest {

    @Test
    public void normalizeViewNameStripsLeadingSlash() {
        ViewTemplateSupport support = new ViewTemplateSupport();
        assertEquals("modules/test/pages/index", support.normalizeViewName("/modules/test/pages/index"));
    }

    @Test
    public void toTemplatePathAppendsHtmlSuffix() {
        ViewTemplateSupport support = new ViewTemplateSupport();
        ReflectionTestUtils.setField(support, "viewSuffix", ".html");
        assertEquals("modules/test/pages/index.html", support.toTemplatePath("/modules/test/pages/index"));
    }

    @Test
    public void specialViewNamesSkipExistenceCheck() {
        ViewTemplateSupport support = new ViewTemplateSupport();
        assertTrue(support.isSpecialViewName("redirect:/login"));
        assertTrue(support.isSpecialViewName("forward:/main"));
        assertTrue(support.isSpecialViewName("404"));
        assertFalse(support.isSpecialViewName("modules/sys/user"));
    }

    @Test
    public void resolveMissingTemplateReturns404View() {
        ViewTemplateSupport support = new ViewTemplateSupport() {
            @Override
            public boolean exists(String viewName) {
                return false;
            }
        };
        ResolvedView resolved = support.resolve("/modules/test/pages/index");
        assertTrue(resolved.isNotFound());
        assertEquals(ViewTemplateSupport.FALLBACK_404_VIEW, resolved.getViewName());
    }

    @Test
    public void resolveNormalizesExistingViewName() {
        ViewTemplateSupport support = new ViewTemplateSupport() {
            @Override
            public boolean exists(String viewName) {
                return true;
            }
        };
        ResolvedView resolved = support.resolve("/modules/sys/user");
        assertFalse(resolved.isNotFound());
        assertEquals("modules/sys/user", resolved.getViewName());
    }
}
