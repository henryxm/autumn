package cn.org.autumn.modules.auth.support;

import cn.org.autumn.auth.model.AuthRealNameInfo;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** 实名资源 URI / 响应体解析与 AS 出口响应构造。 */
public final class AuthRealNameHttpSupport {

    private AuthRealNameHttpSupport() {
    }

    public static String deriveRealNameUri(String userInfoUri) {
        if (StringUtils.isBlank(userInfoUri)) {
            return null;
        }
        String trimmed = userInfoUri.trim();
        if (trimmed.endsWith("userInfo")) {
            return trimmed.substring(0, trimmed.length() - "userInfo".length()) + "realName";
        }
        if (trimmed.endsWith("userinfo")) {
            return trimmed.substring(0, trimmed.length() - "userinfo".length()) + "realName";
        }
        return null;
    }

    public static AuthRealNameInfo parseBody(String body) {
        if (StringUtils.isBlank(body) || "{}".equals(body.trim())) {
            return null;
        }
        JSONObject json = JSON.parseObject(body);
        if (json == null || json.containsKey("error") || json.isEmpty()) {
            return null;
        }
        return json.toJavaObject(AuthRealNameInfo.class);
    }

    public static ResponseEntity<String> insufficientScope() {
        return new ResponseEntity<>("{\"error\":\"insufficient_scope\"}", HttpStatus.FORBIDDEN);
    }

    public static ResponseEntity<String> unsupported() {
        return new ResponseEntity<>("{\"error\":\"unsupported\"}", HttpStatus.NOT_IMPLEMENTED);
    }

    public static ResponseEntity<String> emptyOk() {
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

    public static ResponseEntity<String> ok(AuthRealNameInfo info) {
        if (info == null) {
            return emptyOk();
        }
        return new ResponseEntity<>(JSON.toJSONString(info), HttpStatus.OK);
    }
}
