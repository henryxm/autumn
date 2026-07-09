package cn.org.autumn.modules.client.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScanLoginCredentialResolveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String id;
}
