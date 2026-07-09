package cn.org.autumn.modules.client.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.Request;
import cn.org.autumn.modules.client.dto.ScanLoginCredentialResolveRequest;
import cn.org.autumn.modules.client.service.ScanLoginCredentialService;
import cn.org.autumn.utils.R;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("client/scan-login/credential")
@SkipInterceptor
public class ScanLoginCredentialController {

    @Autowired
    private ScanLoginCredentialService scanLoginCredentialService;

    @PostMapping("/resolve")
    public R resolve(@RequestBody(required = false) Request<ScanLoginCredentialResolveRequest> request, HttpServletRequest servlet) {
        try {
            ScanLoginCredentialResolveRequest data = request == null ? null : request.getData();
            if (data == null || StringUtils.isBlank(data.getType()) || StringUtils.isBlank(data.getId())) {
                return R.error("type 与 id 不能为空");
            }
            return R.ok().put("data", scanLoginCredentialService.toView(scanLoginCredentialService.require(data.getType(), data.getId())));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
}
