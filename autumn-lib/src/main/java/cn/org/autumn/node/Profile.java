package cn.org.autumn.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * 本机节点画像 POJO，对应磁盘文件 {@code node-profile.json}。
 * <p>
 * 字段均为单单词：{@code uuid}/{@code version}/{@code create}/{@code update}/{@code roles}/{@code labels}。
 * {@code roles} 非空表示运维已手动调整角色（{@link #adjusted()}），才会参与 LoopJob 角色门禁。
 */
@Getter
@Setter
public class Profile implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本机节点稳定身份（32 位小写 hex）。 */
    private String uuid;
    /** 画像结构版本号，供消费方演进字段时识别。 */
    private int version = 1;
    /** 首次创建时间（ISO-8601）。 */
    private String create;
    /** 最近写入时间（ISO-8601）；每次落盘刷新。 */
    private String update;
    /** 节点角色列表；空 = 未手动调整。 */
    private List<String> roles = new ArrayList<>();
    /** 开放扩展标签，框架不解释键含义。 */
    private Map<String, String> labels = new LinkedHashMap<>();

    public List<String> getRoles() {
        return roles == null ? Collections.emptyList() : roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
    }

    public Map<String, String> getLabels() {
        return labels == null ? Collections.emptyMap() : labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels != null ? new LinkedHashMap<>(labels) : new LinkedHashMap<>();
    }

    /** {@code roles} 中存在非空白项时视为已手动调整。 */
    public boolean adjusted() {
        for (String s : getRoles()) {
            if (s != null && !s.isBlank()) {
                return true;
            }
        }
        return false;
    }

    /** 深拷贝，避免调用方修改缓存实例。 */
    public Profile copy() {
        Profile p = new Profile();
        p.uuid = this.uuid;
        p.version = this.version;
        p.create = this.create;
        p.update = this.update;
        p.setRoles(this.roles);
        p.setLabels(this.labels);
        return p;
    }
}
