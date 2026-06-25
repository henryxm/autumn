package cn.org.autumn.service;

import cn.org.autumn.handler.MessageHandler;
import cn.org.autumn.redis.resilience.RedisResilience;
import cn.org.autumn.utils.RedisUtils;
import cn.org.autumn.utils.Uuid;
import com.google.gson.Gson;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

/**
 * Redis Pub/Sub 消息服务实现
 *
 * @author Autumn
 */
@Slf4j
@Service
public class RedisListenerService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private RedisProperties redisProperties;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private Gson gson;

    @Autowired(required = false)
    private RedisResilience redisResilience;

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
    private boolean initialized = false;

    /**
     * Redis连接测试重试次数
     */
    private int retryCount = 0;

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 3;

    @PostConstruct
    public void init() {
        if (initialized) {
            return;
        }
        if (redisUtils.isOpen() && redisTemplate != null)
            new Thread(this::initListener).start();
    }

    /**
     * 初始化消息监听容器
     */
    void initListener() {
        if (initialized || !redisUtils.isOpen() || redisTemplate == null) {
            return;
        }
        // 先测试Redis连接是否可用
        if (!ping()) {
            retryCount++;
            if (retryCount > MAX_RETRY_COUNT) {
                log.error("Redis connection test failed after {} retries, abandoning Pub/Sub init", MAX_RETRY_COUNT);
                if (redisResilience != null) {
                    redisResilience.recordFailure();
                }
                return;
            }
            log.warn("Redis connection test failed, retrying Pub/Sub init in 5s ({}/{})", retryCount, MAX_RETRY_COUNT);
            asyncTaskExecutor.execute(() -> {
                try {
                    Thread.sleep(10000);
                    initListener();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Delayed retry interrupted");
                }
            });
            return;
        }
        // 连接成功，重置重试计数
        retryCount = 0;
        try {
            // 创建专用的 Lettuce 连接工厂（Pub/Sub更稳定）
            RedisConnectionFactory connectionFactory = create();
            if (connectionFactory == null) {
                log.warn("Cannot create Lettuce connection factory, trying default");
                connectionFactory = redisTemplate.getConnectionFactory();
                if (connectionFactory == null) {
                    log.warn("Redis connection factory null, cannot start Pub/Sub");
                    return;
                }
            }
            if (log.isDebugEnabled())
                log.debug("Initializing Redis Pub/Sub, connection factory type: {}", connectionFactory.getClass().getName());
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
            if (redisResilience != null) {
                redisResilience.recordSuccess();
            }
        } catch (Exception e) {
            log.error("Redis Pub/Sub init failed: {}", e.getMessage(), e);
            if (redisResilience != null && RedisResilience.isInfrastructureFailure(e)) {
                redisResilience.recordFailure();
            }
            cleanup();
        }
    }

    /**
     * 创建专用的 Lettuce 连接工厂用于 Pub/Sub
     */
    private LettuceConnectionFactory create() {
        try {
            if (redisProperties == null) {
                return null;
            }
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
            log.error("Creation failed: {}", e.getMessage());
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
                        log.debug("Redis connection test succeeded");
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Redis connection test failed: {}", e.getMessage());
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
                    log.debug("Redis Pub/Sub not initialized, cannot subscribe channel: {}", channel);
                return false;
            }
            // 添加消息监听器
            messageListenerContainer.addMessageListener((message, pattern) -> {
                try {
                    String messageBody = new String(message.getBody());
                    if (log.isDebugEnabled())
                        log.debug("Subscription received: {}", messageBody);
                    handler.handle(channel, messageBody);
                } catch (Exception e) {
                    log.error("Failed to handle message on channel {}: {}", channel, e.getMessage(), e);
                }
            }, new ChannelTopic(channel));
            subscribedChannels.put(channel, handler);
            if (log.isDebugEnabled())
                log.debug("Subscribed channel: {}", channel);
            return true;
        } catch (Exception e) {
            log.error("Failed to subscribe channel {}: {}", channel, e.getMessage(), e);
            return false;
        }
    }

    public boolean unsubscribe(String channel) {
        if (channel == null) {
            return false;
        }
        if (!subscribedChannels.containsKey(channel)) {
            if (log.isDebugEnabled())
                log.debug("Channel {} not subscribed", channel);
            return false;
        }
        try {
            // 注意：RedisMessageListenerContainer 没有直接移除单个监听器的方法
            // 这里只是从本地记录中移除，实际订阅仍然存在
            // 如果需要完全移除，需要重新创建容器或使用其他方式
            subscribedChannels.remove(channel);
            if (log.isDebugEnabled())
                log.debug("Unsubscribed: {}", channel);
            return true;
        } catch (Exception e) {
            log.error("Unsubscribe failed: {}, error: {}", channel, e.getMessage());
            return false;
        }
    }

    public boolean publish(String channel, Object message) {
        if (channel == null || message == null) {
            return false;
        }
        if (!initialized) {
            if (log.isDebugEnabled())
                log.debug("Redis disabled, cannot publish to channel: {}", channel);
            return false;
        }
        try {
            String messageJson = gson.toJson(message);
            // 使用原生Redis连接发送字符串消息，避免被JdkSerializationRedisSerializer再次序列化
            redisTemplate.execute((RedisCallback<Long>) connection -> {
                connection.publish(channel.getBytes(), messageJson.getBytes());
                return 1L;
            });
            if (log.isDebugEnabled()) {
                log.debug("Published message: {}, content: {}", channel, messageJson);
            }
            return true;
        } catch (Exception e) {
            log.error("Publish message failed: {}, error: {}", channel, e.getMessage(), e);
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
     * 清理资源
     */
    private void cleanup() {
        if (messageListenerContainer != null) {
            try {
                messageListenerContainer.stop();
                messageListenerContainer.destroy();
            } catch (Exception e) {
                if (log.isDebugEnabled())
                    log.debug("Error cleaning up Redis Pub/Sub resources: {}", e.getMessage());
            }
            messageListenerContainer = null;
        }
        subscribedChannels.clear();
        initialized = false;
        retryCount = 0; // 重置重试计数
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
