package cn.org.autumn.modules.open.support;

import cn.org.autumn.model.UserContext;
import cn.org.autumn.utils.Uuid;
import org.apache.commons.lang.StringUtils;

/** 开放模块用户 API 公共工具。 */
public final class OpenApiUsers {

    private OpenApiUsers() {
    }

    public static String requireUser(UserContext context) {
        if (context == null || StringUtils.isBlank(context.getUuid())) {
            throw new IllegalStateException("未登录");
        }
        return Uuid.requireValid(context.getUuid());
    }
}
