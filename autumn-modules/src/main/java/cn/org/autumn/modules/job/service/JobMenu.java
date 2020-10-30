package cn.org.autumn.modules.job.service;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.table.TableInit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 定时任务日志
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

@Service
public class JobMenu {

    public static final String job_menu = "job_menu";

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected TableInit tableInit;

    @Autowired
    protected LanguageService languageService;

    public void init() {
    }
}
