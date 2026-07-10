package cn.org.autumn.modules.opl.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.modules.opl.dao.OpenCodeDao;
import cn.org.autumn.modules.opl.entity.OpenCodeEntity;
import cn.org.autumn.utils.Uuid;
import com.qiniu.util.Md5;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenCodeService extends ModuleService<OpenCodeDao, OpenCodeEntity> {

    @Transactional(rollbackFor = Exception.class)
    public OpenCodeEntity issue(String appId, String userUuid, String redirectUri, String codeChallenge, String codeChallengeMethod, String grantedScope) {
        OpenCodeEntity codeEntity = new OpenCodeEntity();
        codeEntity.setCode(generateCode());
        codeEntity.setAppId(appId);
        codeEntity.setUser(userUuid);
        codeEntity.setRedirectUri(redirectUri);
        codeEntity.setScope(grantedScope);
        codeEntity.setCodeChallenge(StringUtils.isBlank(codeChallenge) ? null : codeChallenge.trim());
        codeEntity.setCodeChallengeMethod(StringUtils.isBlank(codeChallengeMethod) ? null : codeChallengeMethod.trim());
        Date now = new Date();
        codeEntity.setCreate(now);
        codeEntity.setExpire(new Date(now.getTime() + OplConstants.AUTH_CODE_TTL_SECONDS * 1000L));
        insert(codeEntity);
        return codeEntity;
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenCodeEntity issue(String appId, String userUuid, String redirectUri, String codeChallenge, String codeChallengeMethod) {
        return issue(appId, userUuid, redirectUri, codeChallenge, codeChallengeMethod, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenCodeEntity consume(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        OpenCodeEntity entity = baseMapper.getByCodeForUpdate(code);
        if (entity == null || entity.getExpire() == null || !entity.getExpire().after(new Date())) {
            return null;
        }
        deleteById(entity.getId());
        return entity;
    }

    public OpenCodeEntity getValidCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        OpenCodeEntity entity = baseMapper.getByCode(code);
        if (entity == null || entity.getExpire() == null || !entity.getExpire().after(new Date())) {
            return null;
        }
        return entity;
    }

    public boolean isValidCode(String code) {
        return getValidCode(code) != null;
    }

    private String generateCode() {
        return Md5.md5((Uuid.uuid() + System.nanoTime()).getBytes());
    }
}
