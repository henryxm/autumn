package cn.org.autumn.modules.usr.support;

import cn.org.autumn.modules.usr.entity.UserProfileEntity;

/**
 * 出站前回填头像展示 URL（有 {@code iconUuid} 时签发签名 URL；不落库）。
 * 由业务模块（如 account）提供实现；缺省无 Bean 时保持原 {@code icon}。
 * <p>
 * 调用方应对 <strong>副本</strong> 使用 {@link #fillDisplayIcon}，避免污染 cache / Shiro principal。
 */
public interface UserProfileIconFiller {

    /**
     * 返回展示 URL：有 iconUuid 则签发；否则返回 profile.icon（可能为空）。
     * 不要求 mutate profile。
     */
    String resolveDisplayIcon(UserProfileEntity profile);

    /**
     * 将展示 URL 写入内存中的 {@code profile.icon}（仅用于可丢弃副本）。
     */
    void fillDisplayIcon(UserProfileEntity profile);
}
