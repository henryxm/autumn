package cn.org.autumn.modules.bot.controller;

import cn.org.autumn.annotation.Authenticated;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Request;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.bot.dto.RobotMessagePushRequest;
import cn.org.autumn.modules.bot.dto.RobotMessagePushResult;
import cn.org.autumn.modules.bot.service.RobotMessageService;
import cn.org.autumn.modules.bot.support.RobotOpenApiLogSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 机器人入站 API：使用 {@code rbt_} 令牌调用（与 {@link RobotApiController} 用户管理 API 分离）。
 */
@Slf4j
@RestController
@RequestMapping("/robot/api/v1")
public class RobotInboundApiController {

    @Autowired
    private RobotMessageService robotMessageService;

    /**
     * 外部推送消息：按 type 分发给系统内 {@link cn.org.autumn.handler.RobotMessageSubscriber} 与已订阅该事件的 Hook。
     */
    @PostMapping("/message/push")
    @Authenticated
    public Response<RobotMessagePushResult> push(@Valid @RequestBody Request<RobotMessagePushRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            RobotMessagePushRequest body = request == null ? null : request.getData();
            String type = body == null ? null : body.getType();
            String payload = body == null ? null : body.getData();
            String idempotencyKey = body == null ? null : body.getMessageId();
            if (StringUtils.isBlank(idempotencyKey))
                idempotencyKey = servlet.getHeader("X-Robot-Message-Id");
            RobotMessagePushResult result = robotMessageService.push(
                    requireRobot(context), requireOwner(context), type, payload, idempotencyKey);
            return Response.ok(result);
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "机器人消息推送", e, servlet);
            return Response.error(e);
        }
    }

    private String requireRobot(UserContext context) throws CodeException {
        if (context == null)
            throw new CodeException("请登录", -10000);
        if (!context.isRobot())
            throw new CodeException("请使用机器人访问令牌", -10000);
        if (!context.isActive())
            throw new CodeException("机器人未启用");
        if (StringUtils.isBlank(context.getUuid()))
            throw new CodeException("机器人不可用");
        return context.getUuid();
    }

    private String requireOwner(UserContext context) throws CodeException {
        if (StringUtils.isBlank(context.getSubject()))
            throw new CodeException("机器人不可用");
        return context.getSubject();
    }
}
