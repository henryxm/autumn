package cn.org.autumn.modules.safe.dto;

import cn.org.autumn.modules.safe.entity.PayUserBiometricEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class PayBiometricDeviceView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;
    private String deviceId;
    private String platform;
    private String credentialId;
    private Date lastUsedTime;
    private Date createTime;

    public static PayBiometricDeviceView of(PayUserBiometricEntity entity) {
        PayBiometricDeviceView view = new PayBiometricDeviceView();
        view.setUuid(entity.getUuid());
        view.setDeviceId(entity.getDeviceId());
        view.setPlatform(entity.getPlatform());
        view.setCredentialId(entity.getCredentialId());
        view.setLastUsedTime(entity.getLastUsedTime());
        view.setCreateTime(entity.getCreateTime());
        return view;
    }
}
