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

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.HttpURLConnection;
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

    public String write(PluginEntry pluginEntry) {
        try {
            URL url = new URL(pluginEntry.getUrl());
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setConnectTimeout(3000);
            http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)");
            InputStream inputStream = http.getInputStream();
            byte[] buff = new byte[1024 * 10];
            String tmp = System.getProperty("java.io.tmpdir");
            if (tmp.endsWith("/"))
                tmp += "minclouds/plugin";
            else
                tmp += "/minclouds/plugin";
            File file = new File(tmp, pluginEntry.getUuid() + ".jar");
            if (file.exists())
                file.delete();
            if (!file.exists()) {
                if (!file.getParentFile().exists())
                    file.getParentFile().mkdirs();
                OutputStream out = new FileOutputStream(file);
                int len;
                int count = 0; // 计数
                while ((len = inputStream.read(buff)) != -1) {
                    out.write(buff, 0, len);
                    out.flush();
                    ++count;
                }
                // 关闭资源
                out.close();
                inputStream.close();
                http.disconnect();
            }
            return file.getAbsolutePath();
        } catch (Throwable e) {
        }
        return "";
    }

    public PluginEntry load(PluginEntry pluginEntry) throws IOException {
        if (null == pluginEntry || StringUtils.isBlank(pluginEntry.getUrl()) || StringUtils.isBlank(pluginEntry.getUuid())) {
            pluginEntry.setCode(500);
            pluginEntry.setMsg("数据不完整");
            return pluginEntry;
        }
        String file = write(pluginEntry);
        if (StringUtils.isBlank(file)) {
            pluginEntry.setCode(501);
            pluginEntry.setMsg("未下载插件");
            return pluginEntry;
        }
        ClassLoader classLoader = ClassLoaderUtil.getClassLoader("file:" + file);
        if (null == classLoader) {
            pluginEntry.setCode(502);
            pluginEntry.setMsg("类加载错误");
            return pluginEntry;
        }
        if (contain(pluginEntry)) {
            unload(pluginEntry);
        }
        //加入新的jar到系统的ClassPath中
        String classPath = System.getProperty("java.class.path");
        if (!classPath.contains(file))
            System.setProperty("java.class.path", classPath + ":" + file);
        Launcher.getBootstrapClassPath().addURL(new URL("file:" + file));
        List<String> classes = ClassLoaderUtil.getClasses(new File(file));
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
            pluginEntry.setCode(503);
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