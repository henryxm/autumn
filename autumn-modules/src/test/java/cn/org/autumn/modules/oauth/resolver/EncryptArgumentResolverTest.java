package cn.org.autumn.modules.oauth.resolver;

import cn.org.autumn.model.CompatibleRequest;
import com.google.gson.Gson;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class EncryptArgumentResolverTest {

    @Test
    @SuppressWarnings("unchecked")
    public void shouldKeepLongPrecisionWhenUsingSuperResolverPath() throws Exception {
        EncryptArgumentResolver resolver = newResolver();

        long expected = 9007199254740993L;
        String body = "{\"data\":{\"id\":" + expected + "}}";
        NativeWebRequest webRequest = webRequest(body);

        Method method = DummyController.class.getDeclaredMethod("objectCompatible", CompatibleRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        Object resolved = resolver.resolveArgument(parameter, new ModelAndViewContainer(), webRequest, null);
        assertTrue(resolved instanceof CompatibleRequest);

        CompatibleRequest<Object> request = (CompatibleRequest<Object>) resolved;
        assertNotNull(request.getData());
        assertTrue(request.getData() instanceof Map);

        Map<String, Object> data = (Map<String, Object>) request.getData();
        Object id = data.get("id");
        assertTrue("id should be integral type, but was: " + id.getClass(), id instanceof Long || id instanceof Integer);
        assertEquals(expected, ((Number) id).longValue());
        assertFalse("id should not degrade to Double", id instanceof Double);
    }

    private EncryptArgumentResolver newResolver() {
        HttpMessageConverter<?> converter = new MappingJackson2HttpMessageConverter();
        EncryptArgumentResolver resolver = new EncryptArgumentResolver(Collections.singletonList(converter));
        ReflectionTestUtils.setField(resolver, "gson", new Gson());
        return resolver;
    }

    private NativeWebRequest webRequest(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return new ServletWebRequest(request);
    }

    static class DummyController {
        public void objectCompatible(@RequestBody CompatibleRequest<Object> request) {
        }
    }
}
