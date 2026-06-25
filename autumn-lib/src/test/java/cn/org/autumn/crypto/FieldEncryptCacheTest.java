package cn.org.autumn.crypto;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.config.FieldEncryptProperties;
import cn.org.autumn.table.annotation.Column;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FieldEncryptCacheTest {

    private static final byte[] KEY = new byte[32];

    static {
        Arrays.fill(KEY, (byte) 9);
    }

    private FieldEncryptService service;

    @Before
    public void setUp() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setEnabled(true);
        properties.setKey(Base64.getEncoder().encodeToString(KEY));
        properties.setHashKey(Base64.getEncoder().encodeToString(KEY));
        service = FieldEncryptTestSupport.newService(properties);
        service.registerEntity(ApiCredentialEntity.class);
    }

    @Test
    public void resolveCacheDbLookupUsesHashColumnForPlainKey() {
        FieldEncryptCacheLookup lookup = service.resolveCacheDbLookup(ApiCredentialEntity.class, "apiKey", "sk-live-abc");
        Assert.assertEquals("api_key_hash", lookup.getColumnName());
        Assert.assertEquals(service.hashValueForced("sk-live-abc"), lookup.getQueryValue());
    }

    @Test
    public void resolveCacheDbLookupUsesHashColumnForHashFieldCache() {
        String hash = service.hashValueForced("sk-live-abc");
        FieldEncryptCacheLookup lookup = service.resolveCacheDbLookup(ApiCredentialEntity.class, "apiKeyHash", hash);
        Assert.assertEquals("api_key_hash", lookup.getColumnName());
        Assert.assertEquals(hash, lookup.getQueryValue());
    }

    @Test
    public void normalizeCacheEvictionKeyDecryptsCipher() {
        String cipher = service.encryptValueForced("plain-key", "");
        Object key = service.normalizeCacheEvictionKey(ApiCredentialEntity.class, "apiKey", cipher);
        Assert.assertEquals("plain-key", key);
    }

    @Test
    public void resolveCacheEvictionKeysIncludesPlainAndHashChannels() {
        ApiCredentialEntity entity = new ApiCredentialEntity();
        entity.apiKey = service.encryptValueForced("plain-key", "");
        entity.apiKeyHash = service.hashValueForced("plain-key");
        List<FieldEncryptCacheKey> keys = service.resolveCacheEvictionKeys(ApiCredentialEntity.class, "apiKey", "", entity);
        Assert.assertTrue(keys.stream().anyMatch(k -> "".equals(k.getNaming()) && "plain-key".equals(k.getKey())));
        Assert.assertTrue(keys.stream().anyMatch(k -> FieldEncryptService.HASH_CACHE_CHANNEL.equals(k.getNaming()) && entity.apiKeyHash.equals(k.getKey())));
    }

    @Test
    public void resolveCacheMirrorKeysPlainToHash() {
        List<FieldEncryptCacheKey> mirrors = service.resolveCacheMirrorKeys(ApiCredentialEntity.class, "apiKey", "", "plain-key", null);
        Assert.assertEquals(1, mirrors.size());
        Assert.assertEquals(FieldEncryptService.HASH_CACHE_CHANNEL, mirrors.get(0).getNaming());
        Assert.assertEquals(service.hashValueForced("plain-key"), mirrors.get(0).getKey());
    }

    @Test
    public void hashFieldCacheEvictionAlsoClearsPlainChannel() {
        ApiCredentialEntity entity = new ApiCredentialEntity();
        entity.apiKey = service.encryptValueForced("plain-key", "");
        entity.apiKeyHash = service.hashValueForced("plain-key");
        List<FieldEncryptCacheKey> keys = service.resolveCacheEvictionKeys(ApiCredentialEntity.class, "apiKeyHash", FieldEncryptService.HASH_CACHE_CHANNEL, entity);
        Assert.assertTrue(keys.stream().anyMatch(k -> FieldEncryptService.HASH_CACHE_CHANNEL.equals(k.getNaming())));
        Assert.assertTrue(keys.stream().anyMatch(k -> "".equals(k.getNaming()) && "plain-key".equals(k.getKey())));
    }

    @Test
    public void isEncryptCacheFieldDetectsEncryptAndHashOnly() {
        Assert.assertTrue(service.isEncryptCacheField(ApiCredentialEntity.class, "apiKey"));
        Assert.assertTrue(service.isEncryptCacheField(ApiCredentialEntity.class, "apiKeyHash"));
        Assert.assertFalse(service.isEncryptCacheField(ApiCredentialEntity.class, "uuid"));
        Assert.assertFalse(service.isEncryptCacheField(ApiCredentialEntity.class, "unknown"));
    }

    static class ApiCredentialEntity {
        @Column
        @Cache
        String uuid;

        @Column
        @Cache
        @FieldEncrypt(searchable = true)
        String apiKey;

        @Column(length = 64)
        @Cache(name = FieldEncryptService.HASH_CACHE_CHANNEL)
        String apiKeyHash;
    }
}
