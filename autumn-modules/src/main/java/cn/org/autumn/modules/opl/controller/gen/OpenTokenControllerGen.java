package cn.org.autumn.modules.opl.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.opl.entity.OpenTokenEntity;
import cn.org.autumn.modules.opl.service.OpenTokenService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class OpenTokenControllerGen {

    @Autowired
    protected OpenTokenService openTokenService;

    @RequestMapping("/list")
    @RequiresPermissions("opl:opentoken:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = openTokenService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("opl:opentoken:info")
    public R info(@PathVariable("id") Long id) {
        OpenTokenEntity openToken = openTokenService.selectById(id);
        return R.ok().put("openToken" , openToken);
    }

    @RequestMapping("/save")
    @RequiresPermissions("opl:opentoken:save")
    public R save(@RequestBody OpenTokenEntity openToken) {
        openTokenService.insert(openToken);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("opl:opentoken:update")
    public R update(@RequestBody OpenTokenEntity openToken) {
        ValidatorUtils.validateEntity(openToken);
        openTokenService.updateAllColumnById(openToken);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("opl:opentoken:delete")
    public R delete(@RequestBody Long[] ids) {
        openTokenService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
