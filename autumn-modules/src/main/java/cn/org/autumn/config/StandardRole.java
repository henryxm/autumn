package cn.org.autumn.config;

import cn.org.autumn.modules.sys.service.SysUserRoleService;
import org.apache.commons.lang3.StringUtils;

/**
 * 无 Minclouds 时的默认 {@link Role}：仅基于 {@link SysUserRoleService}，不访问菜单扩展。
 */
public class StandardRole implements Role, VariablesHandler {

    private final SysUserRoleService sysUserRoleService;

    public StandardRole(SysUserRoleService sysUserRoleService) {
        this.sysUserRoleService = sysUserRoleService;
    }

    @Override
    public String getName() {
        return "role";
    }

    @Override
    public boolean isSystemAdmin(String userUuid) {
        if (StringUtils.isBlank(userUuid)) {
            return false;
        }
        return sysUserRoleService.isAdmin(userUuid);
    }

    @Override
    public boolean isSystemAdmin() {
        return sysUserRoleService.isAdmin();
    }

    @Override
    public boolean isUserAdmin(String userUuid) {
        return false;
    }

    @Override
    public boolean isUserAdmin() {
        return false;
    }

    @Override
    public boolean isAgentAdmin(String userUuid) {
        return false;
    }

    @Override
    public boolean isAgentAdmin() {
        return false;
    }

    @Override
    public boolean hasRole(String roleId) {
        return false;
    }
}
