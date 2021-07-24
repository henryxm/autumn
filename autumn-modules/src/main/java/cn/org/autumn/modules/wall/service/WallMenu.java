package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.wall.service.gen.WallMenuGen;
import org.springframework.stereotype.Service;

/**
 * 链接黑名单
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
@Service
public class WallMenu extends WallMenuGen {

    @Override
    protected String order() {
        return "777777";
    }

    @Override
    public String ico() {
        return "fa-firefox";
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
        super.init();
    }
}
