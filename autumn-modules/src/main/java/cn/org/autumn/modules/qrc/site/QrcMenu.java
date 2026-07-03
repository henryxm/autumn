package cn.org.autumn.modules.qrc.site;

import cn.org.autumn.menu.Menu;
import cn.org.autumn.base.ModuleMenu;
import org.springframework.stereotype.Service;

@Service
@Menu(name = "扫码登录", order = 448000, ico = "fa-qrcode")
public class QrcMenu extends ModuleMenu {

}