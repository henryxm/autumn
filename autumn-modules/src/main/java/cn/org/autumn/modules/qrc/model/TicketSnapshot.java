package cn.org.autumn.modules.qrc.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;
    private String intent;
    private String status;
    private Map<String, String> payload = new HashMap<>();
    private String scanner;
    private String subject;
    private String exchange;
    private Map<String, String> result = new HashMap<>();
    private long created;
    private long expired;
    private String ip;
    private String agent;
    private String redirect;
}
