package cn.org.autumn.modules.client.dto;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthClientSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long oauthId;
    private String clientId;
    private String clientName;
    private Integer trusted;
    private Integer archived;
    private Date createTime;
    private boolean hasWeb;
    private boolean hasCombine;
    private boolean hasQrc;
    private boolean qrcEnabled;
}
