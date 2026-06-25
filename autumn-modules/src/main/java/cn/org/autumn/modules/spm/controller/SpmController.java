package cn.org.autumn.modules.spm.controller;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.site.MappingFactory;
import cn.org.autumn.site.PageFactory;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@Slf4j
public class SpmController {


    @Autowired
    SuperPositionModelService superPositionModelService;

    @Autowired
    SysUserRoleService sysUserRoleService;

    @Autowired
    PageFactory pageFactory;

    @Autowired
    MappingFactory mappingFactory;

    @Autowired
    LoadFactory loadFactory;

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
            if (sysUserRoleService.isSystemAdministrator(sysUserEntity)) {
                String resourceId = superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, model, spm);
                if (log.isDebugEnabled())
                    log.debug("Admin resourceId:{}", resourceId);
                return resourceId;
            }
        }
        if (log.isDebugEnabled())
            log.debug("Unauthorized access");
        return pageFactory._404(httpServletRequest, httpServletResponse, model);
    }

    @RequestMapping("/")
    public String spm(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model, String spm) {
        String resourceId = superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, model, spm);
        if (log.isDebugEnabled())
            log.debug("ResourceId:{}", resourceId);
        return resourceId;
    }

    @RequestMapping(value = "/{value}", method = RequestMethod.GET)
    public String mapping(HttpServletRequest request, HttpServletResponse response, Model model, @PathVariable("value") String value) {
        if (!loadFactory.isDone()) {
            if (log.isDebugEnabled())
                log.debug("Starting:{}", request.getRequestURL());
            return pageFactory.loading(request, response, model);
        }
        String resourceId = mappingFactory.mapping(request, response, model, value);
        if (log.isDebugEnabled())
            log.debug("Path:{}, resourceId:{}", value, resourceId);
        return resourceId;
    }
}
