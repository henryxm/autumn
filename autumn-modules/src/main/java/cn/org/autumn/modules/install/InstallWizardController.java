package cn.org.autumn.modules.install;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

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
