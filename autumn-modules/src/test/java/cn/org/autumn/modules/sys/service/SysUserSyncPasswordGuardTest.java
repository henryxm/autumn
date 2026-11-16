package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 文档化 sync 防回写约定：Session 主体口令不得覆盖库中权威哈希。
 */
public class SysUserSyncPasswordGuardTest {

    @Test
    void sessionPasswordMustNotReplaceStoreWhenUpdating() {
        SysUserEntity fromSession = new SysUserEntity();
        fromSession.setUuid("u1");
        fromSession.setUsername("alice");
        fromSession.setPassword("old-sha-hex");
        fromSession.setSalt("oldsaltoldsaltoldsalt");

        SysUserEntity fromStore = new SysUserEntity();
        fromStore.setUuid("u1");
        fromStore.setUsername("alice");
        fromStore.setPassword("pbkdf2$120000$abc$hash");
        fromStore.setSalt("newsaltnewsaltnewsalt");
        fromStore.setDeptKey("dept-a");
        fromStore.setUserId(9L);

        // 与 SysUserService.sync 写库前逻辑一致
        fromSession.setDeptKey(fromStore.getDeptKey());
        fromSession.setUserId(fromStore.getUserId());
        fromSession.setPassword(fromStore.getPassword());
        fromSession.setSalt(fromStore.getSalt());

        assertEquals("pbkdf2$120000$abc$hash", fromSession.getPassword());
        assertEquals("newsaltnewsaltnewsalt", fromSession.getSalt());
        assertNotEquals("old-sha-hex", fromSession.getPassword());
    }
}
