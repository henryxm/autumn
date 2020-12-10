package cn.org.autumn.modules.spm.controller;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class SpmController {

    @Autowired
    SuperPositionModelService superPositionModelService;

    @RequestMapping("admin")
    public String admin(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String spm) {
        return superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, spm);
    }

    @RequestMapping("/")
    public String spm(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String spm) {
        return superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, spm);
    }
}
