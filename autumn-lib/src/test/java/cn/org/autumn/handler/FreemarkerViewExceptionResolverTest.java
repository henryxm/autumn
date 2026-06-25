package cn.org.autumn.handler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import freemarker.template.TemplateException;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class FreemarkerViewExceptionResolverTest {

    @Test
    public void extractFindsTemplateExceptionInCauseChain() {
        TemplateException te = new TemplateException("x", null);
        Exception wrapper = new Exception("wrap", te);
        FreemarkerViewExceptionResolver r = new FreemarkerViewExceptionResolver();
        Throwable t = (Throwable) ReflectionTestUtils.invokeMethod(r, "extractFreemarkerThrowable", wrapper);
        assertNotNull(t);
        assertSame(te, t);
    }

    @Test
    public void extractFindsMessageHintWhenNoFreemarkerTypeInChain() {
        Exception root = new Exception("nested: freemarker.template.TemplateNotFoundException: missing");
        FreemarkerViewExceptionResolver r = new FreemarkerViewExceptionResolver();
        Throwable t = (Throwable) ReflectionTestUtils.invokeMethod(r, "extractFreemarkerThrowable", root);
        assertNotNull(t);
        assertSame(root, t);
    }

    @Test
    public void extractReturnsNullForUnrelatedException() {
        FreemarkerViewExceptionResolver r = new FreemarkerViewExceptionResolver();
        Throwable t = (Throwable) ReflectionTestUtils.invokeMethod(r, "extractFreemarkerThrowable", new IllegalStateException("no"));
        assertNull(t);
    }

    @Test
    public void loadingPathWithQueryRecognized() {
        FreemarkerViewExceptionResolver r = new FreemarkerViewExceptionResolver();
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(r, "isLoadingPageRequest", "/ctx/loading.html?foo=1"));
    }
}
