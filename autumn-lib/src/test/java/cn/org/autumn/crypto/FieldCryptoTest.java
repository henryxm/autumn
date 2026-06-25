package cn.org.autumn.crypto;

import java.util.Arrays;
import java.util.Base64;
import org.junit.Assert;
import org.junit.Test;

public class FieldCryptoTest {

    private static final String PREFIX = "ENC$v1$";
    private static final byte[] KEY = new byte[32];

    static {
        Arrays.fill(KEY, (byte) 7);
    }

    @Test
    public void encryptDecryptRoundTrip() {
        String plain = "13800138000";
        String cipher = FieldCrypto.encrypt(plain, KEY, PREFIX, "");
        Assert.assertTrue(cipher.startsWith(PREFIX));
        Assert.assertNotEquals(plain, cipher);
        String decrypted = FieldCrypto.decrypt(cipher, KEY, PREFIX);
        Assert.assertEquals(plain, decrypted);
    }

    @Test
    public void emptyAndIdempotent() {
        Assert.assertEquals("", FieldCrypto.encrypt("", KEY, PREFIX, ""));
        String cipher = FieldCrypto.encrypt("abc", KEY, PREFIX, "");
        Assert.assertEquals(cipher, FieldCrypto.encrypt(cipher, KEY, PREFIX, ""));
    }

    @Test
    public void plaintextPassthroughOnDecrypt() {
        Assert.assertEquals("legacy", FieldCrypto.decrypt("legacy", KEY, PREFIX));
    }

    @Test
    public void hashDeterministic() {
        String h1 = FieldCrypto.hash("test", KEY);
        String h2 = FieldCrypto.hash("test", KEY);
        Assert.assertEquals(h1, h2);
        Assert.assertEquals(64, h1.length());
    }

    @Test
    public void decodeKeyBase64() {
        String b64 = Base64.getEncoder().encodeToString(KEY);
        byte[] decoded = FieldCrypto.decodeKeyBase64(b64);
        Assert.assertArrayEquals(KEY, decoded);
    }

    @Test
    public void generateRandomMaterial() {
        byte[] key = FieldCrypto.decodeKeyBase64(FieldCrypto.generateRandomKeyBase64());
        byte[] iv = Base64.getDecoder().decode(FieldCrypto.generateRandomVectorBase64());
        Assert.assertNotNull(key);
        Assert.assertEquals(32, key.length);
        Assert.assertEquals(12, iv.length);
    }
}
