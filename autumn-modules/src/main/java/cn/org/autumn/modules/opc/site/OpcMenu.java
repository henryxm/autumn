package cn.org.autumn.modules.opc.site;

import cn.org.autumn.base.ModuleMenu;
import cn.org.autumn.menu.Menu;
import org.springframework.stereotype.Service;

@Service
@Menu(name = "开放接入", order = 666661, ico = "fa-plug")
public class OpcMenu extends ModuleMenu {

    public String[][] getLanguageItems() {
        return new String[][]{
                {getLanguageKey(), "开放接入", "Open Connect"},
                {"opc_opcmanage", "统一管理", "OPC Manage"},
                {"opc_connectbind", "接入绑定管理", "Connect Bind Manage"},
        };
    }
}
