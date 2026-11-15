package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.plugin.PluginEntry;
import cn.org.autumn.plugin.PluginManager;
import cn.org.autumn.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("plugin")
public class SysPluginController {

    @Autowired
    PluginManager pluginManager;

    @Autowired
    SysUserRoleService sysUserRoleService;

    private boolean deny() {
        return !ShiroUtils.isLogin() || !sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
    }

    @RequestMapping(value = "/load", method = RequestMethod.POST)
    public Object load(@RequestBody PluginEntry pluginEntry) throws IOException {
        if (deny()) {
            return R.error(403, "无权限");
        }
        return pluginManager.load(pluginEntry).copy();
    }

    @RequestMapping(value = "/unload", method = RequestMethod.POST)
    public Object unload(@RequestBody PluginEntry pluginEntry) throws IOException {
        if (deny()) {
            return R.error(403, "无权限");
        }
        return pluginManager.unload(pluginEntry).copy();
    }

    /**
     * 清理所有插件
     */
    @RequestMapping(value = "/clear", method = RequestMethod.POST)
    public Object clear() throws IOException {
        if (deny()) {
            return R.error(403, "无权限");
        }
        pluginManager.clear();
        return true;
    }

    @RequestMapping(value = "/getPlugins", method = {RequestMethod.POST, RequestMethod.GET})
    public Object getPlugins() {
        if (deny()) {
            return R.error(403, "无权限");
        }
        return pluginManager.getPlugins();
    }
}
