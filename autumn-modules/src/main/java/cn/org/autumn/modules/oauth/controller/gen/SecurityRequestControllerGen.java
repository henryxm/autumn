package cn.org.autumn.modules.oauth.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.oauth.entity.SecurityRequestEntity;
import cn.org.autumn.modules.oauth.service.SecurityRequestService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 安全验证
 *
 * @author User
 * @email henryxm@163.com
 * @date 2026-03
 */
public class SecurityRequestControllerGen {

    @Autowired
    protected SecurityRequestService securityRequestService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("oauth:securityrequest:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = securityRequestService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("oauth:securityrequest:info")
    public R info(@PathVariable("id") Long id) {
        SecurityRequestEntity securityRequest = securityRequestService.selectById(id);
        return R.ok().put("securityRequest" , securityRequest);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("oauth:securityrequest:save")
    public R save(@RequestBody SecurityRequestEntity securityRequest) {
        securityRequestService.insert(securityRequest);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("oauth:securityrequest:update")
    public R update(@RequestBody SecurityRequestEntity securityRequest) {
        ValidatorUtils.validateEntity(securityRequest);
        securityRequestService.updateAllColumnById(securityRequest);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("oauth:securityrequest:delete")
    public R delete(@RequestBody Long[] ids) {
        securityRequestService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
