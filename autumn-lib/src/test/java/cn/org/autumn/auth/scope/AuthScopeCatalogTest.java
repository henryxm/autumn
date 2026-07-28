package cn.org.autumn.auth.scope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import cn.org.autumn.auth.scope.AuthField;
import cn.org.autumn.auth.scope.AuthScopeCatalog;
import cn.org.autumn.auth.scope.AuthScopeDef;
import cn.org.autumn.auth.scope.AuthScopeResolution;
import cn.org.autumn.auth.scope.AuthScopeSensitivity;
import cn.org.autumn.auth.scope.AuthScopeSet;
import cn.org.autumn.auth.scope.AuthTrack;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Before;
import org.junit.Test;

public class AuthScopeCatalogTest {

    private AuthScopeCatalog catalog;

    @Before
    public void setUp() {
        catalog = new AuthScopeCatalog();
    }

    @Test
    public void basicExpandsOAuthIdentityAndProfile() {
        AuthScopeSet basic = AuthScopeSet.of("basic").expand(catalog, AuthTrack.OAUTH);
        assertTrue(basic.contains("identity"));
        assertTrue(basic.contains("profile"));
        assertEquals(2, basic.getCodes().size());
    }

    @Test
    public void basicExpandsOplOpenIdUnionProfile() {
        AuthScopeSet basic = AuthScopeSet.of("basic").expand(catalog, AuthTrack.OPL);
        assertTrue(basic.contains("openid"));
        assertTrue(basic.contains("unionid"));
        assertTrue(basic.contains("profile"));
    }

    @Test
    public void resolveBasicWithinClientAllowed() {
        AuthScopeResolution resolution = catalog.resolve(AuthTrack.OAUTH, Arrays.asList("basic"), "basic");
        assertFalse(resolution.hasInvalid());
        assertTrue(resolution.getGranted().contains("identity"));
        assertTrue(resolution.getGranted().contains("profile"));
    }

    @Test
    public void resolveRejectsUnknownScope() {
        AuthScopeResolution resolution = catalog.resolve(AuthTrack.OAUTH, Arrays.asList("basic"), "unknown_scope");
        assertTrue(resolution.hasInvalid());
    }

    @Test
    public void resolveDeniesScopeNotRegisteredOnClient() {
        AuthScopeResolution resolution = catalog.resolve(AuthTrack.OAUTH, Arrays.asList("identity"), "identity profile");
        assertTrue(resolution.getDenied().contains("profile") || resolution.getGranted().getCodes().size() == 1);
        assertTrue(resolution.getGranted().contains("identity"));
        assertFalse(resolution.getGranted().contains("profile"));
    }

    @Test
    public void fieldsForIdentityOnlyUuid() {
        AuthScopeSet scopes = AuthScopeSet.of("identity");
        EnumSet<AuthField> fields = EnumSet.copyOf(catalog.fieldsFor(AuthTrack.OAUTH, scopes));
        assertTrue(fields.contains(AuthField.uuid));
        assertFalse(fields.contains(AuthField.nickname));
    }

    @Test
    public void unionidRequiresOpenIdInExpansion() {
        AuthScopeSet scopes = AuthScopeSet.of("unionid").expand(catalog, AuthTrack.OPL);
        assertTrue(scopes.contains("openid"));
        assertTrue(scopes.contains("unionid"));
    }

    @Test
    public void resolveRejectsRetiredScopeCode() {
        AuthScopeResolution resolution = catalog.resolve(AuthTrack.OPL, Arrays.asList("basic"), "union");
        assertTrue(resolution.hasInvalid());
        assertFalse(catalog.isRegisteredBuiltin("union"));
        assertTrue(catalog.isRegisteredBuiltin("unionid"));
    }

    @Test
    public void boundedDiscardsRetiredScopeCode() {
        AuthScopeResolution resolution = catalog.resolveBounded(AuthTrack.OPL, Arrays.asList("basic"), "openid union profile");
        assertFalse(resolution.hasInvalid());
        assertTrue(resolution.getGranted().contains("openid"));
        assertTrue(resolution.getGranted().contains("profile"));
        assertFalse(resolution.getGranted().contains("union"));
    }

