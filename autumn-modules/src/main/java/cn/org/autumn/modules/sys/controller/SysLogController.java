package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.config.Config;
import cn.org.autumn.modules.sys.service.SysLogService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
     * 获取所有日志器及其级别
     */
    @ResponseBody
    @GetMapping("/listLoggers")
    @RequiresPermissions("sys:log:list")
    public R listLoggers() {
        List<Map<String, String>> loggers = sysLogService.listLoggers();
        return R.ok().put("loggers", loggers);
    }

    /**
     * 修改项目日志输出级别
     *
     * @param level      日志级别:ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF
     * @param loggerName 需要单独设置日志输出级别的类的全限定名或包名（例:cn.org.autumn.modules.sys.controller.SysLogController）
     * @return
     */
    @GetMapping("changeLevel")
    @Parameters({
            @Parameter(name = "level", description = "日志级别:ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF", required = true),
            @Parameter(name = "loggerName", description = "类或包路径:cn.org.autumn.modules.sys.controller.SysLogController", required = false)})
    @ResponseBody
    @RequiresPermissions("sys:log:list")
    public String changeLevel(String level, String loggerName) {
        return sysLogService.changeLevel(level, loggerName);
    }

    @RequestMapping("changeLevel/{level}/{clazz}")
    @ResponseBody
    @RequiresPermissions("sys:log:list")
    public String debug(@PathVariable String level, @PathVariable String clazz) {
        if (null != clazz && clazz.length() > 0) {
            Object bean = Config.getBean(clazz);
            if (null == bean)
                bean = Config.getBean(clazz, true);
            Class<?> c = null;
            if (null == bean) {
                try {
                    c = Class.forName(clazz);
                } catch (ClassNotFoundException ignored) {
                }
            } else {
                c = bean.getClass();
            }
            if (null != c) {
                String name = c.getName();
                //去除被代理的类型
                if (name.contains("$"))
                    name = name.split("\\$")[0];
                return sysLogService.changeLevel(level, name);
            }
        }
        return "Fail";
    }
}
