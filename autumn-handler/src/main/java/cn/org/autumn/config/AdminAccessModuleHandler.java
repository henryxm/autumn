package cn.org.autumn.config;

import java.util.Collections;
import java.util.Set;

/**
 * 后台门禁扩展：子项目注册「自助 gen 豁免」与「额外管理模块」等。
 * <p>
 * 默认：管理 API / gen CRUD 强制<strong>系统管理员 + 管理 IP 白名单</strong>（{@code wall_ip_admin_white}）；
 * {@code usr}/{@code safe}/{@code bot}/{@code qrc} 及 {@link #selfGenModules()} 豁免（本人数据）；
 * {@link #managedModules()} 追加与框架 {@code GEN_MODULES} 同语义的业务管理模块短名。
 * <p>
 * 非 {@code cn.org.autumn.modules} 前缀、异形包接入步骤见 **{@code docs/AI_ADMIN_IP_BIZ.md}**。
 * 页面侧管理页使用 {@code @PageAware(login = true, admin = true)}。
 */
public interface AdminAccessModuleHandler {

    /**
     * 额外视为「自助」的模块短名（对应包 {@code ...modules.{name}.controller.gen...}），
     * 其 gen CRUD 不强制系统管理员+管理 IP（仍受 {@code @RequiresPermissions} / 业务隔离约束）。
     * <p>
     * <strong>禁止</strong>把全站业务模块塞进本集合。
     */
    default Set<String> selfGenModules() {
        return Collections.emptySet();
    }

    /**
     * 额外视为「全局管理」的模块短名（对应 {@code ...modules.{name}.controller...} 下手写 Controller）。
     * 与框架内置 {@code sys/wall/db/job/gen/oss/lan/spm/node} 同语义：强制超管 + 管理 IP。
     * <p>
     * 用于业务仓非 gen、非 {@code *AdminController} 的管理 REST；详见 {@code docs/AI_ADMIN_IP_BIZ.md} 模式 B。
     */
    default Set<String> managedModules() {
        return Collections.emptySet();
    }
}
