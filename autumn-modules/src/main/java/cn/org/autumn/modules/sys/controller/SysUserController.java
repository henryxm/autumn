package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SysLog;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import cn.org.autumn.validator.Assert;
import cn.org.autumn.validator.ValidatorUtils;
import cn.org.autumn.validator.group.AddGroup;
import cn.org.autumn.validator.group.UpdateGroup;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/user")
public class SysUserController extends AbstractController {
    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    @Lazy
    private SysUserRoleService sysUserRoleService;

    @Autowired
    @Lazy
    private UserProfileService userProfileService;

    /**
     * 所有用户列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("sys:user:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = sysUserService.queryPage(params);
        return R.ok().put("page", page);
    }

    /**
     * 获取登录的用户信息
     */
    @RequestMapping("/info")
    public R info() {
        return R.ok().put("user", getUser());
    }

    /**
     * 修改登录用户密码
     */
    @SysLog("修改密码")
    @RequestMapping("/password")
    public R password(String password, String newPassword) throws Exception {
        Assert.isBlank(newPassword, "新密码不为能空");
        //更新密码
        boolean flag = sysUserService.changePassword(getUserUuid(), password, newPassword);
        if (!flag) {
            return R.error("原密码不正确");
        }
        return R.ok();
    }

    /**
     * 用户信息
     */
    @RequestMapping("/info/{uuid}")
    @RequiresPermissions("sys:user:info")
    public R info(@PathVariable("uuid") String uuid) {
        SysUserEntity user = sysUserService.getByUuid(uuid);
        //获取用户所属的角色列表
        List<String> roleIdList = sysUserRoleService.getRoleKeys(uuid);
        user.setRoleKeys(roleIdList);
        return R.ok().put("user", user);
    }

    /**
     * 保存用户
     */
    @SysLog("保存用户")
    @RequestMapping("/save")
    @RequiresPermissions("sys:user:save")
    public R save(@RequestBody SysUserEntity user) {
        String password = user.getPassword();
        ValidatorUtils.validateEntity(user, AddGroup.class);
        sysUserService.save(user);
        userProfileService.from(user, password, null);
        return R.ok();
    }

    /**
     * 修改用户
     */
    @SysLog("修改用户")
    @RequestMapping("/update")
    @RequiresPermissions("sys:user:update")
    public R update(@RequestBody SysUserEntity user) {
        String password = user.getPassword();
        ValidatorUtils.validateEntity(user, UpdateGroup.class);
        sysUserService.update(user);
        userProfileService.from(user, password, null);
        return R.ok();
    }

    /**
     * 删除用户
     */
    @SysLog("删除用户")
    @RequestMapping("/delete")
    @RequiresPermissions("sys:user:delete")
    public R delete(@RequestBody String[] uuids) {
        if (ArrayUtils.contains(uuids, "admin")) {
            return R.error("系统管理员不能删除");
        }
        if (ArrayUtils.contains(uuids, getUserUuid())) {
            return R.error("当前用户不能删除");
        }
        sysUserService.deleteBatchIds(Arrays.asList(uuids));
        return R.ok();
    }
}
