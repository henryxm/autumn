package cn.org.autumn.modules.spm.controller;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.site.PageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class SpmController {

    @Autowired
    SuperPositionModelService superPositionModelService;

    @Autowired
    SysUserRoleService sysUserRoleService;

    @Autowired
    PageFactory pageFactory;

    @RequestMapping("admin")
    public String admin(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model, String spm) {
        SysUserEntity sysUserEntity = ShiroUtils.getUserEntity();
        if (sysUserRoleService.isSystemAdministrator(sysUserEntity))
            return superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, model, spm);
        return pageFactory._404(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("/")
    public String spm(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model, String spm) {
        return superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, model, spm);
    }
}
