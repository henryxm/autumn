package cn.org.autumn.view;

import cn.org.autumn.config.ViewHandler;
import org.springframework.stereotype.Component;

/**
 * 如果视图名以.js结尾，在直接返回视图前缀加视图名，不需要添加后缀
 * 当js文件需要通过Controller的RequestMapping进行输出时，特别有用
 * 一般是用于在后台动态处理js文件中的信息，特别是在多语言的时候
 */
@Component
public class JsViewHandler implements ViewHandler {
    @Override
    public boolean should(String viewName) {
        return viewName.endsWith(".js");
    }

    @Override
    public String getUrl(String prefix, String viewName, String suffix) {
        return prefix + viewName;
    }
}