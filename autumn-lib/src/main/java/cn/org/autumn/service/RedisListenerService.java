package cn.org.autumn.service;

import cn.org.autumn.handler.MessageHandler;
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.utils.RedisUtils;
import cn.org.autumn.utils.Uuid;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Redis Pub/Sub 消息服务实现
 *
 * @author Autumn
 */
@Slf4j
@Service
public class RedisListenerService implements InitFactory.Init {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisProperties redisProperties;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private Gson gson;

    /**
     * 实例ID，用于标识当前实例
     */
    private final String instanceId = Uuid.uuid();

    /**
     * Redis消息监听容器
     */
    private RedisMessageListenerContainer messageListenerContainer;

    /**
     * 已订阅的频道和对应的处理器
     */
    private final Map<String, MessageHandler> subscribedChannels = new ConcurrentHashMap<>();

    /**
     * 标记是否已初始化监听器
     */
    private volatile boolean initialized = false;

    @Override
    public void init() {
        if (initialized) {
            return;
        }
        asyncTaskExecutor.execute(this::initMessageListenerContainer);
    }

    /**
     * 初始化消息监听容器
     */
    private void initMessageListenerContainer() {
        if (!isEnabled() || redisTemplate == null) {
            log.info("Redis未启用或RedisTemplate为空，跳过Redis Pub/Sub服务初始化");
            return;
        }
        if (initialized) {
            return;
        }
        // 先测试Redis连接是否可用
        if (!ping()) {
            log.warn("Redis连接测试失败，延迟5秒后重试初始化Redis Pub/Sub服务");
            asyncTaskExecutor.execute(() -> {
                try {
                    Thread.sleep(5000);
                    initMessageListenerContainer();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("延迟重试被中断");
                }
            });
            return;
        }
        try {
            // 创建专用的 Lettuce 连接工厂（Pub/Sub更稳定）
            RedisConnectionFactory connectionFactory = create();
            if (connectionFactory == null) {
                log.warn("无法创建Lettuce连接工厂，尝试使用默认连接工厂");
                connectionFactory = redisTemplate.getConnectionFactory();
                if (connectionFactory == null) {
                    log.warn("Redis连接工厂为空，无法启动Redis Pub/Sub服务");
                    return;
                }
            }
            if (log.isDebugEnabled())
                log.debug("开始初始化Redis Pub/Sub服务，连接工厂类型: {}", connectionFactory.getClass().getName());
            // 创建消息监听容器
            messageListenerContainer = new RedisMessageListenerContainer();
            messageListenerContainer.setConnectionFactory(connectionFactory);
            messageListenerContainer.setMaxSubscriptionRegistrationWaitingTime(60000);
            // 使用独立的线程池执行订阅操作
            messageListenerContainer.setSubscriptionExecutor(
                    Executors.newFixedThreadPool(1, r -> {
                        Thread t = new Thread(r, "redis-pubsub-subscription");
                        t.setDaemon(true);
                        return t;
                    })
            );
            // 设置任务执行器（用于处理消息）
            messageListenerContainer.setTaskExecutor(
                    Executors.newFixedThreadPool(2, r -> {
                        Thread t = new Thread(r, "redis-pubsub-handler");
                        t.setDaemon(true);
                        return t;
                    })
            );
            // 初始化并启动容器
            messageListenerContainer.afterPropertiesSet();
            messageListenerContainer.start();
            initialized = true;
            log.info("Redis Pub/Sub started success");
        } catch (Exception e) {
            log.error("初始化Redis Pub/Sub服务失败: {}", e.getMessage(), e);
            cleanup();
        }
    }

    /**
     * 创建专用的 Lettuce 连接工厂用于 Pub/Sub
     */
    private LettuceConnectionFactory create() {
        try {
            RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());
            standalone.setDatabase(redisProperties.getDatabase());
            if (redisProperties.getPassword() != null) {
                standalone.setPassword(redisProperties.getPassword());
            }
            LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder().build();
            LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientConfiguration);
            factory.afterPropertiesSet();
            return factory;
        } catch (Exception e) {
            log.error("创建失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 测试Redis连接是否可用
     */
    private boolean ping() {
        try {
            if (redisTemplate == null) {
                return false;
            }
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return false;
            }
            try (RedisConnection connection = connectionFactory.getConnection()) {
                String result = connection.ping();
                if ("PONG".equals(result)) {
                    if (log.isDebugEnabled())
                        log.debug("Redis连接测试成功");
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Redis连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    public boolean subscribe(String channel, MessageHandler handler) {
        try {
            if (channel == null || handler == null) {
                return false;
            }
            if (!initialized) {
                if (log.isDebugEnabled())
                    log.debug("Redis Pub/Sub服务未初始化，无法订阅频道: {}", channel);
                return false;
            }
            // 添加消息监听器
            messageListenerContainer.addMessageListener((message, pattern) -> {
                try {
                    String messageBody = new String(message.getBody());
                    handler.handle(channel, messageBody);
                } catch (Exception e) {
                    log.error("处理频道 {} 的消息失败: {}", channel, e.getMessage(), e);
                }
            }, new ChannelTopic(channel));
            subscribedChannels.put(channel, handler);
            if (log.isDebugEnabled())
                log.debug("订阅频道: {}", channel);
            return true;
        } catch (Exception e) {
            log.error("订阅频道 {} 失败: {}", channel, e.getMessage(), e);
            return false;
        }
    }

    public boolean unsubscribe(String channel) {
        if (channel == null) {
            return false;
        }
        if (!subscribedChannels.containsKey(channel)) {
            if (log.isDebugEnabled())
                log.debug("频道 {} 未订阅", channel);
            return false;
        }
        try {
            // 注意：RedisMessageListenerContainer 没有直接移除单个监听器的方法
            // 这里只是从本地记录中移除，实际订阅仍然存在
            // 如果需要完全移除，需要重新创建容器或使用其他方式
            subscribedChannels.remove(channel);
            if (log.isDebugEnabled())
                log.debug("取消订阅: {}", channel);
            return true;
        } catch (Exception e) {
            log.error("取消订阅:{}, 失败:{}", channel, e.getMessage());
            return false;
        }
    }

    public boolean publish(String channel, Object message) {
        if (channel == null || message == null) {
            return false;
        }
        if (!isEnabled() || redisTemplate == null) {
            if (log.isDebugEnabled())
                log.debug("Redis未启用，无法发布消息到频道: {}", channel);
            return false;
        }
        try {
            String messageJson = gson.toJson(message);
            redisTemplate.convertAndSend(channel, messageJson);
            if (log.isDebugEnabled()) {
                log.debug("发布消息到频道 {}: {}", channel, messageJson);
            }
            return true;
        } catch (Exception e) {
            log.error("发布消息到频道 {} 失败: {}", channel, e.getMessage(), e);
            return false;
        }
    }

    public boolean isSubscribed(String channel) {
        return subscribedChannels.containsKey(channel);
    }

    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 检查Redis是否启用
     */
    private boolean isEnabled() {
        return redisUtils != null && redisUtils.isOpen() && redisTemplate != null;
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        if (messageListenerContainer != null) {
            try {
                messageListenerContainer.stop();
                messageListenerContainer.destroy();
            } catch (Exception e) {
                if (log.isDebugEnabled())
                    log.debug("清理Redis Pub/Sub服务资源时出错: {}", e.getMessage());
            }
            messageListenerContainer = null;
        }
        subscribedChannels.clear();
        initialized = false;
    }

    /**
     * 销毁时停止消息监听器
     */
    @PreDestroy
    public void destroy() {
        cleanup();
        log.info("Redis Pub/Sub Service is stopped");
    }
}
