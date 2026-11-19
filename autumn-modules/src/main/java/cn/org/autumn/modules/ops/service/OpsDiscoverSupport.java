package cn.org.autumn.modules.ops.service;

import cn.org.autumn.site.SitePortalSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * 公开发现页视图解析。
 * <ul>
 *   <li>匿名 {@code /ops/discover}：受 {@code discoverEnabled} 控制；关闭 → 404</li>
 *   <li>后台 {@code main}：管理员已登录，始终展示完整落地页；并注入公网开关真值供模板提示</li>
 * </ul>
 */
@Component
public class OpsDiscoverSupport {

    public static final String VIEW_OPEN = "ops/discover";

    @Autowired
    private OpsCertService opsCertService;

    @Autowired
    private SitePortalSupport sitePortalSupport;

    /**
     * 匿名公开入口：仅在开启时填充 model 并返回落地页视图；关闭时返回 {@code null}（调用方应 404）。
     */
    public String resolvePublicDiscoverView(HttpServletRequest request, Model model) {
        if (!opsCertService.isDiscoverEnabled()) {
            return null;
        }
        sitePortalSupport.applyDiscoverPublicModel(request, model);
        if (model != null) {
            // 公网路径：发现页当前即为开启态
            model.addAttribute("publicDiscoverEnabled", true);
            model.addAttribute("adminDiscoverPreview", false);
        }
        return VIEW_OPEN;
    }

    /**
     * 后台 {@code main.html}：已登录管理员首页，始终完整落地页（不受开关控制版面）。
     * 注入 {@code publicDiscoverEnabled} 真值，便于提示运维区链接在公网可能 404。
     */
    public String resolveAdminDiscoverView(HttpServletRequest request, Model model) {
        sitePortalSupport.applyDiscoverPublicModel(request, model);
        if (model != null) {
            model.addAttribute("publicDiscoverEnabled", opsCertService.isDiscoverEnabled());
            model.addAttribute("adminDiscoverPreview", true);
        }
        return VIEW_OPEN;
    }
}
