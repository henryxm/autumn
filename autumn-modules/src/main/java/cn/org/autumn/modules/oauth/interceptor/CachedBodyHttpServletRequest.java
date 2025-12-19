package cn.org.autumn.modules.oauth.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 缓存请求体的HttpServletRequest包装器
 * 解决请求体只能读取一次的问题
 * <p>
 * 优化说明：
 * 1. 添加连接状态检查，避免在连接已关闭时读取
 * 2. 优化异常处理，提供更清晰的错误信息
 * 3. 添加请求体大小检查
 *
 * @author Autumn
 */
@Slf4j
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    /**
     * 最大请求体大小（10MB）
     */
    private static final int MAX_BODY_SIZE = 10 * 1024 * 1024;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);

        // 检查Content-Length
        long contentLength = request.getContentLengthLong();
        if (contentLength > MAX_BODY_SIZE) {
            throw new IOException(String.format("请求体过大: %d字节，超过限制: %d字节", contentLength, MAX_BODY_SIZE));
        }

        // 读取并缓存请求体
        try {
            ServletInputStream inputStream = request.getInputStream();
            if (inputStream == null) {
                this.body = new byte[0];
                return;
            }

            // 使用StreamUtils安全地读取请求体
            // 如果连接已关闭，这里会抛出IOException
            this.body = StreamUtils.copyToByteArray(inputStream);
        } catch (java.net.SocketException e) {
            // Socket异常（包括Broken pipe），通常是客户端提前关闭连接
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("Broken pipe") || errorMsg.contains("Connection reset"))) {
                if (log.isDebugEnabled()) {
                    log.debug("客户端连接已关闭，无法读取请求体: {}", errorMsg);
                }
                // 抛出更明确的异常
                throw new IOException("客户端连接已关闭，无法读取请求体", e);
            }
            throw e;
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedBodyServletInputStream(this.body);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.body);
        return new BufferedReader(new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8));
    }

    /**
     * 获取缓存的请求体内容（字符串形式）
     *
     * @return 请求体字符串，如果为空则返回空字符串
     */
    public String getBody() {
        if (this.body == null || this.body.length == 0) {
            return "";
        }
        return new String(this.body, StandardCharsets.UTF_8);
    }

    /**
     * 获取缓存的请求体字节数组
     *
     * @return 请求体字节数组
     */
    public byte[] getBodyBytes() {
        return this.body != null ? this.body.clone() : new byte[0];
    }

    /**
     * 自定义ServletInputStream实现
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream buffer;

        public CachedBodyServletInputStream(byte[] contents) {
            this.buffer = new ByteArrayInputStream(contents);
        }

        @Override
        public int read() throws IOException {
            return buffer.read();
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException();
        }
    }
}
