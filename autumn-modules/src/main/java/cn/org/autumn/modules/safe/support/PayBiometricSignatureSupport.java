package cn.org.autumn.modules.safe.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public final class PayBiometricSignatureSupport {

    private PayBiometricSignatureSupport() {
    }

    public static boolean verifySha256Rsa(String publicKeyBase64, String challenge, String signatureBase64) {
        if (StringUtils.isBlank(publicKeyBase64) || StringUtils.isBlank(challenge) || StringUtils.isBlank(signatureBase64))
            return false;
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64.trim());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(challenge.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64.trim()));
        } catch (Exception e) {
            log.debug("生物识别验签失败: {}", e.getMessage());
            return false;
        }
    }
}
