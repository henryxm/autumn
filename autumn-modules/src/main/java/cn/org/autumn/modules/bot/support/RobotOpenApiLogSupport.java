package cn.org.autumn.modules.bot.support;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.utils.IPUtils;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 机器人开放 API 日志：业务 {@link CodeException} 记 WARN，系统异常记 ERROR。
 */
@Slf4j
public final class RobotOpenApiLogSupport {

    private RobotOpenApiLogSupport() {
    }

    public static void logFailure(String action, Exception e, HttpServletRequest servlet) {
        String ip = IPUtils.getIp(servlet);
        if (e instanceof CodeException)
            log.warn("{}: {}, IP:{}", action, e.getMessage(), ip);
        else
            log.error("{}: {}, IP:{}", action, e.getMessage(), ip, e);
    }
}
