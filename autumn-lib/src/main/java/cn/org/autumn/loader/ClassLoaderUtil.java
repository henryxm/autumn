package cn.org.autumn.loader;

import cn.org.autumn.utils.SpringContextUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class ClassLoaderUtil {

    static final Logger log = LoggerFactory.getLogger(ClassLoaderUtil.class);

    private static String getClassName(JarEntry entry) {
        String name = entry.getName();
        // 这个获取的就是一个实体类class java.util.jar.JarFile$JarFileEntry
        // Class<? extends JarEntry> class1 = nextElement.getClass();
        //System.out.println("entry name=" + name);
        // 加载某个class文件，并实现动态运行某个class
        if (name.endsWith(".class")) {
            String replace = name.replace(".class", "").replace("/", ".");
            return replace;
        }
        return null;
    }

    public static void pringManifestFile(Manifest manifest) {
        Attributes mainAttributes = manifest.getMainAttributes();
        Set<Map.Entry<Object, Object>> entrySet = mainAttributes.entrySet();
        Iterator<Map.Entry<Object, Object>> iterator = entrySet.iterator();
        // 打印并显示当前的MAINFEST.MF文件中的信息
        while (iterator.hasNext()) {
            Map.Entry<Object, Object> next = iterator.next();
            Object key = next.getKey();
            Object value = next.getValue();
            // 这里可以获取到Class-Path,或者某个执行的Main-Class
            System.out.println(key + ": " + value);
        }
    }

    public static List<String> getClasses(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        JarInputStream jis = new JarInputStream(inputStream);
        //Manifest manifest = jis.getManifest();
        //pringManifestFile(manifest);
        JarEntry nextJarEntry = jis.getNextJarEntry();
        List<String> classes = new ArrayList<>();
        while (nextJarEntry != null) {
            String className = getClassName(nextJarEntry);
            if (StringUtils.isNotBlank(className))
                classes.add(className);
            nextJarEntry = jis.getNextJarEntry();
        }
        return classes;
    }

    public static List<String> getClasses(String url) throws IOException {
        URL url1 = new URL(url);
        JarInputStream jis = new JarInputStream(url1.openStream());
        //Manifest manifest = jis.getManifest();
        //pringManifestFile(manifest);
        JarEntry nextJarEntry = jis.getNextJarEntry();
        List<String> classes = new ArrayList<>();
        while (nextJarEntry != null) {
            String className = getClassName(nextJarEntry);
            if (StringUtils.isNotBlank(className))
                classes.add(className);
            nextJarEntry = jis.getNextJarEntry();
        }
        return classes;
    }

    public static ClassLoader getClassLoader(String url) {
        try {
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            URLClassLoader classLoader = new URLClassLoader(new URL[]{}, SpringContextUtils.getApplicationContext().getClassLoader());
            method.invoke(classLoader, new URL(url));
            return classLoader;
        } catch (Exception e) {
            log.error("getClassLoader-error", e);
            return null;
        }
    }
}