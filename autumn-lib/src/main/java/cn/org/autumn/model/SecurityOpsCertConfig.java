package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 运维双边 RSA 证书配置（与 {@link SecurityOpsConfig} / 业务 {@code /rsa/api/v1} 隔离）。
 * <p>
 * 单机与集群共用同一份：服务端密钥对、客户端公钥、OpsEnroll salt/hash 均落本配置（sys_config）。
 * OpsEnroll 明文永不落库，仅启动/重置日志打印一次。
 */
@Getter
@Setter
@ConfigParam(paramKey = SecurityOpsCertConfig.CONFIG_KEY, category = SecurityOpsCertConfig.config,
        name = "运维证书", description = "运维通道服务端密钥对与已入驻客户端公钥；私钥仅服务端，勿下发")
public class SecurityOpsCertConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "SECURITY_OPS_CERT_CONFIG";
    public static final String config = "security_hardening_config";

    @ConfigField(category = InputType.StringType, name = "站点标识", description = "入驻返回的 siteId")
    private String siteId = "";

    @ConfigField(category = InputType.StringType, name = "服务端公钥", description = "Base64 X509 公钥")
    private String serverPublicKey = "";

    @ConfigField(category = InputType.StringType, name = "服务端私钥", description = "Base64 PKCS8 私钥（仅服务端）")
    private String serverPrivateKey = "";

    @ConfigField(category = InputType.StringType, name = "公钥指纹", description = "SHA-256 指纹（短展示）")
    private String fingerprint = "";

    @ConfigField(category = InputType.BooleanType, name = "入驻密钥已消费", description = "一次性 OpsEnroll 是否已用尽（仍可多设备用 reset 后重新入驻）")
    private boolean enrollSecretConsumed = false;

    @ConfigField(category = InputType.StringType, name = "入驻盐", description = "OpsEnroll salt Base64（无明文）")
    private String enrollSaltB64 = "";

    @ConfigField(category = InputType.StringType, name = "入驻哈希", description = "OpsEnroll SHA-256(salt+secret) Base64（无明文）")
    private String enrollHashB64 = "";

    @ConfigField(category = InputType.NumberType, name = "重置时间", description = "最近一次 reset 的 epoch ms")
    private long resetAt = 0L;

    private List<OpsClientCertEntry> clients = new ArrayList<>();

    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        if (siteId == null) {
            siteId = "";
        }
        if (serverPublicKey == null) {
            serverPublicKey = "";
        }
        if (serverPrivateKey == null) {
            serverPrivateKey = "";
        }
        if (fingerprint == null) {
            fingerprint = "";
        }
        if (enrollSaltB64 == null) {
            enrollSaltB64 = "";
        }
        if (enrollHashB64 == null) {
            enrollHashB64 = "";
        }
        if (clients == null) {
            clients = new ArrayList<>();
        }
        return fixes;
    }

    public boolean hasServerKeyPair() {
        return serverPublicKey != null && !serverPublicKey.isBlank() && serverPrivateKey != null && !serverPrivateKey.isBlank();
    }

    public boolean hasEnrollMaterial() {
        return enrollSaltB64 != null && !enrollSaltB64.isBlank() && enrollHashB64 != null && !enrollHashB64.isBlank();
    }
}
