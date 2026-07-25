package cn.org.autumn.modules.node;

import cn.org.autumn.annotation.JobMeta;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.node.Registry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Registry 心跳（启用条件：显式 {@code autumn.node.registry}，或未配置时跟随 {@code autumn.redis.open}）。
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
