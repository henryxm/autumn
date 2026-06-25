package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import org.apache.shiro.SecurityUtils;

public abstract class SysAbstractController {

    protected SysUserEntity getUser() {
        return (SysUserEntity) SecurityUtils.getSubject().getPrincipal();
    }

    protected String getUserUuid() {
        return getUser().getUuid();
    }

    protected String getDeptKey() {
        return getUser().getDeptKey();
    }
}
