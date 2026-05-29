package cn.org.autumn.modules.safe.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Min;
import java.io.Serializable;

/**
 * 支付闸门评估请求（支付密码输入前提交）。
 */
@Getter
@Setter
public class PayGateAssessRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 支付金额，单位：分（10元 = 1000） */
    @Min(1)
    private long amountCent;

    private String currency;
    private String reason;
    private String orderId;
    private String merchantId;
    private String payScene;
    private String deviceId;
    private String deviceFingerprint;
    private String platform;
    /** 客户端上报地点，如城市名或 lat,lng */
    private String location;
    /** 运行环境摘要 JSON 字符串 */
    private String environment;
    /** 客户端时间戳毫秒，可选 */
    private Long clientTime;
}
