package cn.org.autumn.modules.sys.shiro;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Session 主体 password 与库存储串对齐策略（纯判定，无 I/O）。
 * <p>
 * 用于惰性升级后「Session 仍为遗留 SHA、库已为 {@code pbkdf2$}」时同步而非踢下线；
 * 真改密 / 重置导致两侧哈希均已是新算法却不等时，判定为 MISMATCH。
 */
public final class SessionPasswordAlignSupport {

    public enum Result {
        /** 已一致 */
        MATCH,
        /** 库为 PBKDF2、会话为遗留哈希：应同步 Subject */
        SYNC_UPGRADE,
        /** 真改密或不一致：应强制重登 */
        MISMATCH
    }

    private SessionPasswordAlignSupport() {
    }

    public static Result classify(String sessionPassword, String storePassword) {
        if (Objects.equals(sessionPassword, storePassword)) {
            return Result.MATCH;
        }
        // 空库值不当升级同步
        if (StringUtils.isBlank(storePassword)) {
            return Result.MISMATCH;
        }
        if (PasswordHashSupport.isPbkdf2(storePassword) && !PasswordHashSupport.isPbkdf2(sessionPassword)) {
            return Result.SYNC_UPGRADE;
        }
        return Result.MISMATCH;
    }
}
