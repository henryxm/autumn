package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 遗留 SHA-256 口令 → PBKDF2 惰性升级（原子 CAS）。
 * <p>
 * 硬性门禁：明文必须来自用户登录输入；必须先通过 {@link PasswordHashSupport#matchesLegacySha256}；
 * 禁止超级密码 / 空口令 / 已是 PBKDF2 的行；写库仅更新 password 列且带旧值条件，失败则不改库。
 */
@Slf4j
public final class LegacyPasswordUpgradeSupport {

    private LegacyPasswordUpgradeSupport() {
    }

    /**
     * @param userUuid              用户 uuid
     * @param rawPasswordFromUser   登录表单提交的明文（禁止传入超级密码、空串、Token 占位）
     * @param freshFromDb           刚从 DB 加载的行（含 password + salt）
     * @param mapper                用户表 Mapper
     * @return true 表示本线程成功把该行从旧 SHA 写成 PBKDF2
     */
    public static boolean tryUpgradeAtomically(String userUuid,
                                               String rawPasswordFromUser,
                                               SysUserEntity freshFromDb,
                                               BaseMapper<SysUserEntity> mapper) {
        if (mapper == null || freshFromDb == null || StringUtils.isBlank(userUuid)) {
            return false;
        }
        if (StringUtils.isBlank(rawPasswordFromUser)) {
            log.warn("Password upgrade rejected: blank plaintext uuid={}", userUuid);
            return false;
        }
        if (!userUuid.equals(freshFromDb.getUuid())) {
            log.warn("Password upgrade rejected: uuid mismatch expect={} actual={}", userUuid, freshFromDb.getUuid());
            return false;
        }
        String oldStored = freshFromDb.getPassword();
        String salt = freshFromDb.getSalt();
        if (PasswordHashSupport.isPbkdf2(oldStored)) {
            return false;
        }
        // 关键：仅当「用户明文」对「库中旧 SHA」成立时才允许哈希该明文
        if (!PasswordHashSupport.matchesLegacySha256(rawPasswordFromUser, oldStored, salt)) {
            log.warn("Password upgrade rejected: legacy SHA re-check failed uuid={}", userUuid);
            return false;
        }
        String newStored;
        try {
            newStored = PasswordHashSupport.hashNewFromVerifiedUserInput(rawPasswordFromUser);
        } catch (Exception e) {
            log.error("Password upgrade rejected: PBKDF2 hash failed uuid={} err={}", userUuid, e.getMessage());
            return false;
        }
        if (!PasswordHashSupport.isPbkdf2(newStored)) {
            log.error("Password upgrade rejected: hash result is not PBKDF2 uuid={}", userUuid);
            return false;
        }
        // 再验一次：新哈希必须能用同一用户明文校验（防止实现错误写入不可登录值）
        if (!PasswordHashSupport.matchesPbkdf2(rawPasswordFromUser, newStored)) {
            log.error("Password upgrade rejected: new PBKDF2 self-check failed uuid={}", userUuid);
            return false;
        }
        // 物理列名与实体驼峰下划线一致；CAS：仅当 password 仍为旧 SHA 时写入
        UpdateWrapper<SysUserEntity> uw = new UpdateWrapper<>();
        uw.eq("uuid", userUuid)
                .eq("password", oldStored)
                .set("password", newStored);
        int rows = mapper.update(null, uw);
        if (rows == 1) {
            freshFromDb.setPassword(newStored);
            if (log.isDebugEnabled()) {
                log.debug("Password upgraded atomically to PBKDF2 uuid={}", userUuid);
            }
            return true;
        }
        if (log.isDebugEnabled()) {
            log.debug("Password upgrade CAS miss (likely concurrent upgrade) uuid={} rows={}", userUuid, rows);
        }
        return false;
    }
}
