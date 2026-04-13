package cn.org.autumn.modules.install;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
@ConditionalOnProperty(prefix = "autumn.install", name = "mode", havingValue = "true")
public class InstallWizardController {

    @GetMapping({"/install", "/install/"})
    public String wizard(HttpServletRequest request, Model model) {
        String ctx = request.getContextPath() == null ? "" : request.getContextPath();
        model.addAttribute("installApiBase", ctx + "/install/api");
        return "install/wizard";
    }
}
