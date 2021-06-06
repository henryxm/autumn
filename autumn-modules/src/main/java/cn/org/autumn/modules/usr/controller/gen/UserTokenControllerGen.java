package cn.org.autumn.modules.usr.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import cn.org.autumn.modules.usr.service.UserTokenService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 用户Token
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-06
 */
public class UserTokenControllerGen {

    @Autowired
    protected UserTokenService userTokenService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("usr:usertoken:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = userTokenService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("usr:usertoken:info")
    public R info(@PathVariable("id") Long id) {
        UserTokenEntity userToken = userTokenService.selectById(id);
        return R.ok().put("userToken" , userToken);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("usr:usertoken:save")
    public R save(@RequestBody UserTokenEntity userToken) {
        userTokenService.insert(userToken);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("usr:usertoken:update")
    public R update(@RequestBody UserTokenEntity userToken) {
        ValidatorUtils.validateEntity(userToken);
        userTokenService.updateAllColumnById(userToken);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("usr:usertoken:delete")
    public R delete(@RequestBody Long[] ids) {
        userTokenService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
