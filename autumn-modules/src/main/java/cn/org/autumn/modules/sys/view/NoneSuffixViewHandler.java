package cn.org.autumn.modules.sys.view;

import cn.org.autumn.config.ViewHandler;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 如果视图名以.js结尾，在直接返回视图前缀加视图名，不需要添加后缀
 * 当js文件需要通过Controller的RequestMapping进行输出时，特别有用
 * 一般是用于在后台动态处理js文件中的信息，特别是在多语言的时候
 */
@Component
public class NoneSuffixViewHandler implements ViewHandler, LoopJob.TenMinute {

    @Autowired
    SysConfigService sysConfigService;
    String[] suffixes = null;

    @Override
    public boolean should(String viewName) {
        if (!viewName.contains("."))
            return false;
        if (null == suffixes)
            suffixes = sysConfigService.getNoneSuffix();
        if (null != suffixes) {
            for (String no : suffixes) {
                boolean is = viewName.toLowerCase().endsWith("." + no);
                if (is)
                    return true;
            }
        }
        return false;
    }

    @Override
    public String getUrl(String prefix, String viewName, String suffix) {
        return prefix + viewName;
    }

    @Override
    public void onTenMinute() {
        suffixes = null;
    }
}