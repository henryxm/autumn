package cn.org.autumn.modules.oauth.oauth2.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.apache.commons.lang.StringUtils;

/**
 * OAuth2 PKCE（RFC 7636）校验。
 */
public final class PkceSupport {

    public static final String METHOD_S256 = "S256";

    private PkceSupport() {
    }

    public static boolean requiresVerifier(String codeChallenge) {
        return StringUtils.isNotBlank(codeChallenge);
    }

    public static void validateVerifier(String codeChallenge, String codeChallengeMethod, String codeVerifier) {
        if (!requiresVerifier(codeChallenge)) {
            return;
        }
        if (StringUtils.isBlank(codeVerifier)) {
            throw new IllegalArgumentException("code_verifier不能为空");
        }
        String method = StringUtils.defaultIfBlank(codeChallengeMethod, METHOD_S256);
        if (!METHOD_S256.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("不支持的code_challenge_method");
        }
        String expected = s256Challenge(codeVerifier);
        if (!StringUtils.equals(codeChallenge, expected)) {
            throw new IllegalArgumentException("code_verifier无效");
        }
    }

    public static String s256Challenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256不可用", e);
        }
    }
}
