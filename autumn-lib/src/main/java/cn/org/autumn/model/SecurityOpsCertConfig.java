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
        name = "运维证书", description = "运维通道的服务端密钥对与已入驻设备公钥；私钥仅存服务端，禁止下发到客户端")
public class SecurityOpsCertConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "SECURITY_OPS_CERT_CONFIG";
    public static final String config = "security_hardening_config";

    @ConfigField(category = InputType.StringType, name = "站点标识", description = "站点唯一标识，入驻成功后返回给运维 App")
    private String siteId = "";

    @ConfigField(category = InputType.StringType, name = "服务端公钥", description = "服务端 RSA 公钥（Base64，X.509 编码）")
    private String serverPublicKey = "";

    @ConfigField(category = InputType.StringType, name = "服务端私钥", description = "服务端 RSA 私钥（Base64，PKCS8；仅服务端持有）")
    private String serverPrivateKey = "";

    @ConfigField(category = InputType.StringType, name = "公钥指纹", description = "服务端公钥 SHA-256 指纹，用于 App 核对站点")
    private String fingerprint = "";

    @ConfigField(category = InputType.BooleanType, name = "入驻密钥已消费",
            description = "一次性入驻口令是否已用尽；用尽后需通过 wall/数据库重置才能重新入驻，多设备请用已登录设备签发认领码")
    private boolean enrollSecretConsumed = false;

    @ConfigField(category = InputType.StringType, name = "入驻盐值", description = "入驻口令盐值（Base64，库中不存明文口令）")
    private String enrollSaltB64 = "";

    @ConfigField(category = InputType.StringType, name = "入驻哈希",
            description = "入驻口令哈希 SHA-256(盐值+口令) 的 Base64；核对后不回显明文")
    private String enrollHashB64 = "";

    @ConfigField(category = InputType.NumberType, name = "最近重置时间", description = "最近一次运维证书重置的时间戳（毫秒）")
    private long resetAt = 0L;

    /**
     * 是否开放 {@code /ops/discover} 公开落地页；默认 true；证书绑定成功后自动 false。
     */
    @ConfigField(category = InputType.BooleanType, name = "站点发现页",
            description = "关闭后浏览器访问发现页仅提示用超然信管理；可用运维 App 再打开")
    private boolean discoverEnabled = true;

    private List<OpsClientCertEntry> clients = new ArrayList<>();

    /** 已登录设备签发的待认领码（明文不落库）。 */
    private List<OpsPendingClaimEntry> pendingClaims = new ArrayList<>();

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
        if (pendingClaims == null) {
            pendingClaims = new ArrayList<>();
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
