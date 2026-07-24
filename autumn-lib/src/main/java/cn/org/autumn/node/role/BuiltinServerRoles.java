package cn.org.autumn.node.role;

import cn.org.autumn.site.LoadFactory;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 预置通用服务器角色（只注册元数据，不写 Profile.roles）。
 */
@Component
public class BuiltinServerRoles implements LoadFactory.Must {

    private final ServerRoleRegistry registry;

    public BuiltinServerRoles(ServerRoleRegistry registry) {
        this.registry = registry;
    }

    @Override
    @Order(5)
    public void must() {
        registry.register(new ServerRole(ServerRole.CODE_ALL, "全部", "接收所有访问与能力（默认全开）",
                Set.of(ServerRole.CAP_ALL), 0));
        registry.register(new ServerRole(ServerRole.CODE_WEB, "网站", "仅网站网页访问",
                Set.of(ServerRole.CAP_WEB_UI), 10));
        registry.register(new ServerRole(ServerRole.CODE_API, "接口", "仅 API 调用与文件下载",
                Set.of(ServerRole.CAP_API_HTTP, ServerRole.CAP_FILE_DOWNLOAD), 20));
        registry.register(new ServerRole(ServerRole.CODE_WORKER, "后台", "后台静默工作",
                Set.of(ServerRole.CAP_BACKGROUND), 30));
        registry.register(new ServerRole(ServerRole.CODE_JOB, "任务", "定时任务服务器",
                Set.of(ServerRole.CAP_SCHEDULED_JOB), 40));
        registry.register(new ServerRole(ServerRole.CODE_MONITOR, "监视", "监视与观测",
                Set.of(ServerRole.CAP_MONITOR), 50));
    }
}
