package cn.org.autumn.site;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import cn.org.autumn.config.SiteLegalLinksHandler;
import cn.org.autumn.model.SitePortalConfig;
import cn.org.autumn.model.SitePortalLegalLinks;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SiteLegalLinksFactoryTest {

    private final List<SiteLegalLinksHandler> handlers = new ArrayList<>();
    private SiteLegalLinksFactory factory;

    @Before
    public void setUp() {
        handlers.clear();
        factory = spy(new SiteLegalLinksFactory());
        doReturn(handlers).when(factory).getOrderList(SiteLegalLinksHandler.class);
    }

    @Test
    public void resolveUsesFirstNonBlankSpiValue() {
        SitePortalConfig config = new SitePortalConfig();
        config.getLegalLinks().setPrivacyUrl("/configured/privacy.html");
        handlers.add(new SiteLegalLinksHandler() {
        });
        handlers.add(new SiteLegalLinksHandler() {
            @Override
            public String privacyUrl(SitePortalConfig cfg) {
                return "https://spi.example/privacy";
            }
        });
        assertEquals("https://spi.example/privacy", factory.resolve(config).getPrivacyUrl());
    }

    @Test
    public void resolveFallsBackToConfigWhenSpiEmpty() {
        SitePortalConfig config = new SitePortalConfig();
        config.getLegalLinks().setTermsUrl("/custom/terms.html");
        handlers.add(new SiteLegalLinksHandler() {
        });
        assertEquals("/custom/terms.html", factory.resolve(config).getTermsUrl());
    }

    @Test
    public void resolveSkipsFactoryBeanInstance() {
        SitePortalConfig config = new SitePortalConfig();
        config.getLegalLinks().setAboutUrl("/config/about.html");
        handlers.add(new FactoryLikeHandler());
        assertEquals("/config/about.html", factory.resolve(config).getAboutUrl());
    }

    private static class FactoryLikeHandler extends SiteLegalLinksFactory implements SiteLegalLinksHandler {
        @Override
        public String aboutUrl(SitePortalConfig cfg) {
            return "https://factory-should-skip.example/about";
        }
    }

    @Test
    public void resolveIgnoresHandlerExceptionAndContinues() {
        SitePortalConfig config = new SitePortalConfig();
        handlers.add(new SiteLegalLinksHandler() {
            @Override
            public String helpUrl(SitePortalConfig cfg) {
                throw new RuntimeException("spi broken");
            }
        });
        handlers.add(new SiteLegalLinksHandler() {
            @Override
            public String helpUrl(SitePortalConfig cfg) {
                return "https://help.example.com";
            }
        });
        assertEquals("https://help.example.com", factory.resolve(config).getHelpUrl());
    }

    @Test
    public void resolveReturnsDefaultPathsWhenConfigNull() {
        SitePortalLegalLinks resolved = factory.resolve(null);
        assertEquals(SitePortalLegalLinks.DEFAULT_PRIVACY_PATH, resolved.getPrivacyUrl());
        assertEquals(SitePortalLegalLinks.DEFAULT_TERMS_PATH, resolved.getTermsUrl());
        assertEquals("", resolved.getAboutUrl());
        assertEquals("", resolved.getHelpUrl());
    }
}
