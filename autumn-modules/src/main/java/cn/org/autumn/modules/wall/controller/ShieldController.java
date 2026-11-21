package cn.org.autumn.modules.wall.controller;

import org.springframework.web.bind.annotation.*;
import cn.org.autumn.modules.wall.controller.gen.ShieldControllerGen;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 攻击防御
 *
 * @author User
 * @email henryxm@163.com
 * @date 2024-11
 */
@RestController
@RequestMapping({"wall/shield", "shield"})
public class ShieldController extends ShieldControllerGen {

    /**
     * 防火墙探测：仅返回 403，不再把客户端 IP 写入护盾放行集（防 XFF 伪造加白）。
     */
    @RequestMapping(value = "/test", method = RequestMethod.POST)
    public void test(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(403);
        response.sendRedirect("/");
    }

    @RequestMapping("/print")
    public String print(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return shieldService.print() ? "打印日志" : "关闭日志";
    }

    @RequestMapping("/attack")
    public String attack(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return shieldService.attack() ? "攻击模式" : "和平模式";
    }

    @RequestMapping("/reset")
    public String reset(HttpServletRequest request, HttpServletResponse response) throws IOException {
        shieldService.onOneMinute();
        shieldService.onOneDay();
        return "已重置";
    }
}