package cn.org.autumn.modules.oss.cloud;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import cn.org.autumn.exception.AException;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 腾讯云存储
 */
public class QcloudCloudStorageService extends CloudStorageService {
    private COSClient client;

    public QcloudCloudStorageService(CloudStorageConfig config) {
        this.config = config;
        //初始化
        init();
    }

    private void init() {
        COSCredentials credentials = new BasicCOSCredentials(config.getQcloudSecretId(), config.getQcloudSecretKey());

        //初始化客户端配置
        ClientConfig clientConfig = new ClientConfig();
        //设置bucket所在的区域，如 ap-guangzhou, ap-shanghai, ap-beijing
        clientConfig.setRegion(new Region(config.getQcloudRegion()));

        client = new COSClient(credentials, clientConfig);
    }

    /**
     * 获取 bucket 名称（COS 5.x 格式为 bucketName-appId）
     */
    private String getBucketName() {
        return config.getQcloudBucketName() + "-" + config.getQcloudAppId();
    }

    @Override
    public String upload(byte[] data, String path) {
        //腾讯云 key 不以"/"开头
        String key = path.startsWith("/") ? path.substring(1) : path;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);

        PutObjectRequest putObjectRequest = new PutObjectRequest(getBucketName(), key, new ByteArrayInputStream(data), metadata);
        try {
            client.putObject(putObjectRequest);
        } catch (Exception e) {
            throw new AException("文件上传失败，" + e.getMessage());
        }

        return config.getQcloudDomain() + (path.startsWith("/") ? path : "/" + path);
    }

    @Override
    public String upload(InputStream inputStream, String path) {
        try {
            byte[] data = IOUtils.toByteArray(inputStream);
            return this.upload(data, path);
        } catch (IOException e) {
            throw new AException("上传文件失败", e);
        }
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        return upload(data, getPath(config.getQcloudPrefix(), suffix));
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        return upload(inputStream, getPath(config.getQcloudPrefix(), suffix));
    }

    @Override
    public void setObjectAcl(String key, AccessControl access) {

    }

    @Override
    public void setBucketAcl(AccessControl access) {

    }

    @Override
    public String upload(byte[] data, String path, Object metadata) {
        return null;
    }

    @Override
    public String upload(InputStream inputStream, String path, Object metadata) {
        return null;
    }

    @Override
    public String remove(String path) {
        return null;
    }
}
