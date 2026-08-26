package cn.org.autumn.model;

/**
 * 系统账号解析结果，与 {@link cn.org.autumn.modules.sys.service.UserContextService#getUserContext(String)} 语义对齐并细分禁用/注销。
 */
public enum UserAccountState {
    ACTIVE,
    DISABLED,
    CANCELLED,
    NOT_FOUND,
    ROBOT_ACTIVE
}
