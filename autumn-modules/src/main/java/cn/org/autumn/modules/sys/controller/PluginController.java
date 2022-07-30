package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.plugin.PluginEntry;
import cn.org.autumn.plugin.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("plugin")
public class PluginController {

    @Autowired
    PluginManager pluginManager;

    @RequestMapping(value = "/load", method = RequestMethod.POST)
    public String load(@RequestBody PluginEntry pluginEntry) throws IOException {
        return pluginManager.load(pluginEntry);
    }

    @RequestMapping(value = "/unload", method = RequestMethod.POST)
    public String unload(@RequestBody PluginEntry pluginEntry) throws IOException {
        pluginManager.unload(pluginEntry);
        return "success";
    }

    @RequestMapping(value = "/getPlugins", method = {RequestMethod.POST, RequestMethod.GET})
    public Set<PluginEntry> getPlugins() {
        return pluginManager.getPlugins();
    }
}