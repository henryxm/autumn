package cn.org.autumn.auth.scope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public class AuthScopeResolution implements Serializable {
    private static final long serialVersionUID = 1L;

    private final AuthTrack track;
    private final AuthScopeSet requested;
    private final AuthScopeSet granted;
    private final List<String> invalid;
    private final List<String> denied;

    public AuthScopeResolution(AuthTrack track, AuthScopeSet requested, AuthScopeSet granted, List<String> invalid, List<String> denied) {
        this.track = track;
        this.requested = requested == null ? AuthScopeSet.empty() : requested;
        this.granted = granted == null ? AuthScopeSet.empty() : granted;
        this.invalid = invalid == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<>(invalid));
        this.denied = denied == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<>(denied));
    }

    public boolean hasInvalid() {
        return !invalid.isEmpty();
    }

    public boolean hasDenied() {
        return !denied.isEmpty();
    }
}
