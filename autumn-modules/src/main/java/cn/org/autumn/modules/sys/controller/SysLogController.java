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

package cn.org.autumn.modules.sys.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import cn.org.autumn.modules.sys.service.SysLogService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
     * @param singlePath  需要单独设置日志输出级别的类的全限定名（例:com.chinasofti.cloudeasy.api.web.LogController）
     * @return
     */
    @ApiOperation(value = "changeLogLevel")
    @GetMapping("changeLevel")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "rootLevel",
                    value = "root,全局级别:ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF", required = false),
            @ApiImplicitParam(name = "singleLevel",
                    value = "单独设置类日志级别:ALL,TRACE,DEBUG,INFO,WARN,ERROR,OFF", required = false),
            @ApiImplicitParam(name = "singlePath",
                    value = "单独类路径:com.chinasofti.cloudeasy.api.web.LogController",
                    required = false)})
    public String changeLevel(String rootLevel, String singleLevel, String singlePath) {
        return sysLogService.changeLevel(rootLevel, singleLevel, singlePath);
    }
}
