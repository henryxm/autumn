package cn.org.autumn.oauth.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuthClientSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String clientId;
    private String clientName;
    private String scope;
}
