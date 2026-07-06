package cn.org.autumn.modules.opl.controller;

import cn.org.autumn.annotation.Authenticated;
import cn.org.autumn.model.Request;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.open.support.OpenApiUsers;
import cn.org.autumn.modules.opl.dto.OplAppRegisterRequest;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAccountSnapshot;
import cn.org.autumn.opl.model.OpenAppRegisterOutcome;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.model.OpenAppType;
import cn.org.autumn.opl.spi.OpenPlatformService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开发者自助 Open API：登录用户管理自己的开发者账号与应用。
 */
@Slf4j
@RestController
@RequestMapping(OplConstants.API_PLATFORM)
public class OplApiController {

    @Autowired
    private OpenPlatformService openPlatformService;

    @PostMapping("/account/open")
    @Authenticated
    public Response<Map<String, Object>> openAccount(@Valid @RequestBody(required = false) Request<?> request, UserContext context, HttpServletRequest servlet) {
        try {
            OpenAccountSnapshot account = openPlatformService.getOrCreateAccount(OpenApiUsers.requireUser(context), "开发者");
            Map<String, Object> data = new HashMap<>();
            data.put("uuid", account.getUuid());
            data.put("name", account.getName());
            data.put("status", account.getStatus());
            return Response.ok(data);
        } catch (Exception e) {
            log.error("opl open account failed: {}", e.getMessage());
            return Response.error(e);
        }
    }

    @PostMapping("/app/register")
    @Authenticated
    public Response<OpenAppRegisterOutcome> registerApp(@Valid @RequestBody Request<OplAppRegisterRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            OpenAccountSnapshot account = openPlatformService.getOrCreateAccount(OpenApiUsers.requireUser(context), "开发者");
            OplAppRegisterRequest data = request == null ? null : request.getData();
            OpenAppRegisterOutcome result = openPlatformService.registerApp(
                    account.getUuid(),
                    data == null ? null : data.getName(),
                    data == null ? null : data.getRedirectUri(),
                    data == null ? null : data.getScope(),
                    data == null ? null : data.getAppType());
            return Response.ok(result);
        } catch (Exception e) {
            log.error("opl register app failed: {}", e.getMessage());
            return Response.error(e);
        }
    }

    @PostMapping("/" + OplConstants.NS + "/app/list")
    @Authenticated
    public Response<List<OpenAppSnapshot>> listApps(@Valid @RequestBody(required = false) Request<?> request, UserContext context, HttpServletRequest servlet) {
        try {
            OpenAccountSnapshot account = openPlatformService.getAccountByUser(OpenApiUsers.requireUser(context));
            if (account == null) {
                return Response.ok(openPlatformService.listAppsByAccount(null));
            }
            return Response.ok(openPlatformService.listAppsByAccount(account.getUuid()));
        } catch (Exception e) {
            log.error("opl list apps failed: {}", e.getMessage());
            return Response.error(e);
        }
    }

    @PostMapping("/app/resetSecret")
    @Authenticated
    public Response<OpenAppRegisterOutcome> resetSecret(@Valid @RequestBody Request<Map<String, String>> request, UserContext context, HttpServletRequest servlet) {
        try {
            OpenAccountSnapshot account = openPlatformService.getOrCreateAccount(OpenApiUsers.requireUser(context), "开发者");
            Map<String, String> data = request == null ? null : request.getData();
            String appId = data == null ? null : data.get("appId");
            return Response.ok(openPlatformService.resetAppSecret(account.getUuid(), appId));
        } catch (Exception e) {
            log.error("opl reset secret failed: {}", e.getMessage());
            return Response.error(e);
        }
    }

    @PostMapping("/app/update")
    @Authenticated
    public Response<OpenAppSnapshot> updateApp(@Valid @RequestBody Request<Map<String, String>> request, UserContext context, HttpServletRequest servlet) {
        try {
            OpenAccountSnapshot account = openPlatformService.getOrCreateAccount(OpenApiUsers.requireUser(context), "开发者");
            Map<String, String> data = request == null ? null : request.getData();
            String appId = data == null ? null : data.get("appId");
            OpenAppType appType = null;
            if (data != null && StringUtils.isNotBlank(data.get("appType"))) {
                appType = OpenAppType.parse(data.get("appType"));
            }
            OpenAppSnapshot app = openPlatformService.updateApp(
                    account.getUuid(),
                    appId,
                    data == null ? null : data.get("name"),
                    data == null ? null : data.get("redirectUri"),
                    data == null ? null : data.get("scope"),
                    appType);
            return Response.ok(app);
        } catch (Exception e) {
            log.error("opl update app failed: {}", e.getMessage());
            return Response.error(e);
        }
    }
}
