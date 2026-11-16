package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.plugin.PluginEntry;
import cn.org.autumn.plugin.PluginManager;
import cn.org.autumn.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("plugin")
public class SysPluginController {

    @Autowired
    PluginManager pluginManager;

    @Autowired
    AdminAccessGuard adminAccessGuard;

    private R denyIfUnauthorized() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return R.error(403, "无权限");
            }
            HttpServletRequest request = attrs.getRequest();
            adminAccessGuard.requireOpsAdmin(request, getClass(), "plugin");
            return null;
        } catch (AException e) {
            return R.error(403, "无权限");
        }
    }

    @RequestMapping(value = "/load", method = RequestMethod.POST)
    public Object load(@RequestBody PluginEntry pluginEntry) throws IOException {
        R denied = denyIfUnauthorized();
        if (denied != null) {
            return denied;
        }
        return pluginManager.load(pluginEntry).copy();
    }

    @RequestMapping(value = "/unload", method = RequestMethod.POST)
    public Object unload(@RequestBody PluginEntry pluginEntry) throws IOException {
        R denied = denyIfUnauthorized();
        if (denied != null) {
            return denied;
        }
        return pluginManager.unload(pluginEntry).copy();
    }

    /**
     * 清理所有插件
     */
    @RequestMapping(value = "/clear", method = RequestMethod.POST)
    public Object clear() throws IOException {
        R denied = denyIfUnauthorized();
        if (denied != null) {
            return denied;
        }
        pluginManager.clear();
        return true;
    }

    @RequestMapping(value = "/getPlugins", method = {RequestMethod.POST, RequestMethod.GET})
    public Object getPlugins() {
        R denied = denyIfUnauthorized();
        if (denied != null) {
            return denied;
        }
        return pluginManager.getPlugins();
    }
}
