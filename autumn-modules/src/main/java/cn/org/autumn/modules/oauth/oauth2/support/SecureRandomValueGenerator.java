package cn.org.autumn.modules.oauth.oauth2.support;

import org.apache.oltu.oauth2.as.issuer.ValueGenerator;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * OAuth 令牌值生成：CSPRNG 不透明串，替代 Oltu {@code MD5Generator}。
 */
public class SecureRandomValueGenerator implements ValueGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generateValue() throws OAuthSystemException {
        return generateValue(null);
    }

    @Override
    public String generateValue(String param) throws OAuthSystemException {
        byte[] extra = new byte[16];
        RANDOM.nextBytes(extra);
        StringBuilder sb = new StringBuilder(64);
        sb.append(UUID.randomUUID().toString().replace("-", ""));
        for (byte b : extra) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
