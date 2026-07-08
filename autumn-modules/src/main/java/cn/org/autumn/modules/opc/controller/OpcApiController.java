package cn.org.autumn.modules.opc.controller;

import cn.org.autumn.annotation.Authenticated;
import cn.org.autumn.model.Request;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.support.OpenApiUsers;
import cn.org.autumn.modules.opc.dto.OpcConnectApplyRequest;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.opc.model.ConnectAppSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户自助 API：管理接入应用配置。 */
@Slf4j
@RestController
@RequestMapping(OpcConstants.API_PLATFORM)
public class OpcApiController {

    @Autowired
    private ConnectAppService connectAppService;

    @PostMapping("/app/apply")
    @Authenticated
    public Response<ConnectAppSnapshot> apply(@Valid @RequestBody Request<OpcConnectApplyRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            OpcConnectApplyRequest data = request == null ? null : request.getData();
            ConnectAppEntity app = connectAppService.applyToPlatform(
                    OpenApiUsers.requireUser(context),
                    data == null ? null : data.getPlatformBaseUrl(),
                    data == null ? null : data.getName(),
                    data == null ? null : data.getRedirectUri(),
                    data == null ? null : data.getScope(),
                    data == null ? null : data.getAccessToken());
            return Response.ok(connectAppService.toPublicSnapshot(app));
        } catch (Exception e) {
            log.error("opc apply app failed: {}", e.getMessage());
            return Response.error(e);
        }
    }

    @PostMapping("/app/save")
    @Authenticated
    public Response<ConnectAppSnapshot> save(@Valid @RequestBody Request<Map<String, String>> request, UserContext context, HttpServletRequest servlet) {
        try {
            Map<String, String> data = request == null ? null : request.getData();
            ConnectAppEntity app = connectAppService.saveConfig(
                    OpenApiUsers.requireUser(context),
                    data == null ? null : data.get("appId"),
                    data == null ? null : data.get("appSecret"),
                    data == null ? null : data.get("platformBaseUrl"),
                    data == null ? null : data.get("redirectUri"),
                    data == null ? null : data.get("name"),
                    data == null ? null : data.get("scope"));
            return Response.ok(connectAppService.toPublicSnapshot(app));
        } catch (Exception e) {
            log.error("opc save app failed: {}", e.getMessage());
            return Response.error(e);
        }
    }

    @PostMapping("/" + OpcConstants.NS + "/app/list")
    @Authenticated
    public Response<List<ConnectAppSnapshot>> list(@Valid @RequestBody(required = false) Request<?> request, UserContext context, HttpServletRequest servlet) {
        try {
            return Response.ok(connectAppService.listSnapshotsByUser(OpenApiUsers.requireUser(context)));
        } catch (Exception e) {
            log.error("opc list app failed: {}", e.getMessage());
            return Response.error(e);
        }
    }

    @PostMapping("/login/url")
    @Authenticated
    public Response<Map<String, String>> loginUrl(@Valid @RequestBody Request<Map<String, String>> request, UserContext context, HttpServletRequest servlet) {
        try {
            Map<String, String> data = request == null ? null : request.getData();
            String appId = data == null ? null : data.get("appId");
            ConnectAppEntity app = connectAppService.getByAppId(appId);
            if (app == null) {
                throw new IllegalArgumentException("appId不存在");
            }
            if (!StringUtils.equals(app.getUser(), OpenApiUsers.requireUser(context))) {
                throw new IllegalArgumentException("无权访问该接入应用");
            }
            Map<String, String> result = new HashMap<>();
            result.put("authorizeUrl", connectAppService.buildAuthorizeEntryUrl(app.getAppId()));
            return Response.ok(result);
        } catch (Exception e) {
            log.error("opc login url failed: {}", e.getMessage());
            return Response.error(e);
        }
    }
}
