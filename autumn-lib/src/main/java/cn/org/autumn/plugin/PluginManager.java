package cn.org.autumn.plugin;

import cn.org.autumn.loader.ClassLoaderUtil;
import cn.org.autumn.table.utils.HumpConvert;
import cn.org.autumn.utils.SpringContextUtils;
import org.apache.commons.lang.StringUtils;
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

    public Set<PluginEntry> getPlugins(){
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

    public String load(PluginEntry pluginEntry) throws IOException {
        if (null == pluginEntry || StringUtils.isBlank(pluginEntry.getUrl()) || StringUtils.isBlank(pluginEntry.getUuid()))
            return "Empty Plugin";
        ClassLoader classLoader = ClassLoaderUtil.getClassLoader(pluginEntry.getUrl());
        if (null == classLoader) {
            return "Class Load Error";
        }
        if (contain(pluginEntry)) {
            unload(pluginEntry);
        }
        //加入新的jar到系统的ClassPath中
        //String classPath = System.getProperty("java.class.path");
        //System.setProperty("java.class.path", classPath + ":"+);
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
            } catch (Exception ignored) {
            }
        }
        if (null == plugin)
            return "Plugin Not Found";
        plugins.put(pluginEntry, beans);
        return plugin.version();
    }

    public void unload(PluginEntry pluginEntry) {
        Map.Entry<PluginEntry, List<String>> item = getEntryItem(pluginEntry.getUuid());
        if (null != item) {
            List<String> list = item.getValue();
            if (null != list && !list.isEmpty()) {
                for (String bean : list) {
                    SpringContextUtils.removeBean(bean);
                }
            }
        }
        remove(pluginEntry);
    }
}