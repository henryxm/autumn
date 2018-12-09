/**
 * Copyright 2018 Autumn.org.cn http://www.autumn.org.cn
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.org.autumn.modules.gen.utils;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.gen.entity.GenTypeWrapper;
import cn.org.autumn.table.data.TableInfo;
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
import java.util.zip.ZipOutputStream;

/**
 * Coding generator tool
 */
public class GenUtils {
	public static List<String> getTemplates(){
		List<String> templates = new ArrayList<String>();
		templates.add("template/Entity.java.vm");
		templates.add("template/Dao.java.vm");
		templates.add("template/ServiceGen.java.vm");
		templates.add("template/Service.java.vm");
		templates.add("template/Controller.java.vm");
		templates.add("template/ControllerGen.java.vm");
		templates.add("template/list.html.vm");
		templates.add("template/list.js.vm");
		return templates;
	}

	public static void generatorCode(TableInfo tableInfo, GenTypeWrapper wrapper, ZipOutputStream zip){
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
		map.put("pathName", tableInfo.getClassname().toLowerCase());
		map.put("columns", tableInfo.getColumns());
		map.put("hasBigDecimal", tableInfo.getHasBigDecimal());
		map.put("mainPath", mainPath);
		map.put("package", wrapper.getModulePackage());
		map.put("moduleName", wrapper.getModuleName());
		map.put("author", wrapper.getAuthorName());
		map.put("email", wrapper.getEmail());
		map.put("moduleId", wrapper.getModuleId());
		map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
		VelocityContext context = new VelocityContext(map);

		//获取模板列表
		List<String> templates = getTemplates();
		for(String template : templates){
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

		if (template.contains("Entity.java.vm" )) {
			return packagePath + "entity" + File.separator + className + "Entity.java";
		}

		if (template.contains("Dao.java.vm" )) {
			return packagePath + "dao" + File.separator + className + "Dao.java";
		}

		if (template.contains("ServiceGen.java.vm" )) {
			return packagePath + "service" + File.separator +"gen"+ File.separator+ className + "ServiceGen.java";
		}

		if (template.contains("Service.java.vm" )) {
			return packagePath + "service" + File.separator + className + "Service.java";
		}

		if (template.contains("Controller.java.vm" )) {
			return packagePath + "controller" + File.separator + className + "Controller.java";
		}

		if (template.contains("ControllerGen.java.vm" )) {
			return packagePath + "controller" + File.separator +"gen"+ File.separator+ className + "ControllerGen.java";
		}

		if (template.contains("list.html.vm" )) {
			return "main" + File.separator + "resources" + File.separator + "templates" + File.separator
					+ "modules" + File.separator + moduleName + File.separator + className.toLowerCase() + ".html";
		}

		if (template.contains("list.js.vm" )) {
			return "main" + File.separator + "resources" + File.separator + "statics" + File.separator + "js" + File.separator
					+ "modules" + File.separator + moduleName + File.separator + className.toLowerCase() + ".js";
		}

		return null;
	}
}
