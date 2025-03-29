package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(TableHandler.class)
public interface TableHandler {
    /**
     * 系统运行时，重新初始化数据表的接口
     * 实现类需自己实现重新初始化的方法，系统通过顶层接口统一调用初始化
     */
    void reinit();
}
