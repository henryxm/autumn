package cn.org.autumn.auth.scope;

import cn.org.autumn.opl.OplConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 授权 scope 注册表：内置定义 + DB 自定义合并；解析、校验与字段映射。
 * 同 code 在 OAuth / OPL 轨分别存储，避免互相覆盖。
 */
@Component
public class AuthScopeCatalog {

    private final EnumMap<AuthTrack, Map<String, AuthScopeDef>> trackDefinitions = new EnumMap<AuthTrack, Map<String, AuthScopeDef>>(AuthTrack.class);

    public AuthScopeCatalog() {
        for (AuthTrack track : AuthTrack.values()) {
            trackDefinitions.put(track, new LinkedHashMap<String, AuthScopeDef>());
        }
        registerBuiltins();
    }

    public synchronized void refreshCustom(List<AuthScopeDef> customDefs) {
        registerBuiltins();
        if (customDefs == null) {
            return;
        }
        for (AuthScopeDef def : customDefs) {
            if (def == null || StringUtils.isBlank(def.getCode())) {
                continue;
            }
            String code = AuthScopeSet.normalize(def.getCode());
            def.setCode(code);
            if (def.isAlias()) {
                continue;
            }
            if (def.isBuiltin()) {
                updateBuiltinState(def);
                continue;
            }
            registerCustom(def);
        }
    }

    public List<AuthScopeDef> listDefinitions(AuthTrack track) {
        List<AuthScopeDef> list = new ArrayList<AuthScopeDef>();
        if (track == null) {
            return list;
        }
        Map<String, AuthScopeDef> bucket = trackDefinitions.get(track);
        if (bucket == null) {
            return list;
        }
        for (AuthScopeDef def : bucket.values()) {
            if (def != null) {
                list.add(def);
            }
        }
        return list;
    }

    public List<String> labels(AuthTrack track, AuthScopeSet scopes) {
        List<String> labels = new ArrayList<String>();
        if (scopes == null || scopes.isEmpty() || track == null) {
            return labels;
        }
        AuthScopeSet expanded = scopes.expand(this, track);
        for (String code : orderCodesForDisplay(track, expanded.getCodes())) {
            AuthScopeDef def = getDefinition(track, code);
            if (def != null && StringUtils.isNotBlank(def.getLabel())) {
                labels.add(def.getLabel());
            } else {
                labels.add(code);
            }
        }
        return labels;
    }

    private List<String> orderCodesForDisplay(AuthTrack track, Set<String> codes) {
        List<String> ordered = new ArrayList<String>();
        if (codes == null || codes.isEmpty()) {
            return ordered;
        }
        if (track == AuthTrack.OAUTH) {
            addIfPresent(ordered, codes, "identity");
        } else if (track == AuthTrack.OPL) {
            addIfPresent(ordered, codes, "openid");
            addIfPresent(ordered, codes, "unionid");
        }
        for (String code : new String[]{"profile", "phone", "email", "verified", "status"}) {
            addIfPresent(ordered, codes, code);
        }
        for (String code : new TreeSet<String>(codes)) {
            if (!ordered.contains(code)) {
                ordered.add(code);
            }
        }
        return ordered;
    }

    private void addIfPresent(List<String> ordered, Set<String> codes, String code) {
        if (codes.contains(code)) {
            ordered.add(code);
        }
    }

    public AuthScopeResolution resolve(AuthTrack track, List<String> clientScopes, String requestedScope) {
        AuthScopeSet clientAllowed = resolveClientAllowed(track, clientScopes);
        AuthScopeSet rawRequested = AuthScopeSet.withDefault(requestedScope);
        List<String> invalid = new ArrayList<String>();
        List<String> denied = new ArrayList<String>();
        for (String code : rawRequested.getCodes()) {
            if (AuthScopeSet.BASIC.equals(code) || AuthScopeSet.ALL.equals(code)) {
                continue;
            }
            if (!isValidCode(track, code)) {
                invalid.add(code);
            }
        }
        if (!invalid.isEmpty()) {
            return new AuthScopeResolution(track, rawRequested, AuthScopeSet.empty(), invalid, denied);
        }
        AuthScopeSet expanded = rawRequested.expand(this, track);
        for (String code : expanded.getCodes()) {
            if (!isValidCode(track, code)) {
                denied.add(code);
            }
        }
        AuthScopeSet granted = expanded.intersect(clientAllowed);
        for (String code : expanded.getCodes()) {
            if (!granted.contains(code)) {
                denied.add(code);
            }
        }
        if (granted.isEmpty() && !expanded.isEmpty()) {
            return new AuthScopeResolution(track, rawRequested, AuthScopeSet.empty(), invalid, denied);
        }
        return new AuthScopeResolution(track, rawRequested, granted, invalid, denied);
    }

