package cn.org.autumn.modules.safe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class PayPinVerifyRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String pin;
    /** 支付闸门 assess 返回的 gateToken，启用闸门时建议必传 */
    private String gateToken;
    /** 与 assess 时一致的金额（分），启用闸门时建议必传 */
    private long amountCent;
}
