package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.loader.ClassLoaderUtil;
import cn.org.autumn.plugin.Plugin;
import cn.org.autumn.plugin.PluginEntry;
import cn.org.autumn.table.utils.HumpConvert;
import cn.org.autumn.utils.SpringContextUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import sun.misc.Launcher;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@RestController
@RequestMapping("plugin")
public class PluginController {

    @RequestMapping(value = "/load", method = RequestMethod.POST)
    public String load(@RequestBody PluginEntry pluginEntry) throws MalformedURLException {
        if (null == pluginEntry)
            return "Empty Plugin";
        ClassLoader classLoader = ClassLoaderUtil.getClassLoader(pluginEntry.getUrl());
        if (null == classLoader) {
            return "Class Load Error";
        }
        //加入新的jar到系统的ClassPath中
        //String classPath = System.getProperty("java.class.path");
        //System.setProperty("java.class.path", classPath + ":"+);
        Launcher.getBootstrapClassPath().addURL(new URL(pluginEntry.getUrl()));
        List<String> classes = pluginEntry.getClasses();
        Plugin plugin = null;
        for (String cla : classes) {
            try {
                Class<?> clazz = classLoader.loadClass(cla);
                String name = clazz.getSimpleName();
                name = HumpConvert.toFirstStringLower(name);
                Object bean = SpringContextUtils.getBean(name);
                if (null == bean) {
                    SpringContextUtils.registerBean(name, clazz);
                    bean = SpringContextUtils.getBean(name);
                }
                if (bean instanceof Plugin) {
                    plugin = (Plugin) bean;
                    plugin.install();
                }
            } catch (Exception ignored) {
            }
        }
        if (null == plugin)
            return "Plugin Not Found";
        return plugin.version();
    }
}