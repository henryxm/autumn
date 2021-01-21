package cn.org.autumn.modules.job.service;

import cn.org.autumn.modules.job.service.gen.JobMenuGen;
import org.springframework.stereotype.Service;

/**
 * 定时任务
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class JobMenu extends JobMenuGen {

    @Override
    protected String order() {
        return super.order();
    }

    @Override
    public String ico() {
        return super.ico();
    }

    @Override
    public String getMenu() {
        return super.getMenu();
    }

    @Override
    public String getParentMenu() {
        return super.getParentMenu();
    }

    @Override
    public void init() {
    }

    public String[][] getLanguageItems() {
        return null;
    }
}
