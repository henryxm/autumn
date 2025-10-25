package cn.org.autumn.config;

import cn.org.autumn.site.InitFactory;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.site.RefreshFactory;
import cn.org.autumn.site.UpgradeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author henryxm
 */
@Component
public class PostApplicationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PostApplicationRunner.class);

    @Autowired
    LoadFactory loadFactory;

    @Autowired
    InitFactory initFactory;

    @Autowired
    UpgradeFactory upgradeFactory;

    @Autowired
    RefreshFactory refreshFactory;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long start = System.currentTimeMillis();
        try {
            log.info("Start application initializing.");
            initFactory.init();
            loadFactory.load();
            upgradeFactory.upgrade();
            refreshFactory.refresh();
        } catch (Exception e) {
            log.error("Application initializing with error:{}", e.getMessage());
        } finally {
            long end = System.currentTimeMillis();
            float time = (float) (end - start) / 1000;
            log.info("Application initialized in {} seconds.", time);
        }
    }
}
