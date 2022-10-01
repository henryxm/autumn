package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(InitHandler.class)
public interface InitHandler {

    //一票否决制，只要有一个实现类，返回false,即不执行
    default boolean canBefore() {
        return true;
    }

    default boolean canInit() {
        return true;
    }

    default boolean canAfter() {
        return true;
    }

    default boolean canPost() {
        return true;
    }
}