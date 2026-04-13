package cn.org.autumn.config;

import cn.org.autumn.site.InitFactory;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.site.RefreshFactory;
import cn.org.autumn.site.UpgradeFactory;
import cn.org.autumn.utils.IPUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
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

    @Autowired
    Environment environment;

    @Autowired
    ApplicationInitializationProgress initializationProgress;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (Boolean.parseBoolean(environment.getProperty("autumn.install.mode", "false"))) {
            initializationProgress.enterWizardWaiting();
            log.info("Please visit install wizard: {}", buildInstallWizardUrl());
            return;
        }
        long start = System.currentTimeMillis();
        initializationProgress.beginInitialization();
        try {
            log.info("Start application initializing.");
            initFactory.init();
            initializationProgress.enterLoad();
            loadFactory.load();
            initializationProgress.enterUpgrade();
            upgradeFactory.upgrade();
            initializationProgress.enterRefresh();
            refreshFactory.refresh();
            initializationProgress.markDone();
        } catch (Exception e) {
            log.error("Application initializing with error:{}", e.getMessage(), e);
            initializationProgress.markFailed(e);
        } finally {
            long end = System.currentTimeMillis();
            float time = (float) (end - start) / 1000;
            log.info("Application initialized in {} seconds.", time);
        }
    }

    private String buildInstallWizardUrl() {
        String host = IPUtils.getIp();
        int port = environment.getProperty("server.port", Integer.class, 8080);
        StringBuilder sb = new StringBuilder("http://").append(host);
        if (port != 80 && port != 443) {
            sb.append(':').append(port);
        }
        String cp = StringUtils.trimToEmpty(environment.getProperty("server.servlet.context-path"));
        if (StringUtils.isNotBlank(cp) && !"/".equals(cp)) {
            if (!cp.startsWith("/")) {
                sb.append('/');
            }
            sb.append(cp);
        }
        return sb.toString();
    }
}
