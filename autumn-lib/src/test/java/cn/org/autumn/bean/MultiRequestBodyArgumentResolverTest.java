package cn.org.autumn.bean;

import cn.org.autumn.annotation.MultiRequestBody;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.Date;

public class MultiRequestBodyArgumentResolverTest {

    private final MultiRequestBodyArgumentResolver resolver = new MultiRequestBodyArgumentResolver();

    @Test
    public void optionalUnparseableBodyReturnsNull() throws Exception {
        Object value = resolve("optionalUnparseable", UnparseableBody.class, "{}");
        Assert.assertNull(value);
    }

    @Test
    public void requiredUnparseableBodyThrowsWhenAbsent() throws Exception {
        try {
            resolve("requiredUnparseable", UnparseableBody.class, "{}");
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("required param requiredBody is not present"));
        }
    }

    @Test
    public void requiredParseAllFieldsJsonErrorThrows() throws Exception {
        try {
            resolve("flatDateHolder", DateHolder.class, "{\"when\":\"not-a-date\"}");
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("failed to parse required param flatDateHolder"));
            Assert.assertNotNull(e.getCause());
        }
    }

    @Test
    public void optionalParseAllFieldsJsonErrorReturnsNull() throws Exception {
        Object value = resolve("flatOptionalDateHolder", DateHolder.class, "{\"when\":\"not-a-date\"}");
        Assert.assertNull(value);
    }

    @Test
    public void explicitKeyMissingThrows() throws Exception {
        try {
            resolve("requiredNamed", NamedBody.class, "{}");
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("required param requiredBody is not present"));
        }
    }

    @Test
    public void invalidJsonBodyThrows() throws Exception {
        try {
            resolve("flatNamed", NamedBody.class, "not-json");
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("request body is not valid JSON", e.getMessage());
        }
    }

    @Test
    public void parseAllFieldsMapsOuterJson() throws Exception {
        NamedBody body = (NamedBody) resolve("flatNamed", NamedBody.class, "{\"name\":\"a\"}");
        Assert.assertEquals("a", body.getName());
    }

    private Object resolve(String methodName, Class<?> paramType, String jsonBody) throws Exception {
        Method method = SampleController.class.getDeclaredMethod(methodName, paramType);
        MethodParameter parameter = new MethodParameter(method, 0);
        parameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent(jsonBody.getBytes("UTF-8"));
        NativeWebRequest webRequest = new ServletWebRequest(request);
        return resolver.resolveArgument(parameter, null, webRequest, null);
    }

    static class SampleController {
        void requiredNamed(@MultiRequestBody(value = "requiredBody", required = true, parseAllFields = true) NamedBody body) {
        }

        void requiredUnparseable(@MultiRequestBody(value = "requiredBody", required = true, parseAllFields = true) UnparseableBody body) {
        }

        void optionalUnparseable(@MultiRequestBody(value = "optionalBody", required = false, parseAllFields = true) UnparseableBody body) {
        }

        void flatDateHolder(@MultiRequestBody(required = true, parseAllFields = true) DateHolder flatDateHolder) {
        }

        void flatOptionalDateHolder(@MultiRequestBody(required = false, parseAllFields = true) DateHolder flatOptionalDateHolder) {
        }

        void flatNamed(@MultiRequestBody(required = true, parseAllFields = true) NamedBody namedBody) {
        }
    }

    static class NamedBody {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static abstract class UnparseableBody {
        abstract String value();
    }

    static class DateHolder {
        private Date when;

        public Date getWhen() {
            return when;
        }

        public void setWhen(Date when) {
            this.when = when;
        }
    }
}
