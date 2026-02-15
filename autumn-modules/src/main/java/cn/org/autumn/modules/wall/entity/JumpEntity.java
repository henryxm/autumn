package cn.org.autumn.modules.wall.entity;

import cn.org.autumn.table.annotation.*;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("wall_jump")
@Table(value = "wall_jump", comment = "防御跳转")
@Indexes({@Index(name = "hosturi", fields = {@IndexField(field = "host"), @IndexField(field = "uri")}, indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE)})
public class JumpEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(comment = "域名", length = 50, defaultValue = "")
    private String host;

    @Column(comment = "资源", length = 50, defaultValue = "")
    private String uri;

    @Column(comment = "跳转", defaultValue = "")
    private String url;

    @Column(comment = "开启", defaultValue = "1")
    private boolean enable;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
