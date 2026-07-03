package cn.org.autumn.modules.qrc.site;

import cn.org.autumn.annotation.PageAware;
import org.springframework.stereotype.Component;

/**
 * 模块站点入口：在此添加自定义页面与扩展。表结构对应的页面由 QrcPages 生成维护。
 */
@Component
public class QrcSite extends QrcPages {

    @PageAware(login = false)
    public String authorize = "modules/qrc/pages/authorize";

    @PageAware(login = true)
    public String ticket = "modules/qrc/pages/ticket";

}
