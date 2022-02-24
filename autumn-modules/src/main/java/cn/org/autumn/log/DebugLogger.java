package cn.org.autumn.log;

import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DebugLogger implements LoopJob.OneMinute {
    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    SysConfigService sysConfigService;

    private Boolean debug = null;

    public boolean debug() {
        if (null == debug)
            debug = sysConfigService.debug();
        return debug;
    }

    public void debug(String str) {
        if (log.isInfoEnabled() && debug())
            log.info(str);
    }

    public void debug(String str, Object obj) {
        if (log.isInfoEnabled() && debug())
            log.info(str, obj);
    }

    public void debug(String str, Object... objects) {
        if (log.isInfoEnabled() && debug())
            log.info(str, objects);
    }

    public void debug(String str, Throwable throwable) {
        if (log.isInfoEnabled() && debug())
            log.info(str, throwable);
    }

    @Override
    public void onOneMinute() {
        debug = null;
    }
}
