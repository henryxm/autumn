package cn.org.autumn.modules.oauth.interceptor;

import cn.org.autumn.model.CompatibleResponse;
import cn.org.autumn.model.Response;
import cn.org.autumn.service.AesService;
import cn.org.autumn.service.RsaService;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("null")
public class EncryptInterceptorTest {

    @InjectMocks
    private EncryptInterceptor interceptor;

    @Mock
    private AesService aesService;

    @Mock
    private RsaService rsaService;

    @Mock
    private Gson gson;

    private final ServerHttpResponse serverHttpResponse = mock(ServerHttpResponse.class);

    @Test
    public void shouldReturnPlainDataWhenCompatibleResponseDeclaredAndNoSession() throws Exception {
        Method method = Objects.requireNonNull(DummyController.class.getDeclaredMethod("compatibleEndpoint"));
        MethodParameter returnType = new MethodParameter(method, -1);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/demo/compatible");

        Object result = interceptor.beforeBodyWrite(
                "plain-data",
                returnType,
                MediaType.APPLICATION_JSON,
                rawConverterType(),
                new ServletServerHttpRequest(servletRequest),
                Objects.requireNonNull(serverHttpResponse)
        );

        assertTrue(result instanceof String);
        assertEquals("plain-data", result);
    }

    @Test
    public void shouldUnwrapCompatibleResponseDataWhenNoSession() throws Exception {
        Method method = Objects.requireNonNull(DummyController.class.getDeclaredMethod("compatibleEndpoint"));
        MethodParameter returnType = new MethodParameter(method, -1);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/demo/compatible");

        CompatibleResponse<String> body = new CompatibleResponse<>();
        body.setCode(0);
        body.setMsg("success");
        body.setData("payload");
        Object result = interceptor.beforeBodyWrite(
                body,
                returnType,
                MediaType.APPLICATION_JSON,
                rawConverterType(),
                new ServletServerHttpRequest(servletRequest),
                Objects.requireNonNull(serverHttpResponse)
        );

        assertTrue(result instanceof String);
        assertEquals("payload", result);
    }

    @Test
    public void shouldKeepOriginalBodyWhenNotCompatibleAndNoSession() throws Exception {
        Method method = Objects.requireNonNull(DummyController.class.getDeclaredMethod("plainEndpoint"));
        MethodParameter returnType = new MethodParameter(method, -1);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/demo/plain");

        Object result = interceptor.beforeBodyWrite(
                "plain-data",
                returnType,
                MediaType.APPLICATION_JSON,
                rawConverterType(),
                new ServletServerHttpRequest(servletRequest),
                Objects.requireNonNull(serverHttpResponse)
        );

        assertTrue(result instanceof String);
        assertEquals("plain-data", result);
    }

    @Test
    public void shouldEncryptResponseWhenSessionHeaderPresent() throws Exception {
        Method method = Objects.requireNonNull(DummyController.class.getDeclaredMethod("responseEndpoint"));
        MethodParameter returnType = new MethodParameter(method, -1);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/demo/encrypt");
        servletRequest.addHeader("X-Encrypt-Session", "session-1");

        Response<String> original = Response.ok("payload");
        when(gson.toJson(original)).thenReturn("{\"code\":0,\"msg\":\"success\",\"data\":\"payload\"}");
        when(aesService.encrypt(anyString(), eq("session-1"))).thenReturn("cipher-text");

        Object result = interceptor.beforeBodyWrite(
                original,
                returnType,
                MediaType.APPLICATION_JSON,
                rawConverterType(),
                new ServletServerHttpRequest(servletRequest),
                Objects.requireNonNull(serverHttpResponse)
        );

        assertTrue(result instanceof Response);
        Response<?> encrypted = (Response<?>) result;
        assertEquals(0, encrypted.getCode());
        assertEquals("success", encrypted.getMsg());
        assertNull(encrypted.getData());
        assertEquals("cipher-text", encrypted.getCiphertext());
        assertEquals("AES", encrypted.getAlgorithm());
        assertEquals("session-1", encrypted.getSession());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Class<? extends HttpMessageConverter<?>> rawConverterType() {
        return (Class) HttpMessageConverter.class;
    }

    static class DummyController {
        public CompatibleResponse<String> compatibleEndpoint() {
            return null;
        }

        public String plainEndpoint() {
            return null;
        }

        public Response<String> responseEndpoint() {
            return null;
        }
    }
}
