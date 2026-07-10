package cn.org.autumn.auth.scope;

import cn.org.autumn.opl.OplConstants;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;

/**
 * 不可变 scope 集合。
 */
public final class AuthScopeSet implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BASIC = "basic";
    public static final String ALL = "all";

    private final Set<String> codes;

    private AuthScopeSet(Set<String> codes) {
        this.codes = Collections.unmodifiableSet(new LinkedHashSet<>(codes));
    }

    public static AuthScopeSet empty() {
        return new AuthScopeSet(Collections.<String>emptySet());
    }

    public static AuthScopeSet of(String... codes) {
        Set<String> set = new LinkedHashSet<>();
        if (codes != null) {
            for (String code : codes) {
                addNormalized(set, code);
            }
        }
        return new AuthScopeSet(set);
    }

    public static AuthScopeSet fromCollection(java.util.Collection<String> codes) {
        Set<String> set = new LinkedHashSet<>();
        if (codes != null) {
            for (String code : codes) {
                addNormalized(set, code);
            }
        }
        return new AuthScopeSet(set);
    }

    public static AuthScopeSet parse(String source) {
        if (StringUtils.isBlank(source)) {
            return empty();
        }
        Set<String> set = new LinkedHashSet<>();
        String[] parts = source.split("[;,；，\\s]+");
        for (String part : parts) {
            addNormalized(set, part);
        }
        return new AuthScopeSet(set);
    }

    public static AuthScopeSet basicFor(AuthTrack track) {
        if (track == AuthTrack.OPL) {
            return of("openid", "unionid", "profile");
        }
        return of("identity", "profile");
    }

    public static AuthScopeSet withDefault(String source) {
        AuthScopeSet parsed = parse(source);
        if (parsed.isEmpty()) {
            return of(BASIC);
        }
        return parsed;
    }

    public boolean isEmpty() {
        return codes.isEmpty();
    }

    public boolean contains(String code) {
        return codes.contains(normalize(code));
    }

    public Set<String> getCodes() {
        return codes;
    }

    public List<String> sortedCodes() {
        return new ArrayList<>(new TreeSet<>(codes));
    }

    public String toScopeString() {
        if (codes.isEmpty()) {
            return OplConstants.DEFAULT_SCOPE;
        }
        return StringUtils.join(sortedCodes(), " ");
    }

    public AuthScopeSet expand(AuthScopeCatalog catalog, AuthTrack track) {
        if (catalog == null || track == null) {
            return this;
        }
        Set<String> expanded = new LinkedHashSet<>();
        for (String code : codes) {
            if (BASIC.equals(code)) {
                expanded.addAll(basicFor(track).codes);
                continue;
            }
            if (ALL.equals(code)) {
                expanded.addAll(catalog.enabledCodes(track));
                continue;
            }
            expanded.add(code);
        }
        catalog.applyRequires(track, expanded);
        return new AuthScopeSet(expanded);
    }

    public AuthScopeSet intersect(AuthScopeSet other) {
        if (other == null || other.isEmpty()) {
            return empty();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String code : codes) {
            if (other.contains(code)) {
                result.add(code);
            }
        }
        return new AuthScopeSet(result);
    }

    private static void addNormalized(Set<String> set, String code) {
        String normalized = normalize(code);
        if (StringUtils.isNotBlank(normalized)) {
            set.add(normalized);
        }
    }

    public static String normalize(String code) {
        return code == null ? null : code.trim().toLowerCase();
    }
}
