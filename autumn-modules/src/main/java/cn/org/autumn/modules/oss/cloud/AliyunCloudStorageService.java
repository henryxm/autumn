package cn.org.autumn.modules.oss.cloud;

import com.aliyun.oss.OSSClient;
import cn.org.autumn.exception.AException;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.SetBucketAclRequest;
import com.aliyun.oss.model.SetObjectAclRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class AliyunCloudStorageService extends CloudStorageService {
    private OSSClient client;

    public AliyunCloudStorageService(CloudStorageConfig config) {
        this.config = config;

        //初始化
        init();
    }

    private void init() {
        client = new OSSClient(config.getAliyunEndPoint(), config.getAliyunAccessKeyId(),
                config.getAliyunAccessKeySecret());
    }

    @Override
    public String upload(byte[] data, String path, Object metadata) {
        return upload(new ByteArrayInputStream(data), path, metadata);
    }

    @Override
    public String upload(InputStream inputStream, String path, Object metadata) {
        try {
            client.putObject(config.getAliyunBucketName(), path, inputStream, (ObjectMetadata) metadata);
        } catch (Exception e) {
            throw new AException("上传文件失败，请检查配置信息", e);
        }
        return config.getAliyunDomain() + "/" + path;
    }

    public String remove(String path) {
        try {
            client.deleteObject(config.getAliyunBucketName(), path);
        } catch (Exception e) {
            throw new AException("上传文件失败，请检查配置信息", e);
        }
        return config.getAliyunDomain() + "/" + path;
    }

    @Override
    public String upload(byte[] data, String path) {
        return upload(new ByteArrayInputStream(data), path);
    }

    @Override
    public String upload(InputStream inputStream, String path) {
        try {
            client.putObject(config.getAliyunBucketName(), path, inputStream);
        } catch (Exception e) {
            throw new AException("上传文件失败，请检查配置信息", e);
        }
        return config.getAliyunDomain() + "/" + path;
    }

    public void setObjectAcl(String key, AccessControl access) {
        CannedAccessControlList accessControlList = CannedAccessControlList.parse(access.toString());
        SetObjectAclRequest request = new SetObjectAclRequest(config.getAliyunBucketName(), key, accessControlList);
        client.setObjectAcl(request);
    }

    public void setBucketAcl(AccessControl access) {
        CannedAccessControlList accessControlList = CannedAccessControlList.parse(access.toString());
        SetBucketAclRequest request = new SetBucketAclRequest(config.getAliyunBucketName(), accessControlList);
        client.setBucketAcl(request);
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        return upload(data, getPath(config.getAliyunPrefix(), suffix));
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        return upload(inputStream, getPath(config.getAliyunPrefix(), suffix));
    }
}
