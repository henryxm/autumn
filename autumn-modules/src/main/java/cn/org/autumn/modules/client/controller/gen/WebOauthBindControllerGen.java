package cn.org.autumn.modules.client.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.client.entity.WebOauthBindEntity;
import cn.org.autumn.modules.client.service.WebOauthBindService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class WebOauthBindControllerGen {

    @Autowired
    protected WebOauthBindService webOauthBindService;

    @RequestMapping("/list")
    @RequiresPermissions("client:weboauthbind:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = webOauthBindService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("client:weboauthbind:info")
    public R info(@PathVariable("id") Long id) {
        WebOauthBindEntity webOauthBind = webOauthBindService.selectById(id);
        return R.ok().put("webOauthBind" , webOauthBind);
    }

    @RequestMapping("/save")
    @RequiresPermissions("client:weboauthbind:save")
    public R save(@RequestBody WebOauthBindEntity webOauthBind) {
        webOauthBindService.insert(webOauthBind);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("client:weboauthbind:update")
    public R update(@RequestBody WebOauthBindEntity webOauthBind) {
        ValidatorUtils.validateEntity(webOauthBind);
        webOauthBindService.updateAllColumnById(webOauthBind);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("client:weboauthbind:delete")
    public R delete(@RequestBody Long[] ids) {
        webOauthBindService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
