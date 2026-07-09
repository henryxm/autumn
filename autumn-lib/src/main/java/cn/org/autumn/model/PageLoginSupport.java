package cn.org.autumn.model;

/** 登录页展示位标志：0 不展示；1 Tab；2 扫码；3 Tab+扫码。 */
public final class PageLoginSupport {

    public static final int NONE = 0;
    public static final int TAB = 1;
    public static final int QR = 2;
    public static final int TAB_AND_QR = 3;

    private PageLoginSupport() {
    }

    public static int parse(Integer value) {
        if (value == null) {
            return NONE;
        }
        int v = value;
        if (v < NONE || v > TAB_AND_QR) {
            return NONE;
        }
        return v;
    }

    public static boolean showTab(int pageLogin) {
        return pageLogin == TAB || pageLogin == TAB_AND_QR;
    }

    public static boolean showQr(int pageLogin) {
        return pageLogin == QR || pageLogin == TAB_AND_QR;
    }
}
