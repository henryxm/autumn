package cn.org.autumn.model;

/**
 * 按业务 uuid 判定账号状态（无 Spring 依赖，由各模块实现）。
 */
public interface UserAccountProbe {

    UserAccountState resolve(String uuid);
}
