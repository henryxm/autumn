package cn.org.autumn.modules.safe.support;

/**
 * 支付校验方式常量（verifyToken 元数据、allowedVerifyMethods）。
 */
public final class PayCredentialVerifyMethods {

    public static final String PIN = "PIN";
    public static final String BIO = "BIO";
    public static final String GESTURE = "GESTURE";
    public static final String GATE = "GATE";

    private PayCredentialVerifyMethods() {
    }
}
