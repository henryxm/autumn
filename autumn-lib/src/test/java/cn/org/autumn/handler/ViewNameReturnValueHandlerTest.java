package cn.org.autumn.handler;

import static org.junit.Assert.assertEquals;

import cn.org.autumn.view.ViewTemplateSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ViewNameMethodReturnValueHandler;

public class ViewNameReturnValueHandlerTest {

    @Test
    public void missingTemplateKeepsOriginalViewName() throws Exception {
        ViewTemplateSupport support = new MissingTemplateSupport();
        ViewNameMethodReturnValueHandler delegate = new ViewNameMethodReturnValueHandler();
        ViewNameReturnValueHandler handler = new ViewNameReturnValueHandler(delegate, support);
        ModelAndViewContainer mavContainer = new ModelAndViewContainer();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletWebRequest webRequest = new ServletWebRequest(new org.springframework.mock.web.MockHttpServletRequest(), response);
        handler.handleReturnValue("/modules/test/pages/index", null, mavContainer, webRequest);
        assertEquals("modules/test/pages/index", mavContainer.getViewName());
        assertEquals(200, response.getStatus());
    }

    private static class MissingTemplateSupport extends ViewTemplateSupport {
        @Override
        public boolean exists(String viewName) {
            return false;
        }
    }
}
