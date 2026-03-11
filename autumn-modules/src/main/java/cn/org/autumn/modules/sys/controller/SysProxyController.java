package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.DisableXssFilter;
import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.service.BaseHttpProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 通用透明反向代理控制器
 * <p>
 * 完全通用的透明反向代理，可以代理所有 HTTP/HTTPS 请求
 * 类似 nginx 反向代理，客户端传递目标 URL 和认证信息，服务端负责转发
 * 支持：
 * - 所有 HTTP 方法（GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS 等）
 * - 流式和非流式响应
 * - 完整的请求头转发
 * - 二进制数据（图片、文件等）
 * - 表单数据
 * - JSON/XML 数据
 * - CORS 跨域支持
 *
 * @author Autumn
 * @date 2026-03
 */
@Slf4j
@RestController
@SkipInterceptor
@DisableXssFilter
@RequestMapping(BaseHttpProxyService.proxy)
public class SysProxyController {

    @Autowired
    BaseHttpProxyService baseHttpProxyService;

    /**
     * 代理所有请求 - 主入口
     * <p>
     * 完全透明的代理所有 HTTP/HTTPS 请求
     */
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS, RequestMethod.TRACE})
    public Object proxyAllRequests(@RequestParam(value = "target", required = false) String target, HttpServletRequest request, HttpServletResponse response) {
        return baseHttpProxyService.proxyAllRequests(target, request, response);
    }

    /**
     * OPTIONS 预检请求处理 - 完全透明模式
     * <p>
     * 对于 OPTIONS 请求，先转发到目标服务器获取响应，
     * 然后完全透明地返回给客户端
     */
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public Object handleOptions(@RequestParam(value = "target", required = false) String target, HttpServletRequest request, HttpServletResponse response) {
        return baseHttpProxyService.handleOptions(target, request, response);
    }
}