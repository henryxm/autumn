package cn.org.autumn.modules.install;

import cn.org.autumn.annotation.SkipInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * 安装向导页不依赖业务库与 SPM/多语言等拦截器注入；安装模式下跳过全局 MVC 拦截器，避免 postHandle 查库失败导致 500。
 */
@SkipInterceptor
@Controller
@ConditionalOnProperty(prefix = "autumn.install", name = "mode", havingValue = "true")
public class InstallWizardController {

    @GetMapping({"/install", "/install/"})
    public String wizard(HttpServletRequest request, Model model) {
        String ctx = request.getContextPath() == null ? "" : request.getContextPath();
        model.addAttribute("contextPath", ctx);
        model.addAttribute("installApiBase", ctx + "/install/api");
        model.addAttribute("bootstrapStatusUrl", ctx + "/install/bootstrap-status");
        model.addAttribute("homeUrl", ctx.isEmpty() ? "/" : ctx + "/");
        model.addAttribute("loginUrl", ctx + "/login.html");
        return "install/wizard";
    }
}
