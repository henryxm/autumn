package cn.org.autumn.modules.spm.controller;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.site.PageFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Controller
public class SpmController {

    @Autowired
    SuperPositionModelService superPositionModelService;

    @Autowired
    SysUserRoleService sysUserRoleService;

    @Autowired
    PageFactory pageFactory;

    List<String> active = new ArrayList<>();

    public void active(HttpServletRequest request) {
        String param = request.getParameter("active");
        if (StringUtils.isBlank(param) || !"admin".equals(param))
            return;
        String sessionId = request.getSession().getId();
        active.add(sessionId);
    }

    public boolean isActive(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        return active.contains(sessionId);
    }

    public void inactive(HttpServletRequest request) {
        String param = request.getParameter("inactive");
        if (StringUtils.isNotBlank(param) && "admin".equals(param)) {
            String sessionId = request.getSession().getId();
            active.remove(sessionId);
        }
    }

    @RequestMapping("admin")
    public String admin(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model, String spm) {
        active(httpServletRequest);
        inactive(httpServletRequest);
        if (isActive(httpServletRequest)) {
            SysUserEntity sysUserEntity = ShiroUtils.getUserEntity();
            if (sysUserRoleService.isSystemAdministrator(sysUserEntity))
                return superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, model, spm);
        }
        return pageFactory._404(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("/")
    public String spm(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model, String spm) {
        return superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, model, spm);
    }
}