    @Test
    public void oauthProfileHasUsernameFields() {
        AuthScopeDef profile = catalog.getDefinition(AuthTrack.OAUTH, "profile");
        assertTrue(profile.getFields().contains(AuthField.username));
        assertTrue(profile.getFields().contains(AuthField.nickname));
    }

    @Test
    public void oauthEnabledCodesIncludesProfilePhone() {
        assertTrue(catalog.enabledCodes(AuthTrack.OAUTH).contains("profile"));
        assertTrue(catalog.enabledCodes(AuthTrack.OAUTH).contains("phone"));
        assertTrue(catalog.enabledCodes(AuthTrack.OAUTH).contains("email"));
    }

    @Test
    public void oplProfileExcludesUsername() {
        AuthScopeDef profile = catalog.getDefinition(AuthTrack.OPL, "profile");
        assertFalse(profile.getFields().contains(AuthField.username));
        assertTrue(profile.getFields().contains(AuthField.nickname));
    }

    @Test
    public void boundedReturnsIntersectionWithinUpstream() {
        AuthScopeResolution resolution = catalog.resolveBounded(AuthTrack.OAUTH, Arrays.asList("identity"), "identity profile email");
        assertFalse(resolution.hasInvalid());
        assertTrue(resolution.getGranted().contains("identity"));
        assertFalse(resolution.getGranted().contains("profile"));
        assertFalse(resolution.getGranted().contains("email"));
        assertTrue(resolution.hasDenied());
    }

    @Test
    public void boundedStripsUnknownCodesWithoutInvalid() {
        AuthScopeResolution resolution = catalog.resolveBounded(AuthTrack.OAUTH, Arrays.asList("basic"), "basic unknown_scope");
        assertFalse(resolution.hasInvalid());
        assertTrue(resolution.getGranted().contains("identity"));
        assertTrue(resolution.getGranted().contains("profile"));
    }

    @Test
    public void oplProfileLabelMatchesOAuth() {
        AuthScopeDef oauthProfile = catalog.getDefinition(AuthTrack.OAUTH, "profile");
        AuthScopeDef oplProfile = catalog.getDefinition(AuthTrack.OPL, "profile");
        assertEquals("查看基本资料", oauthProfile.getLabel());
        assertEquals(oauthProfile.getLabel(), oplProfile.getLabel());
    }

    @Test
    public void oplHasEmailScope() {
        AuthScopeDef email = catalog.getDefinition(AuthTrack.OPL, "email");
        assertTrue(email != null);
        assertEquals("查看邮箱", email.getLabel());
        assertTrue(email.getFields().contains(AuthField.email));
        assertTrue(catalog.enabledCodes(AuthTrack.OPL).contains("email"));
    }

    @Test
    public void labelsDisplayOrder() {
        AuthScopeSet scopes = AuthScopeSet.of("basic", "phone", "email", "verified", "status");
        java.util.List<String> oauthLabels = catalog.labels(AuthTrack.OAUTH, scopes);
        assertEquals("获取用户唯一标识", oauthLabels.get(0));
        assertEquals("查看基本资料", oauthLabels.get(1));
        assertEquals("查看手机号", oauthLabels.get(2));
        assertEquals("查看邮箱", oauthLabels.get(3));
        java.util.List<String> oplLabels = catalog.labels(AuthTrack.OPL, scopes);
        assertEquals("应用内识别身份", oplLabels.get(0));
        assertEquals("跨应用识别身份", oplLabels.get(1));
        assertEquals("查看基本资料", oplLabels.get(2));
        assertEquals("查看手机号", oplLabels.get(3));
        assertEquals("查看邮箱", oplLabels.get(4));
    }

    @Test
    public void realnameTiersDisabledByDefault() {
        for (String tier : AuthScopeSet.REALNAME_TIERS) {
            AuthScopeDef oauth = catalog.getDefinition(AuthTrack.OAUTH, tier);
            AuthScopeDef opl = catalog.getDefinition(AuthTrack.OPL, tier);
            assertTrue(oauth != null);
            assertTrue(opl != null);
            assertFalse(oauth.isEnabled());
            assertFalse(opl.isEnabled());
            assertFalse(catalog.enabledCodes(AuthTrack.OAUTH).contains(tier));
            assertFalse(catalog.enabledCodes(AuthTrack.OPL).contains(tier));
            assertTrue(catalog.isRegisteredBuiltin(tier));
        }
        assertTrue(catalog.enabledRealNameTierCodes(AuthTrack.OAUTH).isEmpty());
        assertFalse(catalog.isRegisteredBuiltin(AuthScopeSet.REALNAME));
    }

