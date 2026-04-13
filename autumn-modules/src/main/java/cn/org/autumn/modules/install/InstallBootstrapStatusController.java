package cn.org.autumn.modules.install;

import cn.org.autumn.config.ApplicationInitializationProgress;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 与 {@code autumn.install.mode} 解耦，供安装完成页在进程内重启后轮询启动进度。
 */
@RestController
public class InstallBootstrapStatusController {

    private final ApplicationInitializationProgress progress;
    private final Environment environment;

    public InstallBootstrapStatusController(ApplicationInitializationProgress progress, Environment environment) {
        this.progress = progress;
        this.environment = environment;
    }

    @GetMapping("/install/bootstrap-status")
    public Map<String, Object> status() {
        boolean wizardActive = Boolean.parseBoolean(environment.getProperty(InstallConstants.INSTALL_MODE, "false"));
        ApplicationInitializationProgress.Phase phase = progress.getPhase();
        boolean done = phase == ApplicationInitializationProgress.Phase.DONE;
        boolean failed = phase == ApplicationInitializationProgress.Phase.FAILED;
        boolean initializing = !done && !failed;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("wizardActive", wizardActive);
        out.put("phase", phase.name());
        out.put("phaseLabel", progress.getMessage());
        out.put("percent", progress.getPercentForPhase());
        out.put("initializing", initializing);
        out.put("done", done);
        out.put("failed", failed);
        if (failed) {
            out.put("error", progress.getFailedDetail());
        } else {
            out.put("error", null);
        }
        return out;
    }
}
