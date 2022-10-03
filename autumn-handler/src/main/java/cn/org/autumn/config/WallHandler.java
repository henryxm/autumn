package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(WallHandler.class)
@Order(Integer.MAX_VALUE / 1000)
public interface WallHandler {

    //防火墙总开关
    default boolean isOpen() {
        return true;
    }

    //是否开启IP白名单
    default boolean isIpWhiteEnable() {
        return true;
    }

    default boolean isIpBlackEnable() {
        return true;
    }

    default boolean isHostEnable() {
        return true;
    }

    default boolean isUrlBlackEnable() {
        return true;
    }

    default boolean isVisitEnable() {
        return true;
    }
}
