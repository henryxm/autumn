package cn.org.autumn.modules.ops.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.modules.ops.service.OpsDiscoverSupport;
import cn.org.autumn.site.PageFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 超然信公开落地页：{@code GET /autumn/ops/discover}。
 * App 拦截同 path 后本地 probe，不依赖本页内容。
 * {@code discoverEnabled=false} 时返回 404，对外如同页面不存在。
 * <p>旧路径 {@code /ops/discover} 已废弃，无兼容跳转。
 */
@Controller
@RequestMapping("/autumn/ops")
public class OpsDiscoverController {

    @Autowired
    private OpsDiscoverSupport opsDiscoverSupport;

    @Autowired
    private PageFactory pageFactory;

    @SkipInterceptor
    @GetMapping("/discover")
    public String discover(HttpServletRequest request, HttpServletResponse response, Model model) {
        String view = opsDiscoverSupport.resolvePublicDiscoverView(request, model);
        if (StringUtils.isBlank(view)) {
            return pageFactory._404(request, response, model);
        }
        return view;
    }
}
