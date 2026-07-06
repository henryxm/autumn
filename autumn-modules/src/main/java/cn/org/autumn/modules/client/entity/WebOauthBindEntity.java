package cn.org.autumn.modules.client.entity;

import cn.org.autumn.annotation.Cache;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.IndexField;
import cn.org.autumn.table.annotation.IndexMethodEnum;
import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.annotation.Indexes;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("client_web_oauth_bind")
@Table(value = "client_web_oauth_bind", comment = "OAuth绑定:上游uuid与本地用户")
@Indexes({
        @Index(name = "auth_upper", indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE, fields = {
                @IndexField(field = "authentication", length = 32),
                @IndexField(field = "upper", length = 32)
        }),
        @Index(name = "auth_user", indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE, fields = {
                @IndexField(field = "authentication", length = 32),
                @IndexField(field = "user", length = 32)
        })
})
public class WebOauthBindEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = DataType.BIGINT, length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "用户:本地sys_user.uuid")
    @Index
    private String user;

    @Column(length = 32, comment = "接入:WebAuthentication.uuid")
    @Index
    private String authentication;

    @Cache(name = "upper")
    @Column(length = 32, comment = "上游:授权方用户uuid")
    @Index
    private String upper;

    @Column(type = DataType.DATETIME, comment = "创建:创建时间")
    private Date create;

    @Column(type = DataType.DATETIME, comment = "更新:更新时间")
    private Date update;
}
