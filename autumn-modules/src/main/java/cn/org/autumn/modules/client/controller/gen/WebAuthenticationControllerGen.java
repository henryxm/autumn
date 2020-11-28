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
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 网站客户端
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class WebAuthenticationControllerGen {

    @Autowired
    protected WebAuthenticationService webAuthenticationService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("client:webauthentication:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = webAuthenticationService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("client:webauthentication:info")
    public R info(@PathVariable("id") Long id) {
        WebAuthenticationEntity webAuthentication = webAuthenticationService.selectById(id);
        return R.ok().put("webAuthentication" , webAuthentication);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("client:webauthentication:save")
    public R save(@RequestBody WebAuthenticationEntity webAuthentication) {
        webAuthenticationService.insert(webAuthentication);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("client:webauthentication:update")
    public R update(@RequestBody WebAuthenticationEntity webAuthentication) {
        ValidatorUtils.validateEntity(webAuthentication);
        webAuthenticationService.updateAllColumnById(webAuthentication);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("client:webauthentication:delete")
    public R delete(@RequestBody Long[] ids) {
        webAuthenticationService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
