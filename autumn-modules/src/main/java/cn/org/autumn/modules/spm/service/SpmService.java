package cn.org.autumn.modules.spm.service;

import cn.org.autumn.config.VariablesHandler;
import cn.org.autumn.modules.spm.entity.Spm;
import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class SpmService implements VariablesHandler {
    @Autowired
    SuperPositionModelService superPositionModelService;

    @Override
    public String getName() {
        return "spm";
    }

    public boolean isSpmMode() {
        return superPositionModelService.isSpmMode();
    }

    public String getResourceId(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Model model) {
        if (null == httpServletRequest)
            return "";
        String spm = httpServletRequest.getParameter("spm");
        if (StringUtils.isBlank(spm))
            return "";
        return superPositionModelService.getResourceId(httpServletRequest, httpServletResponse, model, spm);
    }

    public Spm getSpm(HttpServletRequest httpServletRequest) {
        String spm = httpServletRequest.getParameter("spm");
        if (StringUtils.isBlank(spm))
            return null;
        SuperPositionModelEntity entity = superPositionModelService.getSpm(httpServletRequest, spm);
        if (null != entity)
            entity.parse(spm);
        return entity;
    }

    public Spm getSpm(String spm) {
        if (StringUtils.isBlank(spm))
            return null;
        SuperPositionModelEntity entity = superPositionModelService.getSpm(null, spm);
        if (null != entity)
            entity.parse(spm);
        return entity;
    }

    public String getUrl(String key) {
        return superPositionModelService.getUrl(key);
    }
}