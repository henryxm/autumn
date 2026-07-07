package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.Request;
import cn.org.autumn.modules.client.dto.RpQrcCompleteRequest;
import cn.org.autumn.modules.client.dto.RpQrcCreateRequest;
import cn.org.autumn.modules.client.service.OauthRpQrcService;
import cn.org.autumn.modules.spm.interceptor.SpmInterceptor;
import cn.org.autumn.modules.usr.interceptor.AuthorizationInterceptor;
import cn.org.autumn.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("client/oauth2/qrc/web")
public class ClientOauth2QrcController {

    @Autowired
    OauthRpQrcService oauthRpQrcService;

    @PostMapping("/ticket/create")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R create(@Valid @RequestBody(required = false) Request<RpQrcCreateRequest> request, HttpServletRequest servlet) {
        try {
            RpQrcCreateRequest data = request == null ? null : request.getData();
            String callback = data == null ? null : data.getCallback();
            return R.ok().put("data", oauthRpQrcService.createTicket(servlet, callback));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/ticket/status")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R status(@RequestParam("uuid") String uuid, HttpServletRequest servlet) {
        try {
            if (StringUtils.isBlank(uuid)) {
                return R.error("uuid 不能为空");
            }
            return R.ok().put("data", oauthRpQrcService.pollStatus(servlet, uuid));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/ticket/complete")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R complete(@Valid @RequestBody(required = false) Request<RpQrcCompleteRequest> request, HttpServletRequest servlet) {
        try {
            RpQrcCompleteRequest data = request == null ? null : request.getData();
            if (data == null || StringUtils.isBlank(data.getUuid())) {
                return R.error("uuid 不能为空");
            }
            String redirectUrl = oauthRpQrcService.completeTicket(servlet, data.getUuid(), data.getCallback());
            return R.ok().put("data", redirectUrl);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/ticket/cancel")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R cancel(@RequestParam("uuid") String uuid, HttpServletRequest servlet) {
        try {
            if (StringUtils.isBlank(uuid)) {
                return R.error("uuid 不能为空");
            }
            oauthRpQrcService.cancelTicket(servlet, uuid);
            return R.ok().put("data", uuid);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
}
