package cn.org.autumn.modules.auth.service;

import cn.org.autumn.auth.model.AuthRealNameInfo;
import cn.org.autumn.auth.scope.AuthScopeSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthRealNameServiceProjectTest {

    private final AuthRealNameService service = new AuthRealNameService();

    private static AuthRealNameInfo full() {
        AuthRealNameInfo info = new AuthRealNameInfo();
        info.setName("张三");
        info.setAge(20);
        info.setGender("男");
        info.setEthnicity("汉");
        info.setBirthday("20000101");
        info.setIdNumber("110101199001011237");
        info.setAddress("北京市");
        return info;
    }

    @Test
    void project_attrOnly_omitsNameAndId() {
        AuthRealNameInfo out = service.project(full(), AuthScopeSet.of(AuthScopeSet.REALNAME_ATTR));
        assertNotNull(out);
        assertEquals(20, out.getAge());
        assertEquals("男", out.getGender());
        assertEquals("汉", out.getEthnicity());
        assertNull(out.getName());
        assertNull(out.getBirthday());
        assertNull(out.getIdNumber());
        assertNull(out.getAddress());
    }

    @Test
    void project_allTiers_keepsAll() {
        AuthRealNameInfo out = service.project(full(), AuthScopeSet.of(
                AuthScopeSet.REALNAME_ATTR, AuthScopeSet.REALNAME_PERSON, AuthScopeSet.REALNAME_ID));
        assertEquals("张三", out.getName());
        assertEquals(20, out.getAge());
        assertEquals("110101199001011237", out.getIdNumber());
        assertEquals("北京市", out.getAddress());
    }

    @Test
    void project_noTier_returnsNull() {
        assertNull(service.project(full(), AuthScopeSet.of("basic")));
    }
}
