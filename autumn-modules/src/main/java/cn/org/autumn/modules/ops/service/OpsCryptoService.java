package cn.org.autumn.modules.ops.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 运维通道加解密门面（独立于业务 {@code /rsa/api/v1} EncryptKey 会话）。
 */
@Service
public class OpsCryptoService {

    @Autowired
    private OpsSessionService opsSessionService;

    public String encrypt(OpsSessionService.OpsSession session, String plainJson) {
        return opsSessionService.encryptForClient(session, plainJson);
    }

    public String decrypt(OpsSessionService.OpsSession session, String ciphertext) {
        return opsSessionService.decryptFromClient(session, ciphertext);
    }
}
