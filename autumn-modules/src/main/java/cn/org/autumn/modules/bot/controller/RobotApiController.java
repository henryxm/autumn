package cn.org.autumn.modules.bot.controller;

import cn.org.autumn.annotation.Authenticated;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Request;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.bot.dto.*;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.service.RobotConfigService;
import cn.org.autumn.modules.bot.service.RobotHookService;
import cn.org.autumn.modules.bot.service.RobotService;
import cn.org.autumn.modules.bot.service.RobotTokenService;
import cn.org.autumn.modules.bot.support.RobotOpenApiLogSupport;
import cn.org.autumn.modules.sys.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 机器人管理开放 API：统一 {@link Request} / {@link Response}，无 Session，用户令牌鉴权。
 */
@Slf4j
@RestController
@RequestMapping("/robot/api/v1")
public class RobotApiController {

    @Autowired
    private RobotService robotService;

    @Autowired
    private RobotHookService robotHookService;

    @Autowired
    private RobotConfigService robotConfigService;

    @Autowired
    private RobotTokenService robotTokenService;

    @PostMapping("/list")
    @Authenticated
    public Response<RobotListResult> list(@Valid @RequestBody(required = false) Request<?> request, UserContext context, HttpServletRequest servlet) {
        try {
            List<RobotEntity> robots = robotService.listByOwner(requireOwner(context));
            List<User> users = robots == null ? Collections.emptyList() : robots.stream().map(robotService::toUser).collect(Collectors.toList());
            return Response.ok(RobotListResult.of(users));
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "机器人列表", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/create")
    @Authenticated
    public Response<RobotCreateResult> create(@Valid @RequestBody Request<RobotCreateRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            RobotCreateRequest data = request == null ? null : request.getData();
            String owner = requireOwner(context);
            String name = data == null ? null : data.getName();
            String description = data == null ? null : data.getDescription();
            String icon = data == null ? null : data.getIcon();
            Integer tokenExpireDays = data == null ? null : data.getTokenExpireDays();
            String access = data == null ? null : data.getAccess();
            RobotCreateResult result = robotService.create(owner, name, description, icon, tokenExpireDays, access);
            return Response.ok(result);
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "创建机器人", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/update")
    @Authenticated
    public Response<String> update(@Valid @RequestBody Request<RobotUpdateRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            RobotUpdateRequest data = request == null ? null : request.getData();
            if (data == null || StringUtils.isBlank(data.getUuid())) {
                throw new CodeException("机器人uuid不能为空");
            }
            robotService.updateProfile(data.getUuid(), requireOwner(context), data.getName(), data.getDescription(), data.getIcon(), data.getAccess(), data.getBlack());
            return Response.ok();
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "更新机器人", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/disable")
    @Authenticated
    public Response<String> disable(@Valid @RequestBody Request<RobotUuidRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            robotService.disable(requireUuid(request), requireOwner(context));
            return Response.ok();
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "停用机器人", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/enable")
    @Authenticated
    public Response<String> enable(@Valid @RequestBody Request<RobotUuidRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            robotService.enable(requireUuid(request), requireOwner(context));
            return Response.ok();
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "启用机器人", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/delete")
    @Authenticated
    public Response<String> delete(@Valid @RequestBody Request<RobotUuidRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            robotService.delete(requireUuid(request), requireOwner(context));
            return Response.ok();
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "删除机器人", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/destroy")
    @Authenticated
    public Response<String> destroy(@Valid @RequestBody Request<RobotUuidRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            robotConfigService.assertAdministrator(requireOperator(context));
            robotService.destroyByAdministrator(requireUuid(request));
            return Response.ok();
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "销毁机器人", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/hook/list")
    @Authenticated
    public Response<RobotHookListResult> hookList(@Valid @RequestBody Request<RobotUuidRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            return Response.ok(RobotHookListResult.of(robotHookService.listViewsForOwner(requireUuid(request), requireOwner(context))));
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "Hook列表", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/hook/create")
    @Authenticated
    public Response<RobotHookView> hookCreate(@Valid @RequestBody Request<RobotHookCreateRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            RobotHookCreateRequest data = request == null ? null : request.getData();
            if (data == null || StringUtils.isBlank(data.getRobot()))
                throw new CodeException("机器人uuid不能为空");
            RobotHookView hook = robotHookService.create(data.getRobot(), requireOwner(context), data.getName(), data.getCallbackUrl(), data.getSecret(), data.getEvents(), data.getDescription());
            return Response.ok(hook);
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "创建Hook", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/hook/update")
    @Authenticated
    public Response<RobotHookView> hookUpdate(@Valid @RequestBody Request<RobotHookUpdateRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            RobotHookUpdateRequest data = request == null ? null : request.getData();
            if (data == null || StringUtils.isBlank(data.getUuid()))
                throw new CodeException("Hook uuid不能为空");
            RobotHookView hook = robotHookService.update(data.getUuid(), requireOwner(context), data.getName(), data.getCallbackUrl(), data.getSecret(), data.getEvents(), data.getDescription());
            return Response.ok(hook);
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "更新Hook", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/hook/delete")
    @Authenticated
    public Response<String> hookDelete(@Valid @RequestBody Request<RobotHookUuidRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            robotHookService.delete(requireHookUuid(request), requireOwner(context));
            return Response.ok();
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "删除Hook", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/hook/disable")
    @Authenticated
    public Response<String> hookDisable(@Valid @RequestBody Request<RobotHookUuidRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            robotHookService.disable(requireHookUuid(request), requireOwner(context));
            return Response.ok();
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "停用Hook", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/hook/enable")
    @Authenticated
    public Response<String> hookEnable(@Valid @RequestBody Request<RobotHookUuidRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            robotHookService.enable(requireHookUuid(request), requireOwner(context));
            return Response.ok();
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "启用Hook", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/config/get")
    @Authenticated
    public Response<RobotConfigResult> configGet(@Valid @RequestBody(required = false) Request<RobotConfigUserRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            String userUuid = request == null || request.getData() == null ? null : request.getData().getUuid();
            return Response.ok(robotConfigService.getEffective(userUuid, requireOperator(context)));
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "查询配置", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/config/save")
    @Authenticated
    public Response<RobotConfigResult> configSave(@Valid @RequestBody Request<RobotConfigSaveRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            robotConfigService.assertAdministrator(requireOperator(context));
            RobotConfigSaveRequest data = request == null ? null : request.getData();
            if (data == null || StringUtils.isBlank(data.getUuid()))
                throw new CodeException("用户uuid不能为空");
            RobotConfigResult result = robotConfigService.save(requireOperator(context), data.getUuid(), data.getMaxRobots(), data.getMaxTokens(), data.getMaxHooks());
            return Response.ok(result);
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "保存配置", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/token/list")
    @Authenticated
    public Response<RobotTokenListResult> tokenList(@Valid @RequestBody Request<RobotUuidRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            return Response.ok(robotTokenService.listActiveResult(requireUuid(request), requireOwner(context)));
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "令牌列表", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/token/revoke")
    @Authenticated
    public Response<String> tokenRevoke(@Valid @RequestBody Request<RobotTokenUuidRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            robotTokenService.revoke(requireTokenUuid(request), requireOwner(context));
            return Response.ok();
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "作废令牌", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/token/create")
    @Authenticated
    public Response<RobotTokenResult> tokenCreate(@Valid @RequestBody Request<RobotTokenCreateRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            RobotTokenCreateRequest data = request == null ? null : request.getData();
            if (data == null || StringUtils.isBlank(data.getUuid()))
                throw new CodeException("机器人uuid不能为空");
            String token = robotService.createToken(data.getUuid(), requireOwner(context), data.getTokenExpireDays());
            if (StringUtils.isBlank(token))
                return Response.error("令牌生成失败");
            return Response.ok(RobotTokenResult.of(token));
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "创建令牌", e, servlet);
            return Response.error(e);
        }
    }

    @PostMapping("/token/rotate")
    @Authenticated
    public Response<RobotTokenResult> rotateToken(@Valid @RequestBody Request<RobotRotateTokenRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            RobotRotateTokenRequest data = request == null ? null : request.getData();
            if (data == null || StringUtils.isBlank(data.getUuid()))
                throw new CodeException("机器人uuid不能为空");
            String token = robotService.rotateToken(data.getUuid(), requireOwner(context), data.getTokenExpireDays());
            if (StringUtils.isBlank(token))
                return Response.error("令牌生成失败");
            return Response.ok(RobotTokenResult.of(token));
        } catch (Exception e) {
            RobotOpenApiLogSupport.logFailure(log, "轮换令牌", e, servlet);
            return Response.error(e);
        }
    }

    private String requireUuid(Request<RobotUuidRequest> request) throws CodeException {
        if (request == null || request.getData() == null || StringUtils.isBlank(request.getData().getUuid()))
            throw new CodeException("机器人uuid不能为空");
        return request.getData().getUuid();
    }

    private String requireHookUuid(Request<RobotHookUuidRequest> request) throws CodeException {
        if (request == null || request.getData() == null || StringUtils.isBlank(request.getData().getUuid()))
            throw new CodeException("Hook uuid不能为空");
        return request.getData().getUuid();
    }

    private String requireTokenUuid(Request<RobotTokenUuidRequest> request) throws CodeException {
        if (request == null || request.getData() == null || StringUtils.isBlank(request.getData().getUuid()))
            throw new CodeException("令牌uuid不能为空");
        return request.getData().getUuid();
    }

    private String requireOwner(UserContext context) throws CodeException {
        if (context == null)
            throw new CodeException("请登录", -10000);
        if (context.isRobot())
            throw new CodeException("请使用用户令牌管理机器人", -10000);
        if (StringUtils.isBlank(context.getSubject()))
            throw new CodeException("用户不可用", -10000);
        return context.getSubject();
    }

    private String requireOperator(UserContext context) throws CodeException {
        if (context == null)
            throw new CodeException("请登录", -10000);
        if (context.isRobot())
            throw new CodeException("请使用用户令牌", -10000);
        if (StringUtils.isBlank(context.getUuid()))
            throw new CodeException("用户不可用", -10000);
        return context.getUuid();
    }
}