    /**
     * 宽松解析：下游请求与上游登记取交集，结果严格落在上游允许范围内。
     * 未知/未启用的 code 静默剔除；交集为空时回退为 basic 与上游的交集；不填充 invalid。
     */
    public AuthScopeResolution resolveBounded(AuthTrack track, List<String> upstreamScopes, String downstreamScope) {
        if (track == null) {
            return new AuthScopeResolution(null, AuthScopeSet.empty(), AuthScopeSet.empty(), Collections.<String>emptyList(), Collections.<String>emptyList());
        }
        AuthScopeSet upstreamAllowed = resolveClientAllowed(track, upstreamScopes);
        AuthScopeSet rawRequested = AuthScopeSet.withDefault(downstreamScope);
        AuthScopeSet expanded = rawRequested.expand(this, track);
        List<String> denied = new ArrayList<String>();
        Set<String> validRequested = new LinkedHashSet<String>();
        for (String code : expanded.getCodes()) {
            if (isValidCode(track, code)) {
                validRequested.add(code);
            } else {
                denied.add(code);
            }
        }
        AuthScopeSet filtered = AuthScopeSet.fromCollection(validRequested);
        AuthScopeSet granted = filtered.intersect(upstreamAllowed);
        for (String code : filtered.getCodes()) {
            if (!granted.contains(code)) {
                denied.add(code);
            }
        }
        if (granted.isEmpty()) {
            granted = AuthScopeSet.basicFor(track).expand(this, track).intersect(upstreamAllowed);
        }
        if (granted.isEmpty() && !upstreamAllowed.isEmpty()) {
            granted = upstreamAllowed;
        }
        return new AuthScopeResolution(track, rawRequested, granted, Collections.<String>emptyList(), denied);
    }

    public Set<String> enabledCodes(AuthTrack track) {
        Set<String> codes = new LinkedHashSet<String>();
        if (track == null) {
            return codes;
        }
        Map<String, AuthScopeDef> bucket = trackDefinitions.get(track);
        if (bucket == null) {
            return codes;
        }
        for (AuthScopeDef def : bucket.values()) {
            if (def != null && def.isEnabled()) {
                codes.add(def.getCode());
            }
        }
        return codes;
    }

    public Set<AuthField> fieldsFor(AuthTrack track, AuthScopeSet scopes) {
        EnumSet<AuthField> fields = EnumSet.noneOf(AuthField.class);
        if (scopes == null || scopes.isEmpty() || track == null) {
            return fields;
        }
        AuthScopeSet expanded = scopes.expand(this, track);
        for (String code : expanded.getCodes()) {
            AuthScopeDef def = getDefinition(track, code);
            if (def != null && def.getFields() != null) {
                fields.addAll(def.getFields());
            }
        }
        return fields;
    }

