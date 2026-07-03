package cn.org.autumn.modules.client.controller;

import cn.org.autumn.modules.client.dto.AuthClientBundle;
import cn.org.autumn.modules.client.service.AuthClientHubService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("client/authhub")
public class AuthClientHubController {

    @Autowired
    private AuthClientHubService authClientHubService;

    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = authClientHubService.queryPage(params);
        return R.ok().put("page", page);
    }

    @RequestMapping("/info/{clientId}")
    public R info(@PathVariable("clientId") String clientId) {
        AuthClientBundle bundle = authClientHubService.getBundle(clientId);
        if (bundle == null || bundle.getOauth() == null) {
            return R.error("客户端不存在");
        }
        return R.ok().put("bundle", bundle);
    }

    @RequestMapping("/tickets/{clientId}")
    public R tickets(@PathVariable("clientId") String clientId, @RequestParam Map<String, Object> params) {
        PageUtils page = authClientHubService.queryTickets(clientId, params);
        return R.ok().put("page", page);
    }

    @RequestMapping("/create")
    public R create(@RequestBody Map<String, String> body) {
        String clientId = body == null ? null : body.get("clientId");
        if (StringUtils.isBlank(clientId)) {
            return R.error("clientId不能为空");
        }
        try {
            AuthClientBundle bundle = authClientHubService.createClient(clientId.trim());
            return R.ok().put("bundle", bundle);
        } catch (IllegalStateException e) {
            return R.error(e.getMessage());
        }
    }

    @RequestMapping("/save")
    public R save(@RequestBody AuthClientBundle bundle) {
        if (bundle == null || StringUtils.isBlank(bundle.getClientId())) {
            return R.error("clientId不能为空");
        }
        try {
            authClientHubService.saveBundle(bundle);
            return R.ok().put("bundle", authClientHubService.getBundle(bundle.getClientId()));
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        }
    }

    @RequestMapping("/delete")
    public R delete(@RequestBody Map<String, String> body) {
        String clientId = body == null ? null : body.get("clientId");
        if (StringUtils.isBlank(clientId)) {
            return R.error("clientId不能为空");
        }
        authClientHubService.deleteByClientId(clientId.trim());
        return R.ok();
    }
}
