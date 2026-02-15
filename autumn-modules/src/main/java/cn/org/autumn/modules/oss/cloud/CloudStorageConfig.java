package cn.org.autumn.modules.oss.cloud;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;
import cn.org.autumn.modules.sys.service.SysCategoryService;
import cn.org.autumn.validator.group.AliyunGroup;
import cn.org.autumn.validator.group.QcloudGroup;
import cn.org.autumn.validator.group.QiniuGroup;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

import static cn.org.autumn.modules.sys.service.SysConfigService.*;

@ConfigParam(paramKey = CLOUD_STORAGE_CONFIG_KEY, category = SysCategoryService.storage_config, name = category_lang_string + SysCategoryService.storage_config + "_name", description = category_lang_string + SysCategoryService.storage_config + "_description")
public class CloudStorageConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    //类型 1：七牛  2：阿里云  3：腾讯云
    @Range(min = 1, max = 3, message = "类型错误")
    @ConfigField(category = InputType.NumberType, status = 1, name = "云存储类型", description = "配置云存储的类型,1:七牛;2:阿里云;3:腾讯云")
    private Integer type;

    //七牛绑定的域名
    @NotBlank(message = "七牛绑定的域名不能为空", groups = QiniuGroup.class)
    @URL(message = "七牛绑定的域名格式不正确", groups = QiniuGroup.class)
    @ConfigField(category = InputType.StringType, name = "七牛绑定域名", description = "配置七牛绑定的域名")
    private String qiniuDomain;
    //七牛路径前缀
    @ConfigField(category = InputType.StringType, name = "七牛路径前缀", description = "配置七牛路径前缀")
    private String qiniuPrefix;
    //七牛ACCESS_KEY
    @ConfigField(category = InputType.StringType, name = "七牛访问令牌", description = "配置七牛令牌")
    @NotBlank(message = "七牛AccessKey不能为空", groups = QiniuGroup.class)
    private String qiniuAccessKey;
    //七牛SECRET_KEY
    @ConfigField(category = InputType.StringType, name = "七牛访问密匙", description = "配置七牛密匙")
    @NotBlank(message = "七牛SecretKey不能为空", groups = QiniuGroup.class)
    private String qiniuSecretKey;
    //七牛存储空间名
    @ConfigField(category = InputType.StringType, name = "七牛存储空间", description = "配置七牛存储空间")
    @NotBlank(message = "七牛空间名不能为空", groups = QiniuGroup.class)
    private String qiniuBucketName;

    //阿里云绑定的域名
    @NotBlank(message = "阿里云绑定的域名不能为空", groups = AliyunGroup.class)
    @URL(message = "阿里云绑定的域名格式不正确", groups = AliyunGroup.class)
    @ConfigField(category = InputType.StringType, name = "阿里云域名", description = "配置阿里云域名")
    private String aliyunDomain;
    //阿里云路径前缀
    @ConfigField(category = InputType.StringType, name = "阿里云路径前缀", description = "配置阿里云路径前缀")
    private String aliyunPrefix;
    //阿里云EndPoint
    @ConfigField(category = InputType.StringType, name = "阿里云入口点", description = "配置阿里云入口点")
    @NotBlank(message = "阿里云EndPoint不能为空", groups = AliyunGroup.class)
    private String aliyunEndPoint;
    //阿里云AccessKeyId
    @ConfigField(category = InputType.StringType, name = "阿里云访问令牌", description = "配置阿里云访问令牌")
    @NotBlank(message = "阿里云AccessKeyId不能为空", groups = AliyunGroup.class)
    private String aliyunAccessKeyId;
    //阿里云AccessKeySecret
    @ConfigField(category = InputType.StringType, name = "阿里云访问密匙", description = "配置阿里云访问密匙")
    @NotBlank(message = "阿里云AccessKeySecret不能为空", groups = AliyunGroup.class)
    private String aliyunAccessKeySecret;
    //阿里云BucketName
    @ConfigField(category = InputType.StringType, name = "阿里云存储空间", description = "配置阿里云存储空间")
    @NotBlank(message = "阿里云BucketName不能为空", groups = AliyunGroup.class)
    private String aliyunBucketName;

    //腾讯云绑定的域名
    @NotBlank(message = "腾讯云绑定的域名不能为空", groups = QcloudGroup.class)
    @URL(message = "腾讯云绑定的域名格式不正确", groups = QcloudGroup.class)
    @ConfigField(category = InputType.StringType, name = "腾讯云存储空间", description = "配置腾讯云存储空间")
    private String qcloudDomain;
    //腾讯云路径前缀
    @ConfigField(category = InputType.StringType, name = "腾讯云路径前缀", description = "配置腾讯云路径前缀")
    private String qcloudPrefix;
    //腾讯云AppId
    @NotNull(message = "腾讯云AppId不能为空", groups = QcloudGroup.class)
    @ConfigField(category = InputType.NumberType, name = "腾讯云应用ID", description = "配置腾讯云应用ID")
    private Integer qcloudAppId;
    //腾讯云SecretId
    @ConfigField(category = InputType.StringType, name = "腾讯云访问令牌", description = "配置腾讯云访问令牌")
    @NotBlank(message = "腾讯云SecretId不能为空", groups = QcloudGroup.class)
    private String qcloudSecretId;
    //腾讯云SecretKey
    @ConfigField(category = InputType.StringType, name = "腾讯云访问密匙", description = "配置腾讯云访问密匙")
    @NotBlank(message = "腾讯云SecretKey不能为空", groups = QcloudGroup.class)
    private String qcloudSecretKey;
    //腾讯云BucketName
    @ConfigField(category = InputType.StringType, name = "腾讯云存储空间", description = "配置腾讯云存储空间")
    @NotBlank(message = "腾讯云BucketName不能为空", groups = QcloudGroup.class)
    private String qcloudBucketName;
    //腾讯云COS所属地区
    @ConfigField(category = InputType.StringType, name = "腾讯云所属地区", description = "配置腾讯云所属地区")
    @NotBlank(message = "所属地区不能为空", groups = QcloudGroup.class)
    private String qcloudRegion;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getQiniuDomain() {
        return qiniuDomain;
    }

    public void setQiniuDomain(String qiniuDomain) {
        this.qiniuDomain = qiniuDomain;
    }

    public String getQiniuAccessKey() {
        return qiniuAccessKey;
    }

    public void setQiniuAccessKey(String qiniuAccessKey) {
        this.qiniuAccessKey = qiniuAccessKey;
    }

    public String getQiniuSecretKey() {
        return qiniuSecretKey;
    }

    public void setQiniuSecretKey(String qiniuSecretKey) {
        this.qiniuSecretKey = qiniuSecretKey;
    }

    public String getQiniuBucketName() {
        return qiniuBucketName;
    }

    public void setQiniuBucketName(String qiniuBucketName) {
        this.qiniuBucketName = qiniuBucketName;
    }

    public String getQiniuPrefix() {
        return qiniuPrefix;
    }

    public void setQiniuPrefix(String qiniuPrefix) {
        this.qiniuPrefix = qiniuPrefix;
    }

    public String getAliyunDomain() {
        return aliyunDomain;
    }

    public void setAliyunDomain(String aliyunDomain) {
        this.aliyunDomain = aliyunDomain;
    }

    public String getAliyunPrefix() {
        return aliyunPrefix;
    }

    public void setAliyunPrefix(String aliyunPrefix) {
        this.aliyunPrefix = aliyunPrefix;
    }

    public String getAliyunEndPoint() {
        return aliyunEndPoint;
    }

    public void setAliyunEndPoint(String aliyunEndPoint) {
        this.aliyunEndPoint = aliyunEndPoint;
    }

    public String getAliyunAccessKeyId() {
        return aliyunAccessKeyId;
    }

    public void setAliyunAccessKeyId(String aliyunAccessKeyId) {
        this.aliyunAccessKeyId = aliyunAccessKeyId;
    }

    public String getAliyunAccessKeySecret() {
        return aliyunAccessKeySecret;
    }

    public void setAliyunAccessKeySecret(String aliyunAccessKeySecret) {
        this.aliyunAccessKeySecret = aliyunAccessKeySecret;
    }

    public String getAliyunBucketName() {
        return aliyunBucketName;
    }

    public void setAliyunBucketName(String aliyunBucketName) {
        this.aliyunBucketName = aliyunBucketName;
    }

    public String getQcloudDomain() {
        return qcloudDomain;
    }

    public void setQcloudDomain(String qcloudDomain) {
        this.qcloudDomain = qcloudDomain;
    }

    public String getQcloudPrefix() {
        return qcloudPrefix;
    }

    public void setQcloudPrefix(String qcloudPrefix) {
        this.qcloudPrefix = qcloudPrefix;
    }

    public Integer getQcloudAppId() {
        return qcloudAppId;
    }

    public void setQcloudAppId(Integer qcloudAppId) {
        this.qcloudAppId = qcloudAppId;
    }

    public String getQcloudSecretId() {
        return qcloudSecretId;
    }

    public void setQcloudSecretId(String qcloudSecretId) {
        this.qcloudSecretId = qcloudSecretId;
    }

    public String getQcloudSecretKey() {
        return qcloudSecretKey;
    }

    public void setQcloudSecretKey(String qcloudSecretKey) {
        this.qcloudSecretKey = qcloudSecretKey;
    }

    public String getQcloudBucketName() {
        return qcloudBucketName;
    }

    public void setQcloudBucketName(String qcloudBucketName) {
        this.qcloudBucketName = qcloudBucketName;
    }

    public String getQcloudRegion() {
        return qcloudRegion;
    }

    public void setQcloudRegion(String qcloudRegion) {
        this.qcloudRegion = qcloudRegion;
    }
}
