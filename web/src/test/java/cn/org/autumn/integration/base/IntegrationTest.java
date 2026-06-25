package cn.org.autumn.integration.base;

import cn.org.autumn.Web;
import cn.org.autumn.integration.support.IntegrationAssumptions;
import cn.org.autumn.integration.support.RobotApiClient;
import cn.org.autumn.model.RobotQuotaConfig;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import cn.org.autumn.modules.usr.service.UserTokenService;
import cn.org.autumn.site.InitFactory;
import com.google.gson.Gson;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

/**
 * web 模块集成测试基类：拉起完整 Spring Boot（{@link Web}），H2 内存库，Redis 由环境提供。
 * <p>
 * {@link TestInstance.Lifecycle#PER_CLASS}：同类用例共享一次启动与管理员令牌，缩短套件时间。
 */
@SpringBootTest(classes = Web.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected InitFactory initFactory;

    @Autowired
    protected SysUserService sysUserService;

    @Autowired
    protected UserTokenService userTokenService;

    @Autowired
    protected SysConfigService sysConfigService;

    @Autowired
    protected Gson gson;

    @Autowired(required = false)
    protected RedisConnectionFactory redisConnectionFactory;

    protected RobotApiClient robotApi;

    protected String adminUuid;

    protected String userToken;

    @BeforeAll
    void autumnIntegrationInitOnce() {
        IntegrationAssumptions.requireRedis(redisConnectionFactory);
        awaitInitialization();
        relaxRobotQuotaForTests();
        SysUserEntity admin = sysUserService.getByUsername(sysUserService.getAdmin());
        if (admin == null)
            throw new IllegalStateException("初始化未完成：未找到管理员用户");
        adminUuid = admin.getUuid();
        UserTokenEntity tokenEntity = userTokenService.createToken(adminUuid);
        userToken = tokenEntity.getToken();
        robotApi = new RobotApiClient(restTemplate, "http://127.0.0.1:" + port);
        onReadyOnce();
    }

    /**
     * 子类在共享上下文就绪后执行一次（非每个 @Test）。
     */
    protected void onReadyOnce() {
    }

    private void relaxRobotQuotaForTests() {
        RobotQuotaConfig config = new RobotQuotaConfig();
        config.setMaxRobotsPerUser(1000);
        config.setMaxTokensPerRobot(100);
        config.setMaxHooksPerRobot(100);
        config.setMaxMessagePushPerMinute(10000);
        sysConfigService.updateValueByKey(RobotQuotaConfig.CONFIG_KEY, gson.toJson(config));
    }

    private void awaitInitialization() {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(120);
        while (System.currentTimeMillis() < deadline) {
            if (initFactory.isDone())
                return;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待 InitFactory 被中断", e);
            }
        }
        throw new IllegalStateException("InitFactory 在 120s 内未完成初始化");
    }
}
