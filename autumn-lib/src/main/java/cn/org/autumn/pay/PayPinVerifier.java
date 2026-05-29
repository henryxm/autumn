package cn.org.autumn.pay;

/**
 * 业务仓支付前校验契约（由 safe 模块 PayUserPinService 实现并注册为 Spring Bean）。
 */
public interface PayPinVerifier {

    /**
     * 校验 6 位支付密码，失败抛 {@link cn.org.autumn.exception.CodeException}。
     */
    void requireVerified(String userUuid, String pin) throws Exception;

    /**
     * 消费 {@code /safe/api/v1/pin/verify} 或生物 verify 返回的单次令牌。
     */
    void requireVerifyToken(String userUuid, String verifyToken) throws Exception;

    /**
     * 消费校验令牌并校验金额/订单（启用 {@code verifyTokenBindAmount} 时生效）。
     */
    void requireVerifyToken(String userUuid, String verifyToken, long amountCent, String orderId) throws Exception;

    /**
     * 校验并消费 {@code /safe/api/v1/gate/assess} 返回的闸门令牌；金额须与评估时一致（单位：分）。
     */
    void requireGateToken(String userUuid, String gateToken, long amountCent) throws Exception;
}
