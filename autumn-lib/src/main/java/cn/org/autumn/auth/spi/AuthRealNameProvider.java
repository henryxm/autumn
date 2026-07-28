package cn.org.autumn.auth.spi;

import cn.org.autumn.auth.model.AuthRealNameInfo;

/**
 * 实名详情供给 SPI：由业务仓实现；无 Bean 时 {@code /oauth2/realName} 返回 unsupported。
 */
public interface AuthRealNameProvider {

    /**
     * 按用户 uuid 返回已通过实名认证的全量详情；未实名或无数据时返回 {@code null}。
     * 字段裁剪由框架按 granted scope 在出口执行。
     */
    AuthRealNameInfo getRealName(String userUuid);
}
