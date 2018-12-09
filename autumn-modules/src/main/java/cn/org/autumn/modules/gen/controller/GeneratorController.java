package cn.org.autumn.modules.gen.controller;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.gen.service.GeneratorService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.utils.R;
import net.bytebuddy.implementation.bytecode.Throw;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 代码生成器
 */
@Controller
@RequestMapping("/gen/generator")
public class GeneratorController {
	@Autowired
	private GeneratorService sysGeneratorService;
	
	/**
	 * 列表
	 */
	@ResponseBody
	@RequestMapping("/list")
	@RequiresPermissions("gen:generator:list")
	public R list(@RequestParam Map<String, Object> params){
		PageUtils pageUtil = sysGeneratorService.queryPage(params);

		return R.ok().put("page", pageUtil);
	}
	
	/**
	 * 生成代码
	 */
	@RequestMapping("/code")
	@RequiresPermissions("gen:generator:code")
	public void code(String tables, String genId, HttpServletResponse response) throws IOException{
		if(StringUtils.isEmpty(genId)){
			throw new AException("未选择生成方案");
		}
		byte[] data = sysGeneratorService.generatorCode(tables.split(","), genId);
		
		response.reset();  
        response.setHeader("Content-Disposition", "attachment; filename=\"autumn.zip\"");
        response.addHeader("Content-Length", "" + data.length);  
        response.setContentType("application/octet-stream; charset=UTF-8");  
  
        IOUtils.write(data, response.getOutputStream());  
	}
}
