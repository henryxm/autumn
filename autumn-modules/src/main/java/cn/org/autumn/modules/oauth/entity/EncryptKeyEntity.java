package cn.org.autumn.modules.oauth.entity;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.Table;
import cn.org.autumn.table.data.DataType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@TableName("oauth_encrypt_key")
@Table(value = "oauth_encrypt_key", comment = "加密秘钥")
@Getter
@Setter
public class EncryptKeyEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 50, comment = "ID", isUnique = true)
    private String session;

    @Column(comment = "服务器公钥", type = DataType.TEXT)
    private String publicKey;

    @Column(comment = "服务器私钥", type = DataType.TEXT)
    private String privateKey;

    @Column(comment = "客户端公钥", type = DataType.TEXT)
    private String clientKey;

    @Column(comment = "AES秘钥", length = 64)
    private String aesKey;

    @Column(comment = "AES向量", length = 32)
    private String aesIv;

    @Column(comment = "过期时间")
    private Date expire;

    @Column(comment = "创建时间")
    @TableField("`create`")
    private Date create;
}
