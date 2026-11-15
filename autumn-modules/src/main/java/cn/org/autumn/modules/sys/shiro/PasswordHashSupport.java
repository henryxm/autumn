package cn.org.autumn.modules.sys.shiro;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.hash.SimpleHash;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 登录口令哈希：新口令使用 PBKDF2-HMAC-SHA256；旧 SHA-256×16 仍可校验并惰性升级。
 * <p>
 * 存储格式：{@code pbkdf2$<iterations>$<saltB64>$<hashB64>}；遗留为无此前缀的 hex（配合实体 salt 列）。
 */
public final class PasswordHashSupport {

    public static final String PBKDF2_PREFIX = "pbkdf2$";
    public static final int PBKDF2_ITERATIONS = 120_000;
    public static final int PBKDF2_KEY_LEN_BITS = 256;
    public static final int PBKDF2_SALT_BYTES = 16;

    private PasswordHashSupport() {
    }

    public static boolean isPbkdf2(String stored) {
        return StringUtils.isNotBlank(stored) && stored.startsWith(PBKDF2_PREFIX);
    }

    public static String hashNew(String rawPassword) {
        byte[] salt = new byte[PBKDF2_SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, PBKDF2_ITERATIONS);
        return PBKDF2_PREFIX + PBKDF2_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean matches(String rawPassword, String storedPassword, String legacySalt) {
        if (StringUtils.isBlank(rawPassword) || StringUtils.isBlank(storedPassword)) {
            return false;
        }
        if (isPbkdf2(storedPassword)) {
            return matchesPbkdf2(rawPassword, storedPassword);
        }
        return matchesLegacySha256(rawPassword, storedPassword, legacySalt);
    }

    /**
     * 仅校验遗留 SHA-256×16；已是 PBKDF2 或格式不符时返回 false（供惰性升级门禁，禁止误用超级密码等污染）。
     */
    public static boolean matchesLegacySha256(String rawPassword, String storedPassword, String legacySalt) {
        if (StringUtils.isBlank(rawPassword) || StringUtils.isBlank(storedPassword) || StringUtils.isBlank(legacySalt)) {
            return false;
        }
        if (isPbkdf2(storedPassword)) {
            return false;
        }
        String hashed = new SimpleHash(ShiroUtils.hashAlgorithmName, rawPassword, legacySalt, ShiroUtils.hashIterations).toString();
        return storedPassword.equals(hashed);
    }

    /**
     * 用用户提交的明文生成 PBKDF2；调用方必须已证明 {@link #matchesLegacySha256} 通过且明文来自用户输入。
     */
    public static String hashNewFromVerifiedUserInput(String rawPasswordFromUser) {
        if (StringUtils.isBlank(rawPasswordFromUser)) {
            throw new IllegalArgumentException("refuse hash: blank user password");
        }
        return hashNew(rawPasswordFromUser);
    }

    public static boolean matchesPbkdf2(String rawPassword, String stored) {
        try {
            String[] parts = stored.split("\\$");
            // pbkdf2$iter$salt$hash → 4 parts
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations);
            return slowEquals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, PBKDF2_KEY_LEN_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 failed", e);
        }
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
