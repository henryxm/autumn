package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.plugin.PluginEntry;
import cn.org.autumn.plugin.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("plugin")
public class PluginController {

    @Autowired
    PluginManager pluginManager;

    @RequestMapping(value = "/load", method = RequestMethod.POST)
    public PluginEntry load(@RequestBody PluginEntry pluginEntry) throws IOException {
        return pluginManager.load(pluginEntry).copy();
    }

    @RequestMapping(value = "/unload", method = RequestMethod.POST)
    public PluginEntry unload(@RequestBody PluginEntry pluginEntry) throws IOException {
        return pluginManager.unload(pluginEntry).copy();
    }

    /**
     * 清理所有插件
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/clear", method = RequestMethod.POST)
    public Boolean clear() throws IOException {
        pluginManager.clear();
        return true;
    }

    @RequestMapping(value = "/getPlugins", method = {RequestMethod.POST, RequestMethod.GET})
    public List<PluginEntry> getPlugins() {
        return pluginManager.getPlugins();
    }
}