package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(UsingHandler.class)
public interface UsingHandler {
    /**
     * 检查给定的对象或者字符串，是否在使用中，通常用于判断一个ID值或者一个文件是否还是使用，如果在使用中，则返回true,否则返回false
     *
     * @param value 给定一个对象或者字符串
     * @return 如果在使用中，返回true，否则返回false
     */
    boolean using(Object value);
}