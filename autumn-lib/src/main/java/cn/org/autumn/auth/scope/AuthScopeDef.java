package cn.org.autumn.auth.scope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * 单条 scope 元数据（内置或 DB 自定义）。
 */
@Getter
@Setter
public class AuthScopeDef implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String label;
    private Set<AuthTrack> tracks = Collections.emptySet();
    private Set<AuthField> fields = Collections.emptySet();
    private AuthScopeSensitivity sensitivity = AuthScopeSensitivity.low;
    private List<String> requires = Collections.emptyList();
    private boolean enabled = true;
    private boolean builtin;
    private boolean alias;

    public static AuthScopeDef of(String code, String label, AuthTrack track, AuthField... fields) {
        AuthScopeDef def = new AuthScopeDef();
        def.setCode(code);
        def.setLabel(label);
        def.setTracks(EnumSet.of(track));
        if (fields == null || fields.length == 0) {
            def.setFields(Collections.<AuthField>emptySet());
        } else {
            def.setFields(EnumSet.of(fields[0], copyRest(fields)));
        }
        def.setBuiltin(true);
        return def;
    }

    public AuthScopeDef tracks(AuthTrack... trackArr) {
        EnumSet<AuthTrack> set = EnumSet.noneOf(AuthTrack.class);
        if (trackArr != null) {
            for (AuthTrack track : trackArr) {
                if (track != null) {
                    set.add(track);
                }
            }
        }
        this.tracks = set;
        return this;
    }

    public AuthScopeDef fields(AuthField... fieldArr) {
        if (fieldArr == null || fieldArr.length == 0) {
            this.fields = EnumSet.noneOf(AuthField.class);
        } else {
            this.fields = EnumSet.of(fieldArr[0], copyRest(fieldArr));
        }
        return this;
    }

    public AuthScopeDef sensitivity(AuthScopeSensitivity value) {
        this.sensitivity = value == null ? AuthScopeSensitivity.low : value;
        return this;
    }

    public AuthScopeDef requires(String... codes) {
        if (codes == null || codes.length == 0) {
            this.requires = Collections.emptyList();
        } else {
            List<String> list = new ArrayList<>();
            for (String code : codes) {
                if (code != null && !code.trim().isEmpty()) {
                    list.add(code.trim().toLowerCase());
                }
            }
            this.requires = list;
        }
        return this;
    }

    private static AuthField[] copyRest(AuthField[] fields) {
        if (fields.length <= 1) {
            return new AuthField[0];
        }
        AuthField[] rest = new AuthField[fields.length - 1];
        System.arraycopy(fields, 1, rest, 0, rest.length);
        return rest;
    }
}
