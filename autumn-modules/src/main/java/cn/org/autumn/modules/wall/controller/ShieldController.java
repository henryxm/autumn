package cn.org.autumn.modules.wall.controller;

import cn.org.autumn.utils.IPUtils;
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

    @RequestMapping(value = "/test", method = RequestMethod.POST)
    public void test(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String ip = IPUtils.getIp(request);
        shieldService.put(ip);
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