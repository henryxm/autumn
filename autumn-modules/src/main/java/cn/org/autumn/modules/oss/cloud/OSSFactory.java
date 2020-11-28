package cn.org.autumn.modules.oss.cloud;

import cn.org.autumn.utils.Constant;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.SpringContextUtils;

public final class OSSFactory {

    private static SysConfigService sysConfigService;

    public static CloudStorageService build() {
        if (null == sysConfigService)
            sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
        //获取云存储配置信息
        CloudStorageConfig config = sysConfigService.getCloudStorageConfig();
        if (config.getType() == Constant.CloudService.QINIU.getValue()) {
            return new QiniuCloudStorageService(config);
        } else if (config.getType() == Constant.CloudService.ALIYUN.getValue()) {
            return new AliyunCloudStorageService(config);
        } else if (config.getType() == Constant.CloudService.QCLOUD.getValue()) {
            return new QcloudCloudStorageService(config);
        }
        return null;
    }
}
