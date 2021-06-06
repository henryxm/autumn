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
import cn.org.autumn.modules.usr.entity.UserLoginLogEntity;
import cn.org.autumn.modules.usr.service.UserLoginLogService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 登录日志
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-06
 */
public class UserLoginLogControllerGen {

    @Autowired
    protected UserLoginLogService userLoginLogService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("usr:userloginlog:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = userLoginLogService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("usr:userloginlog:info")
    public R info(@PathVariable("id") Long id) {
        UserLoginLogEntity userLoginLog = userLoginLogService.selectById(id);
        return R.ok().put("userLoginLog" , userLoginLog);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("usr:userloginlog:save")
    public R save(@RequestBody UserLoginLogEntity userLoginLog) {
        userLoginLogService.insert(userLoginLog);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("usr:userloginlog:update")
    public R update(@RequestBody UserLoginLogEntity userLoginLog) {
        ValidatorUtils.validateEntity(userLoginLog);
        userLoginLogService.updateAllColumnById(userLoginLog);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("usr:userloginlog:delete")
    public R delete(@RequestBody Long[] ids) {
        userLoginLogService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
