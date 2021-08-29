package cn.org.autumn.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ViewHandler.class)
@Order(Integer.MAX_VALUE / 1000)
public interface ViewHandler {
    /**
     * 判断是否需要根据视图名字重新定义URL
     *
     * @param viewName 视图名字
     * @return 是否
     */
    boolean should(String viewName);

    /**
     * 根据视图名字判断是否需要修改获取视图的后缀
     * 如果FreeMarker定义的缺省后缀不能满足需要的时候，可以动态实现根据名称修改后缀的需求
     *
     * @param prefix   前缀
     * @param viewName 视图名
     * @param suffix   后缀
     * @return url
     */
    String getUrl(String prefix, String viewName, String suffix);
}