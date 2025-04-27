package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.sys.service.SysLogService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping({"/sys/log", "/api"})
public class SysLogController {

    @Autowired
    private SysLogService sysLogService;

    /**
     * 列表
     */
    @ResponseBody
    @RequestMapping("/list")
    @RequiresPermissions("sys:log:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = sysLogService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 修改项目日志输出级别
     *
     * @param rootLevel   全局日志级别
     * @param singleLevel 某个类日志级别
     * @param singlePath  需要单独设置日志输出级别的类的全限定名（例:cn.org.autumn.modules.sys.controller.SysLogController）
     * @return
     */
    @GetMapping("changeLevel")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "rootLevel",
                    value = "root,全局级别:ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF", required = false),
            @ApiImplicitParam(name = "singleLevel",
                    value = "单独设置类日志级别:ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF", required = false),
            @ApiImplicitParam(name = "singlePath",
                    value = "单独类路径:cn.org.autumn.modules.sys.controller.SysLogController",
                    required = false)})
    @ResponseBody
    public String changeLevel(String rootLevel, String singleLevel, String singlePath) {
        return sysLogService.changeLevel(rootLevel, singleLevel, singlePath);
    }

    @RequestMapping("changeLevel/{level}/{clazz}")
    @ResponseBody
    public String debug(@PathVariable String level, @PathVariable String clazz) {
        if (null != clazz && clazz.length() > 0) {
            Object bean = Config.getBean(clazz, true);
            if (null != bean)
                return sysLogService.changeLevel(null, level, bean.getClass().getName());
        }
        return "Fail";
    }
}
