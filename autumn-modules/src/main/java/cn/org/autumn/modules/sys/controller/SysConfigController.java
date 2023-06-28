package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SysLog;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.spm.entity.Spm;
import cn.org.autumn.modules.spm.service.SpmService;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.entity.SystemUpgrade;
import cn.org.autumn.modules.sys.service.SysCategoryService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import cn.org.autumn.validator.ValidatorUtils;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static cn.org.autumn.modules.sys.service.SysConfigService.*;

@RestController
@RequestMapping("/sys/config")
public class SysConfigController extends AbstractController {
    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    @Autowired
    SysCategoryService sysCategoryService;

    @Autowired
    SysUserRoleService sysUserRoleService;

    @Autowired
    Language language;

    @Autowired
    SpmService spmService;

    /**
     * 所有配置列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("sys:config:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = sysConfigService.queryPage(params);

        return R.ok().put("page", page);
    }

    @RequestMapping("/basic")
    @RequiresPermissions("sys:config:list")
    public R basic(@RequestParam Map<String, Object> params) {
        PageUtils page = sysConfigService.queryPage(params, 2);
        return R.ok().put("page", page);
    }

    /**
     * 配置信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("sys:config:info")
    public R info(@PathVariable("id") Long id) {
        SysConfigEntity config = sysConfigService.selectById(id);

        return R.ok().put("config", config);
    }

    /**
     * 保存配置
     */
    @SysLog("保存配置")
    @RequestMapping("/save")
    @RequiresPermissions("sys:config:save")
    public R save(@RequestBody SysConfigEntity config) {
        ValidatorUtils.validateEntity(config);

        sysConfigService.save(config);

        return R.ok();
    }

    /**
     * 修改配置
     */
    @SysLog("修改配置")
    @RequestMapping("/update")
    @RequiresPermissions("sys:config:update")
    public R update(@RequestBody SysConfigEntity config) {
        if (null == config)
            return R.error();
        if (null == config.getId()) {
            SysConfigEntity entity = sysConfigService.getByKey(config.getParamKey());
            if (null == entity)
                return R.error();
            String value = config.getParamValue();
            if (entity.getType().equals(json_type) && StringUtils.isNotBlank(entity.getOptions())) {
                try {
                    Class<?> clazz = Class.forName(entity.getOptions());
                    Gson gson = new Gson();
                    Object o = gson.fromJson(entity.getParamValue(), clazz);
                    List<String> list = Arrays.asList(config.getFieldName().split("\\."));
                    if (!list.isEmpty()) {
                        sysConfigService.inject(list.listIterator(), clazz, null, o, null, null, value);
                        String json = gson.toJson(o);
                        entity.setParamValue(json);
                    }
                } catch (ClassNotFoundException | IllegalAccessException e) {
                }
            } else {
                entity.setParamValue(value);
            }
            config = entity;
        } else
            ValidatorUtils.validateEntity(config);

        sysConfigService.update(config);

        return R.ok();
    }

    /**
     * 删除配置
     */
    @SysLog("删除配置")
    @RequestMapping("/delete")
    @RequiresPermissions("sys:config:delete")
    public R delete(@RequestBody Long[] ids) {
        sysConfigService.deleteBatch(ids);

        return R.ok();
    }

    @RequestMapping(value = "data", method = RequestMethod.POST)
    @RequiresPermissions("sys:config:list")
    public R basic(String spm, HttpServletRequest request) {
        if (!ShiroUtils.isLogin())
            return R.error("Not Login!");
        if (!sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserEntity()))
            return R.error("权限不足");
        String lang = language.toLang(Language.getLocale(request));
        Spm spm1 = spmService.getSpm(spm);
        String key = "";
        if (null != spm1) {
            String productId = spm1.getProductId();
            if (StringUtils.isNotBlank(productId) && !productId.equals("0")) {
                key = spm1.getProductId();
            }
        }
        Map map = sysCategoryService.getCategories(lang, key);
        R r = R.ok();
        r.put("data", map);
        return r;
    }

    @RequestMapping(value = "getSystemUpgrade")
    public R getSystemUpgrade() {
        R r = R.ok();
        r.put("data", sysConfigService.getSystemUpgrade());
        return r;
    }
}
