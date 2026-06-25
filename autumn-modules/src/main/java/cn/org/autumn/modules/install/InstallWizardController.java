package cn.org.autumn.modules.install;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.spm.interceptor.SpmInterceptor;
import cn.org.autumn.modules.usr.interceptor.AuthorizationInterceptor;
import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 安装向导：跳过授权与 SPM，避免依赖业务登录态/埋点库；<b>不跳过</b> {@link cn.org.autumn.modules.lan.interceptor.LanguageInterceptor}，
 * 以便与业务项目下沉模板时保持「必有 {@code lang}」契约。
 * <p>
 * <b>多语言（非二选一）</b>：安装前业务库可能未就绪，{@code lang} 由拦截器分层装入——classpath 种子
 * （{@link cn.org.autumn.modules.install.InstallWizardLangSupport}）+ 可选内存中的 {@link cn.org.autumn.modules.lan.service.LanguageService} 词条；
 * 正常运行期仍以 Load 链后的 DB 缓存为主。详见 {@code install/lang/*.properties}。
 */
@SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
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
