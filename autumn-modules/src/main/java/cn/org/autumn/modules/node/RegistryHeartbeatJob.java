package cn.org.autumn.modules.node;

import cn.org.autumn.annotation.JobMeta;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.node.Registry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Registry 心跳（仅当 {@code autumn.node.registry=true} 时实质上报）。
 */
@Component
@JobMeta(name = "节点登记心跳", duty = cn.org.autumn.job.JobDuty.ALL, skipIfRunning = true)
public class RegistryHeartbeatJob implements LoopJob.OneMinute {

    private final ObjectProvider<Registry> registryProvider;

    public RegistryHeartbeatJob(ObjectProvider<Registry> registryProvider) {
        this.registryProvider = registryProvider;
    }

    @Override
    public void onOneMinute() {
        Registry registry = registryProvider.getIfAvailable();
        if (registry != null) {
            registry.beat();
        }
    }
}