    public void applyRequires(AuthTrack track, Set<String> codes) {
        if (codes == null || codes.isEmpty() || track == null) {
            return;
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String code : new ArrayList<String>(codes)) {
                AuthScopeDef def = getDefinition(track, code);
                if (def == null || def.getRequires() == null) {
                    continue;
                }
                for (String required : def.getRequires()) {
                    if (StringUtils.isNotBlank(required) && !codes.contains(required)) {
                        codes.add(required);
                        changed = true;
                    }
                }
            }
        }
    }

    public AuthScopeDef getDefinition(AuthTrack track, String code) {
        if (track == null || StringUtils.isBlank(code)) {
            return null;
        }
        Map<String, AuthScopeDef> bucket = trackDefinitions.get(track);
        if (bucket == null) {
            return null;
        }
        return bucket.get(AuthScopeSet.normalize(code));
    }

    public boolean isRegisteredBuiltin(String code) {
        if (StringUtils.isBlank(code)) {
            return false;
        }
        String normalized = AuthScopeSet.normalize(code);
        for (AuthTrack track : AuthTrack.values()) {
            AuthScopeDef def = getDefinition(track, normalized);
            if (def != null && def.isBuiltin()) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidCode(AuthTrack track, String code) {
        if (track == null || StringUtils.isBlank(code)) {
            return false;
        }
        AuthScopeDef def = getDefinition(track, code);
        return def != null && def.isEnabled();
    }

    private AuthScopeSet resolveClientAllowed(AuthTrack track, List<String> clientScopes) {
        if (clientScopes == null || clientScopes.isEmpty()) {
            return AuthScopeSet.of(OplConstants.DEFAULT_SCOPE).expand(this, track);
        }
        boolean all = false;
        Set<String> allowed = new LinkedHashSet<String>();
        for (String raw : clientScopes) {
            String code = AuthScopeSet.normalize(raw);
            if (AuthScopeSet.ALL.equals(code)) {
                all = true;
                break;
            }
            if (AuthScopeSet.BASIC.equals(code)) {
                allowed.addAll(AuthScopeSet.basicFor(track).getCodes());
            } else if (StringUtils.isNotBlank(code)) {
                allowed.add(code);
            }
        }
        if (all) {
            allowed.addAll(enabledCodes(track));
        }
        applyRequires(track, allowed);
        return AuthScopeSet.fromCollection(allowed);
    }

    private void registerBuiltins() {
        for (Map<String, AuthScopeDef> bucket : trackDefinitions.values()) {
            bucket.clear();
        }
        registerBuiltin(AuthScopeDef.of("identity", "获取用户唯一标识", AuthTrack.OAUTH, AuthField.uuid).sensitivity(AuthScopeSensitivity.low));
        registerBuiltin(AuthScopeDef.of("profile", "查看基本资料", AuthTrack.OAUTH, AuthField.nickname, AuthField.icon, AuthField.username).sensitivity(AuthScopeSensitivity.low));
        registerBuiltin(AuthScopeDef.of("phone", "查看手机号", AuthTrack.OAUTH, AuthField.mobile).sensitivity(AuthScopeSensitivity.high));
        registerBuiltin(AuthScopeDef.of("email", "查看邮箱", AuthTrack.OAUTH, AuthField.email).sensitivity(AuthScopeSensitivity.high));
        registerBuiltin(AuthScopeDef.of("verified", "查看实名认证状态", AuthTrack.OAUTH, AuthField.verified).sensitivity(AuthScopeSensitivity.medium));
        registerBuiltin(AuthScopeDef.of("status", "查看账号状态", AuthTrack.OAUTH, AuthField.status).sensitivity(AuthScopeSensitivity.medium));
        registerBuiltin(AuthScopeDef.of("openid", "应用内识别身份", AuthTrack.OPL, AuthField.openId).sensitivity(AuthScopeSensitivity.low));
        registerBuiltin(AuthScopeDef.of("unionid", "跨应用识别身份", AuthTrack.OPL, AuthField.unionId).requires("openid").sensitivity(AuthScopeSensitivity.low));
        registerBuiltin(AuthScopeDef.of("profile", "查看基本资料", AuthTrack.OPL, AuthField.nickname, AuthField.icon).sensitivity(AuthScopeSensitivity.low));
        registerBuiltin(AuthScopeDef.of("phone", "查看手机号", AuthTrack.OPL, AuthField.mobile).sensitivity(AuthScopeSensitivity.high));
        registerBuiltin(AuthScopeDef.of("email", "查看邮箱", AuthTrack.OPL, AuthField.email).sensitivity(AuthScopeSensitivity.high));
        registerBuiltin(AuthScopeDef.of("verified", "查看实名认证状态", AuthTrack.OPL, AuthField.verified).sensitivity(AuthScopeSensitivity.medium));
        registerBuiltin(AuthScopeDef.of("status", "查看账号状态", AuthTrack.OPL, AuthField.status).sensitivity(AuthScopeSensitivity.medium));
    }

    private void registerBuiltin(AuthScopeDef def) {
        if (def == null || StringUtils.isBlank(def.getCode())) {
            return;
        }
        normalizeDef(def);
        def.setBuiltin(true);
        putForTracks(def);
    }

    private void registerCustom(AuthScopeDef def) {
        if (def == null || StringUtils.isBlank(def.getCode())) {
            return;
        }
        normalizeDef(def);
        def.setBuiltin(false);
        putForTracks(def);
    }

    private void updateBuiltinState(AuthScopeDef def) {
        if (def == null || def.getTracks() == null) {
            return;
        }
        for (AuthTrack track : def.getTracks()) {
            AuthScopeDef existing = getDefinition(track, def.getCode());
            if (existing != null && existing.isBuiltin()) {
                existing.setEnabled(def.isEnabled());
                if (StringUtils.isNotBlank(def.getLabel())) {
                    existing.setLabel(def.getLabel());
                }
            }
        }
    }

    private void putForTracks(AuthScopeDef def) {
        if (def.getTracks() == null || def.getTracks().isEmpty()) {
            return;
        }
        for (AuthTrack track : def.getTracks()) {
            Map<String, AuthScopeDef> bucket = trackDefinitions.get(track);
            if (bucket != null) {
                bucket.put(def.getCode(), def);
            }
        }
    }

    private void normalizeDef(AuthScopeDef def) {
        def.setCode(AuthScopeSet.normalize(def.getCode()));
        if (def.getRequires() == null) {
            def.setRequires(Collections.<String>emptyList());
        }
        if (def.getTracks() == null) {
            def.setTracks(EnumSet.noneOf(AuthTrack.class));
        }
        if (def.getFields() == null) {
            def.setFields(EnumSet.noneOf(AuthField.class));
        }
    }
}
