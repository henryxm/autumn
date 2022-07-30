package cn.org.autumn.plugin;

import cn.org.autumn.loader.ClassLoaderUtil;
import cn.org.autumn.table.utils.HumpConvert;
import cn.org.autumn.utils.SpringContextUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import sun.misc.Launcher;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;

@Component
public class PluginManager {

    Logger log = LoggerFactory.getLogger(getClass());

    Map<PluginEntry, List<String>> plugins = new HashMap<>();

    public Map.Entry<PluginEntry, List<String>> getEntryItem(String uuid) {
        for (Map.Entry<PluginEntry, List<String>> entry : plugins.entrySet()) {
            if (Objects.equals(entry.getKey().getUuid(), uuid))
                return entry;
        }
        return null;
    }

    public boolean contain(PluginEntry entry) {
        return contain(entry.getUuid());
    }

    public boolean contain(String uuid) {
        return null != getEntryItem(uuid);
    }

    public void remove(PluginEntry entry) {
        Iterator<Map.Entry<PluginEntry, List<String>>> iterator = plugins.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PluginEntry, List<String>> item = iterator.next();
            if (Objects.equals(item.getKey().getUuid(), entry.getUuid())) {
                iterator.remove();
                break;
            }
        }
    }

    public Set<PluginEntry> getPlugins() {
        return plugins.keySet();
    }

    public boolean isComponent(Class<?> clazz) {
        Annotation[] annotations = clazz.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            Class ac = annotation.annotationType();
            if (ac.equals(Component.class)
                    || ac.equals(Controller.class)
                    || ac.equals(RestController.class)
                    || ac.equals(Service.class)
                    || ac.equals(Bean.class))
                return true;
        }
        return false;
    }

    public PluginEntry load(PluginEntry pluginEntry) throws IOException {
        if (null == pluginEntry || StringUtils.isBlank(pluginEntry.getUrl()) || StringUtils.isBlank(pluginEntry.getUuid())) {
            pluginEntry.setCode(500);
            pluginEntry.setMsg("数据不完整");
            return pluginEntry;
        }
        ClassLoader classLoader = ClassLoaderUtil.getClassLoader(pluginEntry.getUrl());
        if (null == classLoader) {
            pluginEntry.setCode(500);
            pluginEntry.setMsg("类加载错误");
            return pluginEntry;
        }
        if (contain(pluginEntry)) {
            unload(pluginEntry);
        }
        //加入新的jar到系统的ClassPath中
        String classPath = System.getProperty("java.class.path");
        if (!classPath.contains(pluginEntry.getUrl()))
            System.setProperty("java.class.path", classPath + ":" + pluginEntry.getUrl());
        Launcher.getBootstrapClassPath().addURL(new URL(pluginEntry.getUrl()));
        List<String> classes = ClassLoaderUtil.getClasses(pluginEntry.getUrl());
        List<String> beans = new ArrayList<>();
        Plugin plugin = null;
        for (String cla : classes) {
            try {
                Class<?> clazz = classLoader.loadClass(cla);
                if (!isComponent(clazz))
                    continue;
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
                if (null != bean)
                    beans.add(name);
            } catch (Throwable e) {
                log.error("Class:{}", cla, e);
            }
        }
        if (null == plugin) {
            pluginEntry.setCode(600);
            pluginEntry.setMsg("插件包不合格");
            return pluginEntry;
        }
        pluginEntry.setPlugin(plugin);
        pluginEntry.merge(plugin.entry());
        plugins.put(pluginEntry, beans);
        pluginEntry.setCode(0);
        pluginEntry.setMsg("插件已加载");
        pluginEntry.setData("");
        return pluginEntry;
    }

    public PluginEntry unload(PluginEntry pluginEntry) {
        unload(pluginEntry.getUuid());
        pluginEntry.setCode(0);
        pluginEntry.setMsg("插件已卸载");
        return pluginEntry;
    }

    public void unload(String uuid) {
        Map.Entry<PluginEntry, List<String>> item = getEntryItem(uuid);
        if (null != item) {
            PluginEntry pluginEntry = item.getKey();
            List<String> list = item.getValue();
            if (null != list && !list.isEmpty()) {
                for (String bean : list) {
                    SpringContextUtils.removeBean(bean);
                }
            }
            remove(pluginEntry);
            if (pluginEntry.getPlugin() instanceof Plugin) {
                Plugin plugin = (Plugin) pluginEntry.getPlugin();
                plugin.uninstall();
            }
        }
    }
}