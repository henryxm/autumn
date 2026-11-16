package cn.org.autumn.config;

import java.util.Collections;
import java.util.Set;

/**
 * 后台门禁扩展：子项目注册「自助 gen 豁免模块」等。
 * <p>
 * 默认：任意包下 {@code *.controller.gen.*} 的生成 CRUD 仅系统管理员可访问；
 * {@code usr}/{@code safe}/{@code bot}/{@code qrc} 及本接口返回的模块名豁免（供普通用户本人数据 API）。
 * 页面侧请对管理页使用 {@code @PageAware(login = true, admin = true)}。
 */
public interface AdminAccessModuleHandler {

    /**
     * 额外视为「自助」的模块短名（对应包 {@code ...modules.{name}.controller.gen...}），
     * 其 gen CRUD 不强制系统管理员（仍受 {@code @RequiresPermissions} / 业务隔离约束）。
     */
    default Set<String> selfGenModules() {
        return Collections.emptySet();
    }
}
