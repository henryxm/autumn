package cn.org.autumn.modules.safe.site;

import cn.org.autumn.menu.Menu;
import cn.org.autumn.base.ModuleMenu;
import org.springframework.stereotype.Service;

@Service
@Menu(name = "支付安全", order = 446000, ico = "fa-shield")
public class SafeMenu extends ModuleMenu {

}