package cn.org.autumn.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 已入驻运维客户端公钥条目（存于 {@link SecurityOpsCertConfig#getClients()}）。
 */
@Getter
@Setter
public class OpsClientCertEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String certId;
    private String clientPublicKey;
    private String label;
    private long createdAt;
    private long lastUsedAt;
    private boolean revoked;
}
