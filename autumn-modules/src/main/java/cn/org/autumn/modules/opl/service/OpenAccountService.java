package cn.org.autumn.modules.opl.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.AccountHandler;
import cn.org.autumn.modules.opl.dao.OpenAccountDao;
import cn.org.autumn.modules.opl.entity.OpenAccountEntity;
import cn.org.autumn.utils.Uuid;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenAccountService extends ModuleService<OpenAccountDao, OpenAccountEntity> implements AccountHandler {

    @Transactional(rollbackFor = Exception.class)
    public OpenAccountEntity getOrCreateByUser(String userUuid, String name) {
        Uuid.requireValid(userUuid);
        OpenAccountEntity existing = baseMapper.getByUser(userUuid);
        if (existing != null) {
            if (existing.getStatus() == OpenAccountEntity.STATUS_DISABLED) {
                throw new IllegalStateException("开发者账号已禁用");
            }
            return existing;
        }
        OpenAccountEntity account = new OpenAccountEntity();
        account.setUser(userUuid);
        account.setName(StringUtils.defaultIfBlank(name, "开发者"));
        account.setStatus(OpenAccountEntity.STATUS_ACTIVE);
        Date now = new Date();
        account.setCreate(now);
        account.setUpdate(now);
        insert(account);
        return account;
    }

    public OpenAccountEntity getByUser(String userUuid) {
        if (StringUtils.isBlank(userUuid)) {
            return null;
        }
        return baseMapper.getByUser(userUuid);
    }

    public OpenAccountEntity getByUuid(String accountUuid) {
        if (StringUtils.isBlank(accountUuid)) {
            return null;
        }
        return baseMapper.getByUuid(accountUuid);
    }

    public OpenAccountEntity requireActiveAccount(String accountUuid) {
        if (StringUtils.isBlank(accountUuid)) {
            throw new IllegalArgumentException("开发者账号不能为空");
        }
        OpenAccountEntity account = baseMapper.getByUuid(accountUuid);
        if (account == null) {
            throw new IllegalArgumentException("开发者账号不存在");
        }
        if (account.getStatus() != OpenAccountEntity.STATUS_ACTIVE) {
            throw new IllegalStateException("开发者账号已禁用");
        }
        return account;
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableByUser(String userUuid) {
        OpenAccountEntity account = getByUser(userUuid);
        if (account == null) {
            return;
        }
        account.setStatus(OpenAccountEntity.STATUS_DISABLED);
        account.setUpdate(new Date());
        updateById(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void canceled(Account obj) {
        if (obj == null || StringUtils.isBlank(obj.getUuid())) {
            return;
        }
        disableByUser(obj.getUuid());
    }
}
