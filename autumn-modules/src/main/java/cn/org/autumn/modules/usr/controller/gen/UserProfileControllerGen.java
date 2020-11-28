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
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 用户信息
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class UserProfileControllerGen {

    @Autowired
    protected UserProfileService userProfileService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("usr:userprofile:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = userProfileService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{userId}")
    @RequiresPermissions("usr:userprofile:info")
    public R info(@PathVariable("userId") Long userId) {
        UserProfileEntity userProfile = userProfileService.selectById(userId);
        return R.ok().put("userProfile" , userProfile);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("usr:userprofile:save")
    public R save(@RequestBody UserProfileEntity userProfile) {
        userProfileService.insert(userProfile);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("usr:userprofile:update")
    public R update(@RequestBody UserProfileEntity userProfile) {
        ValidatorUtils.validateEntity(userProfile);
        userProfileService.updateAllColumnById(userProfile);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("usr:userprofile:delete")
    public R delete(@RequestBody Long[] userIds) {
        userProfileService.deleteBatchIds(Arrays.asList(userIds));
        return R.ok();
    }
}
