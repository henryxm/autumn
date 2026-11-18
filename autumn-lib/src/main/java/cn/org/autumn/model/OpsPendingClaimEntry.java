package cn.org.autumn.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 已登录设备签发的一次性认领码材料（明文 secret 不落库，仅 salt+hash）。
 */
@Getter
@Setter
public class OpsPendingClaimEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String claimId;
    private String saltB64;
    private String secretHashB64;
    private String label;
    private String issuedByCertId;
    private long expiresAt;
    private boolean consumed;
}