    @Test
    public void realnameAliasExpandsOnlyEnabledTiers() {
        enableRealNameTiers(AuthTrack.OAUTH, AuthScopeSet.REALNAME_ATTR, AuthScopeSet.REALNAME_PERSON);
        AuthScopeSet expanded = AuthScopeSet.of(AuthScopeSet.REALNAME).expand(catalog, AuthTrack.OAUTH);
        assertTrue(expanded.contains(AuthScopeSet.REALNAME_ATTR));
        assertTrue(expanded.contains(AuthScopeSet.REALNAME_PERSON));
        assertFalse(expanded.contains(AuthScopeSet.REALNAME_ID));
        assertFalse(expanded.contains(AuthScopeSet.REALNAME));
    }

    @Test
    public void resolveRejectsDisabledRealnameTiers() {
        AuthScopeResolution resolution = catalog.resolve(AuthTrack.OAUTH,
                Arrays.asList("basic", AuthScopeSet.REALNAME_ATTR), AuthScopeSet.REALNAME_ATTR);
        assertTrue(resolution.hasInvalid());
    }

    @Test
    public void allDoesNotIncludeDisabledRealnameTiers() {
        AuthScopeSet all = AuthScopeSet.of("all").expand(catalog, AuthTrack.OAUTH);
        for (String tier : AuthScopeSet.REALNAME_TIERS) {
            assertFalse(all.contains(tier));
        }
    }

    @Test
    public void realnameTiersHaveNoAuthFields() {
        for (String tier : AuthScopeSet.REALNAME_TIERS) {
            AuthScopeDef def = catalog.getDefinition(AuthTrack.OAUTH, tier);
            assertTrue(def.getFields() == null || def.getFields().isEmpty());
            assertEquals(AuthScopeSensitivity.high, def.getSensitivity());
        }
    }

    @Test
    public void resolveGrantsEnabledRealnameTierWithinClient() {
        enableRealNameTiers(AuthTrack.OAUTH, AuthScopeSet.REALNAME_ATTR);
        AuthScopeResolution resolution = catalog.resolve(AuthTrack.OAUTH,
                Arrays.asList("basic", AuthScopeSet.REALNAME_ATTR), AuthScopeSet.REALNAME_ATTR);
        assertFalse(resolution.hasInvalid());
        assertTrue(resolution.getGranted().contains(AuthScopeSet.REALNAME_ATTR));
        assertFalse(resolution.getGranted().contains(AuthScopeSet.REALNAME_PERSON));
    }

    @Test
    public void labelsIncludeEnabledRealnameTiersInOrder() {
        enableRealNameTiers(AuthTrack.OAUTH, AuthScopeSet.REALNAME_ID, AuthScopeSet.REALNAME_ATTR, AuthScopeSet.REALNAME_PERSON);
        AuthScopeSet scopes = AuthScopeSet.of("basic", AuthScopeSet.REALNAME_ID, AuthScopeSet.REALNAME_ATTR, AuthScopeSet.REALNAME_PERSON);
        java.util.List<String> labels = catalog.labels(AuthTrack.OAUTH, scopes);
        int attr = labels.indexOf("查看实名人口属性");
        int person = labels.indexOf("查看实名姓名与生日");
        int id = labels.indexOf("查看实名证件与地址");
        assertTrue(attr > 0);
        assertTrue(person > attr);
        assertTrue(id > person);
    }

    private void enableRealNameTiers(AuthTrack track, String... tiers) {
        java.util.List<AuthScopeDef> defs = new java.util.ArrayList<AuthScopeDef>();
        for (String tier : tiers) {
            AuthScopeDef def = new AuthScopeDef();
            def.setCode(tier);
            def.setBuiltin(true);
            def.setEnabled(true);
            def.setTracks(java.util.EnumSet.of(track));
            defs.add(def);
        }
        catalog.refreshCustom(defs);
    }
}
