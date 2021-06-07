package cn.org.autumn.modules.gen.utils;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.gen.entity.GenTypeWrapper;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * Coding generator tool
 */
public class GenUtils {
    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<String>();
        templates.add("template/Entity.java.vm");
        templates.add("template/Dao.java.vm");
        templates.add("template/ServiceGen.java.vm");
        templates.add("template/Service.java.vm");
        templates.add("template/Menu.java.vm");
        templates.add("template/MenuGen.java.vm");
        templates.add("template/Controller.java.vm");
        templates.add("template/ControllerGen.java.vm");
        templates.add("template/list.html.vm");
        templates.add("template/list.js.vm");
        templates.add("template/Site.java.vm");
        return templates;
    }

    public static void generatorCode(TableInfo tableInfo, GenTypeWrapper wrapper, ZipOutputStream zip) {
        Properties prop = new Properties();
        prop.setProperty("resource.loader", "class");
        prop.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);

        String mainPath = wrapper.getEntity().getRootPackage();
        mainPath = StringUtils.isBlank(mainPath) ? "cn.org.autumn" : mainPath;

        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableInfo.getName());
        map.put("comment", tableInfo.getComment());
        map.put("pk", tableInfo.getPk());
        map.put("className", tableInfo.getClassName());
        map.put("classname", tableInfo.getClassname());
        map.put("enLang", tableInfo.getEnLang());
        map.put("pathName", tableInfo.getClassname().toLowerCase());
        map.put("columns", tableInfo.getColumns());
        map.put("index", tableInfo.buildIndexKey());
        map.put("hasBigDecimal", tableInfo.getHasBigDecimal());
        map.put("mainPath", mainPath);
        map.put("package", wrapper.getModulePackage());
        map.put("moduleName", wrapper.getModuleName());
        map.put("moduleText", wrapper.getModuleText());
        map.put("upperModuleName", HumpConvert.toFirstStringUpper(wrapper.getModuleName()));
        map.put("author", wrapper.getAuthorName());
        map.put("email", wrapper.getEmail());
        map.put("moduleId", wrapper.getModuleId());
        map.put("lang", "lang.");
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);

            try {
                //添加到zip
                zip.putNextEntry(new ZipEntry(getFileName(template, tableInfo.getClassName(), wrapper.getModulePackage(), wrapper.getModuleName())));
                IOUtils.write(sw.toString(), zip, "UTF-8");
                IOUtils.closeQuietly(sw);
                zip.closeEntry();
            } catch (ZipException e) {
                if (null != e.getMessage() && e.getMessage().contains("duplicate entry"))
                    continue;
            } catch (IOException e) {
                throw new AException("渲染模板失败，表名：" + tableInfo.getName(), e);
            }
        }
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, String className, String packageName, String moduleName) {
        String packagePath = "main" + File.separator + "java" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File.separator;
        }

        if (template.contains("Entity.java.vm")) {
            return packagePath + "entity" + File.separator + className + "Entity.java";
        }

        if (template.contains("Dao.java.vm")) {
            return packagePath + "dao" + File.separator + className + "Dao.java";
        }

        if (template.contains("ServiceGen.java.vm")) {
            return packagePath + "service" + File.separator + "gen" + File.separator + className + "ServiceGen.java";
        }

        if (template.contains("Service.java.vm")) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if (template.contains("Menu.java.vm")) {
            return packagePath + "service" + File.separator + HumpConvert.toFirstStringUpper(moduleName) + "Menu.java";
        }

        if (template.contains("MenuGen.java.vm")) {
            return packagePath + "service" + File.separator + "gen" + File.separator + HumpConvert.toFirstStringUpper(moduleName) + "MenuGen.java";
        }

        if (template.contains("Site.java.vm")) {
            return packagePath + "site" + File.separator + HumpConvert.toFirstStringUpper(moduleName) + "Site.java";
        }

        if (template.contains("Controller.java.vm")) {
            return packagePath + "controller" + File.separator + className + "Controller.java";
        }

        if (template.contains("ControllerGen.java.vm")) {
            return packagePath + "controller" + File.separator + "gen" + File.separator + className + "ControllerGen.java";
        }

        if (template.contains("list.html.vm")) {
            return "main" + File.separator + "resources" + File.separator + "templates" + File.separator
                    + "modules" + File.separator + moduleName + File.separator + className.toLowerCase() + ".html";
        }

        if (template.contains("list.js.vm")) {
            return "main" + File.separator + "resources" + File.separator + "templates" + File.separator
                    + "modules" + File.separator + moduleName + File.separator + className.toLowerCase() + ".js.html";
        }
        return null;
    }
}
