package cn.org.autumn.modules.gen.utils;

import cn.org.autumn.modules.gen.entity.GenTypeWrapper;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.utils.HumpConvert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Coding generator tool
 */
public class GenUtils {

    private static final Logger log = LoggerFactory.getLogger(GenUtils.class);

    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<>();
        templates.add("template/Entity.java.vm");
        templates.add("template/Dao.java.vm");
        templates.add("template/Service.java.vm");
        templates.add("template/Controller.java.vm");
        templates.add("template/ControllerGen.java.vm");
        templates.add("template/list.html.vm");
        templates.add("template/list.js.vm");
        return templates;
    }

    public static List<String> getSiteTemplates() {
        List<String> templates = new ArrayList<String>();
        templates.add("template/Site.java.vm");
        templates.add("template/Menu.java.vm");
        return templates;
    }

    public static void generatorCode(TableInfo tableInfo, GenTypeWrapper wrapper, ZipOutputStream zip, List<String> templates, List<Map<String, Object>> tables) {
        String mainPath = wrapper.getEntity().getRootPackage();
        mainPath = StringUtils.isBlank(mainPath) ? "cn.org.autumn" : mainPath;
        Map<String, Object> map = new HashMap<>();
        if (null != tableInfo) {
            map.put("tableName", tableInfo.getName());
            map.put("comment", tableInfo.getComment());
            map.put("pk", tableInfo.getPk());
            map.put("module", tableInfo.getModule());
            map.put("prefix", tableInfo.getPrefix());
            map.put("className", tableInfo.getClassName());
            map.put("classname", tableInfo.getClassname());
            map.put("filename", tableInfo.getFilename());
            map.put("enLang", tableInfo.getEnLang());
            map.put("pathName", tableInfo.getClassname().toLowerCase());
            map.put("columns", tableInfo.getColumns());
            map.put("index", tableInfo.buildIndexKey());
            map.put("hasBigDecimal", tableInfo.getHasBigDecimal());
        }
        map.put("mainPath", mainPath);
        map.put("package", wrapper.getModulePackage());
        map.put("moduleName", wrapper.getModuleName());
        map.put("moduleText", wrapper.getModuleText());
        map.put("moduleIcon", wrapper.getModuleIcon());
        map.put("moduleOrder", wrapper.getModuleOrder());
        map.put("upperModuleName", HumpConvert.toFirstStringUpper(wrapper.getModuleName()));
        map.put("author", wrapper.getAuthorName());
        map.put("email", wrapper.getEmail());
        map.put("moduleId", wrapper.getModuleId());
        map.put("lang", "lang.");
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));

        if (null != tables) {
            if (null != tableInfo)
                tables.add(map);
            map.put("tables", tables);
        }

        VelocityContext context = new VelocityContext(map);

        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);

            try {
                //添加到zip
                String className = "";
                if (null != tableInfo)
                    className = tableInfo.getClassName();
                String fileName = getFileName(template, className, wrapper.getModulePackage(), wrapper.getModuleName());
                if (null != fileName) {
                    zip.putNextEntry(new ZipEntry(fileName));
                    IOUtils.write(sw.toString(), zip, "UTF-8");
                    IOUtils.closeQuietly(sw);
                    zip.closeEntry();
                }
            } catch (Exception e) {
                log.error("generatorCode:", e);
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

        if (template.contains("Service.java.vm")) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if (template.contains("Menu.java.vm")) {
            return packagePath + "site" + File.separator + HumpConvert.toFirstStringUpper(moduleName) + "Menu.java";
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
                    + "modules" + File.separator + moduleName + File.separator + className.toLowerCase() + ".js";
        }
        return null;
    }
}
