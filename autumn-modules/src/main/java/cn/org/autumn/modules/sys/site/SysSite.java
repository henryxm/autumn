package cn.org.autumn.modules.sys.site;

import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SysSite {

    public final static String siteId = "202006";
    public final static String pack = "sys";
    public final static String prefix = pack + "_";
    public final static String suffix = "";

    public final static String config = "config";
    public final static String configPageId = "1001";
    public final static String configProductId = "0";
    public final static String configResourceId = "modules/" + pack + "/" + config;
    public final static String configKeyId = prefix + config + suffix;

    public final static String dept = "dept";
    public final static String deptPageId = "1002";
    public final static String deptProductId = "0";
    public final static String deptResourceId = "modules/" + pack + "/" + dept;
    public final static String deptKeyId = prefix + dept + suffix;

    public final static String dict = "dict";
    public final static String dictPageId = "1003";
    public final static String dictProductId = "0";
    public final static String dictResourceId = "modules/" + pack + "/" + dict;
    public final static String dictKeyId = prefix + dict + suffix;

    public final static String log = "log";
    public final static String logPageId = "1004";
    public final static String logProductId = "0";
    public final static String logResourceId = "modules/" + pack + "/" + log;
    public final static String logKeyId = prefix + log + suffix;

    public final static String menu = "menu";
    public final static String menuPageId = "1005";
    public final static String menuProductId = "0";
    public final static String menuResourceId = "modules/" + pack + "/" + menu;
    public final static String menuKeyId = prefix + menu + suffix;

    public final static String role = "role";
    public final static String rolePageId = "1006";
    public final static String roleProductId = "0";
    public final static String roleResourceId = "modules/" + pack + "/" + role;
    public final static String roleKeyId = prefix + role + suffix;

    public final static String user = "user";
    public final static String userPageId = "1006";
    public final static String userProductId = "0";
    public final static String userResourceId = "modules/" + pack + "/" + user;
    public final static String userKeyId = prefix + user + suffix;

    @Autowired
    SuperPositionModelService superPositionModelService;

    @PostConstruct
    public void init() {
        superPositionModelService.put(siteId, configPageId, config, configProductId, configResourceId, configResourceId, configKeyId, true);
        superPositionModelService.put(siteId, deptPageId, dept, deptProductId, deptResourceId, deptResourceId, deptKeyId, true);
        superPositionModelService.put(siteId, dictPageId, dict, dictProductId, dictResourceId, dictResourceId, dictKeyId, true);
        superPositionModelService.put(siteId, logPageId, log, logProductId, logResourceId, logResourceId, logKeyId, true);
        superPositionModelService.put(siteId, menuPageId, menu, menuProductId, menuResourceId, menuResourceId, menuKeyId, true);
        superPositionModelService.put(siteId, rolePageId, role, roleProductId, roleResourceId, roleResourceId, roleKeyId, true);
        superPositionModelService.put(siteId, userPageId, user, userProductId, userResourceId, userResourceId, userKeyId, true);
    }
}
