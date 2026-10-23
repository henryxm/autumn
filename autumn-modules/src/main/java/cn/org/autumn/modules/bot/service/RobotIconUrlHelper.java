package cn.org.autumn.modules.bot.service;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.modules.objectstorage.dto.ObjectStorageAccessInfo;
import cn.org.autumn.modules.objectstorage.service.ObjectStorageApplicationService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 机器人头像：对象存储 uuid → 展示 URL（稳定链优先，否则签名；写失败抛业务错）。
 */
@Component
public class RobotIconUrlHelper {

    public static final String FAIL_MSG =
            "图片地址解析失败，请确认已上传且为可访问对象（bizType=im_group_info，accessLevel=PUBLIC）";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired(required = false)
    ObjectStorageApplicationService objectStorageApplicationService;

    public String resolveAccessUrl(String objectUuid) {
        if (StringUtils.isBlank(objectUuid)) {
            return "";
        }
        String uuid = objectUuid.trim();
        String stable = resolveStableAccessUrl(uuid);
        if (StringUtils.isNotBlank(stable)) {
            return stable;
        }
        return resolveSignedUrl(uuid);
    }

    public String requireAccessUrl(String objectUuid) throws CodeException {
        String url = resolveAccessUrl(objectUuid);
        if (StringUtils.isBlank(url)) {
            throw new CodeException(FAIL_MSG);
        }
        return url;
    }

    private String resolveStableAccessUrl(String uuid) {
        if (objectStorageApplicationService == null || StringUtils.isBlank(uuid)) {
            return "";
        }
        try {
            ObjectStorageAccessInfo info = objectStorageApplicationService.statByUuid(uuid.trim());
            if (info != null && StringUtils.isNotBlank(info.getAccessUrl())) {
                return info.getAccessUrl();
            }
        } catch (Exception e) {
            log.warn("机器人头像取稳定链失败 uuid={} err={}", uuid, e.getMessage());
        }
        return "";
    }

    private String resolveSignedUrl(String uuid) {
        if (objectStorageApplicationService == null || StringUtils.isBlank(uuid)) {
            return "";
        }
        try {
            Map<String, Object> batch = objectStorageApplicationService
                    .signedAccessBatchByUuidForClient(Collections.singletonList(uuid.trim()), 0);
            Object itemsObj = batch == null ? null : batch.get("items");
            if (!(itemsObj instanceof List<?> items)) {
                return "";
            }
            for (Object raw : items) {
                if (!(raw instanceof Map<?, ?> item)) {
                    continue;
                }
                Object ok = item.get("ok");
                if (!(ok instanceof Boolean) || !((Boolean) ok)) {
                    continue;
                }
                Object id = item.get("uuid");
                Object url = item.get("url");
                if (id instanceof String sid && url instanceof String surl
                        && uuid.equals(sid) && StringUtils.isNotBlank(surl)) {
                    return surl;
                }
            }
        } catch (Exception e) {
            log.warn("机器人头像签发URL失败 uuid={} err={}", uuid, e.getMessage());
        }
        return "";
    }
}
