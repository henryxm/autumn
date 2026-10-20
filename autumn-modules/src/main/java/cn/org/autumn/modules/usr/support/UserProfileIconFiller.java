package cn.org.autumn.modules.usr.support;

import cn.org.autumn.modules.usr.entity.UserProfileEntity;

/**
 * 出站前回填头像展示 URL（有 {@code iconUuid} 时签发签名 URL 写入内存中的 {@code icon}，不落库）。
 * 由业务模块（如 account）提供实现；缺省无 Bean 时 OAuth 等路径保持原 {@code icon}。
 */
public interface UserProfileIconFiller {

    void fillDisplayIcon(UserProfileEntity profile);
}
