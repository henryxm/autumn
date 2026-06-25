package cn.org.autumn.modules.safe.support;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 支付凭证哈希、格式校验与弱码检测。
 */
public final class PayCredentialSupport {

    public static final String PIN_PEPPER = "autumn-safe-pin";
    public static final String GESTURE_PEPPER = "autumn-safe-gesture";

    private static final Set<String> WEAK_PINS = new HashSet<>(Arrays.asList(
            "123456", "654321", "111111", "222222", "333333", "444444", "555555",
            "666666", "777777", "888888", "999999", "000000", "121212", "112233"));

    private PayCredentialSupport() {
    }

    public static String newSalt() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    public static String hashPin(String pin, String salt) {
        return ShiroUtils.sha256(pin + PIN_PEPPER, salt);
    }

    public static String hashGesture(String canonical, String salt) {
        return ShiroUtils.sha256(canonical + GESTURE_PEPPER, salt);
    }

    public static void validatePinFormat(String pin, PayCredentialConfig config) throws CodeException {
        if (config == null)
            config = new PayCredentialConfig();
        int len = config.getPinLength() > 0 ? config.getPinLength() : 6;
        if (StringUtils.isBlank(pin))
            throw new CodeException(Error.PAY_PIN_FORMAT);
        Pattern pattern = Pattern.compile("^\\d{" + len + "}$");
        if (!pattern.matcher(pin).matches())
            throw new CodeException(Error.PAY_PIN_FORMAT);
        if (WEAK_PINS.contains(pin))
            throw new CodeException(Error.PAY_PIN_WEAK);
    }

    public static void assertPinConfirm(String pin, String confirm) throws CodeException {
        if (!StringUtils.equals(pin, confirm))
            throw new CodeException(Error.PAY_PIN_MISMATCH);
    }

    public static String canonicalizeGesture(int[] points, PayCredentialConfig config) throws CodeException {
        if (points == null || points.length == 0)
            throw new CodeException(Error.PAY_GESTURE_INVALID);
        int min = config == null || config.getGestureMinPoints() <= 0 ? 4 : config.getGestureMinPoints();
        if (points.length < min)
            throw new CodeException(Error.PAY_GESTURE_INVALID);
        StringBuilder sb = new StringBuilder();
        int prev = -1;
        for (int p : points) {
            if (p < 0 || p > 8)
                throw new CodeException(Error.PAY_GESTURE_INVALID);
            if (p == prev)
                throw new CodeException(Error.PAY_GESTURE_INVALID);
            if (sb.length() > 0)
                sb.append('-');
            sb.append(p);
            prev = p;
        }
        return sb.toString();
    }

    public static void assertGestureConfirm(int[] points, int[] confirm, PayCredentialConfig config) throws CodeException {
        String a = canonicalizeGesture(points, config);
        String b = canonicalizeGesture(confirm, config);
        if (!StringUtils.equals(a, b))
            throw new CodeException(Error.PAY_GESTURE_INVALID);
    }
}
