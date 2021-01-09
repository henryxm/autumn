package cn.org.autumn.modules.oss.service;

import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.site.InitFactory;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.oss.dao.SysOssDao;
import cn.org.autumn.modules.oss.entity.SysOssEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class SysOssService extends ServiceImpl<SysOssDao, SysOssEntity> implements InitFactory.Init {

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<SysOssEntity> page = this.selectPage(
                new Query<SysOssEntity>(params).getPage()
        );
        return new PageUtils(page);
    }

    public void init() {
        addLanguageColumnItem();
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("sys_sysoss_table_comment", "文件上传", "File upload");
        languageService.addLanguageColumnItem("sys_sysoss_column_url", "URL地址", "URL location");
        languageService.addLanguageColumnItem("sys_sysoss_column_create_date", "创建时间", "Create date");
        languageService.addLanguageColumnItem("sys_sysoss_cloud_storage_no_config", "云存储配置未配置", "Cloud storage no configuration");
        languageService.addLanguageColumnItem("sys_sysoss_support_jpg_png_gif", "只支持jpg、png、gif格式的图片！", "Only support jpg, png, gif");
        languageService.addLanguageColumnItem("sys_sysoss_string_cloud_storage_config", "云存储配置", "Cloud storage config");
        languageService.addLanguageColumnItem("sys_sysoss_string_delete", "删除", "Delete");
        languageService.addLanguageColumnItem("sys_sysoss_string_apply_qiniu_free_space", "免费申请(七牛)10GB储存空间", "Apply qiniu 10GB storage space for free");
        languageService.addLanguageColumnItem("sys_sysoss_string_storage_type", "存储类型", "Storage type");
        languageService.addLanguageColumnItem("sys_sysoss_string_qiniu", "七牛", "Qiniu");
        languageService.addLanguageColumnItem("sys_sysoss_string_aliyun", "阿里云", "Aliyun");
        languageService.addLanguageColumnItem("sys_sysoss_string_qcloud", "腾讯云", "Qcloud");
        languageService.addLanguageColumnItem("sys_sysoss_string_bing_qiniu_domain", "七牛绑定的域名", "Binding domain on qiniu");
        languageService.addLanguageColumnItem("sys_sysoss_string_domain", "域名", "Domain");
        languageService.addLanguageColumnItem("sys_sysoss_string_prefix", "路径前缀", "Prefix");
        languageService.addLanguageColumnItem("sys_sysoss_string_empty_default", "不设置默认为空", "Empty by default");
        languageService.addLanguageColumnItem("sys_sysoss_string_qiniu_access_key", "七牛AccessKey", "Qiniu Access Key");
        languageService.addLanguageColumnItem("sys_sysoss_string_qiniu_secret_key", "七牛SecretKey", "Qiniu Secret Key");
        languageService.addLanguageColumnItem("sys_sysoss_string_space_name", "空间名", "Storage name");
        languageService.addLanguageColumnItem("sys_sysoss_string_qiniu_space_name", "七牛存储空间名", "Qiniu storage name");
        languageService.addLanguageColumnItem("sys_sysoss_string_aliyun_domain", "阿里云绑定的域名", "Aliyun domain");
        languageService.addLanguageColumnItem("sys_sysoss_string_aliyun_endpoint", "阿里云EndPoint", "Aliyun endpoint");
        languageService.addLanguageColumnItem("sys_sysoss_string_aliyun_access_key_id", "阿里云AccessKeyId", "Aliyun Access Key Id");
        languageService.addLanguageColumnItem("sys_sysoss_string_aliyun_access_key_secret", "阿里云AccessKeySecret", "Aliyun Access Key Secret");
        languageService.addLanguageColumnItem("sys_sysoss_string_aliyun_bucket_name", "阿里云BucketName", "Aliyun Bucket Name");
        languageService.addLanguageColumnItem("sys_sysoss_string_qcloud_domain", "腾讯云绑定的域名", "Qcloud domain");
        languageService.addLanguageColumnItem("sys_sysoss_string_qcloud_app_id", "腾讯云AppId", "Qcloud AppId");
        languageService.addLanguageColumnItem("sys_sysoss_string_qcloud_secret_id", "腾讯云SecretId", "Qcloud SecretId");
        languageService.addLanguageColumnItem("sys_sysoss_string_qcloud_secret_key", "腾讯云SecretKey", "Qcloud SecretKey");
        languageService.addLanguageColumnItem("sys_sysoss_string_qcloud_bucket_name", "腾讯云BucketName", "Qcloud BucketName");
        languageService.addLanguageColumnItem("sys_sysoss_string_qcloud_bucket_location", "Bucket所属地区", "Bucket Location");
        languageService.addLanguageColumnItem("sys_sysoss_string_qcloud_bucket_location_sample", "如：sh（可选值 ，华南：gz 华北：tj 华东：sh）", "Example：sh（possible value）");
    }
}
