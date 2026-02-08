package cn.org.autumn.service;

import cn.org.autumn.config.QueueConfig;
import cn.org.autumn.model.QueueMessage;
import cn.org.autumn.thread.TagValue;
import cn.org.autumn.utils.RedisUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import cn.org.autumn.thread.TagRunnable;
import cn.org.autumn.thread.TagTaskExecutor;
import org.springframework.context.annotation.Lazy;

import javax.annotation.PreDestroy;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 通用队列服务
 * 提供多种队列实现方式，支持内存队列、Redis队列、延迟队列、优先级队列等
 */
@Slf4j
@Component
public class QueueService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private RedisUtils redisUtils;

    @Autowired
    private Gson gson;

    /**
     * 统一线程池 — 消费者线程通过 TagTaskExecutor 执行，纳入线程管理体系。
     * <p>消费者任务会显示在线程池管理界面，支持查看状态、堆栈和中断操作。</p>
     */
    @Autowired
    @Lazy
    private TagTaskExecutor tagTaskExecutor;

    /**
     * 内存队列容器
     */
    private final Map<String, BlockingQueue<QueueMessage<?>>> queues = new ConcurrentHashMap<>();

    /**
     * 优先级队列容器
     */
    private final Map<String, PriorityBlockingQueue<QueueMessage<?>>> priorities = new ConcurrentHashMap<>();

    /**
     * 队列配置容器
     */
    private final Map<String, QueueConfig> configs = new ConcurrentHashMap<>();

    /**
     * 空闲检测调度器 — 轻量级单线程，仅用于定期检查消费者是否空闲超时。
     * <p>业务线程（消费者）已交由 {@link TagTaskExecutor} 管理，此调度器只负责定时触发检查逻辑。</p>
     */
    private final ScheduledExecutorService idleCheckScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "queue-idle-checker");
        t.setDaemon(true);
        return t;
    });

    /**
     * 空闲检测任务 Future 映射 — 用于优雅取消定时检测（替代异常中断方式）
     */
    private final Map<String, ScheduledFuture<?>> idleCheckerFutures = new ConcurrentHashMap<>();

    /**
     * 消费者运行状态
     */
    private final Map<String, AtomicBoolean> running = new ConcurrentHashMap<>();

    /**
     * 消费者处理器映射（用于自动启动消费者）
     */
    private final Map<String, QueueConsumer<?>> handlers = new ConcurrentHashMap<>();

    /**
     * 最后一次消息处理时间（用于空闲检测）
     */
    private final Map<String, Long> last = new ConcurrentHashMap<>();

    /**
     * 消费者并发数映射
     */
    private final Map<String, Integer> concurrency = new ConcurrentHashMap<>();

    // ==================== 配置管理 ====================

    /**
     * 注册队列配置
     */
    public void register(QueueConfig config) {
        config.validate();
        configs.put(config.getName(), config);
        if (log.isDebugEnabled())
            log.debug("Queue registered: name={}, type={}, autoStart={}, autoStop={}", config.getName(), config.getQueueType(), config.isAuto(), config.isAuto());
    }

    /**
     * 注册队列配置和消费者处理器
     * 当 autoStart=true 时，发送消息会自动启动消费者
     *
     * @param config   队列配置
     * @param consumer 消费者处理器
     * @param <T>      消息类型
     */
    public <T> void register(QueueConfig config, QueueConsumer<T> consumer) {
        register(config);
        register(config.getName(), consumer, config.getConcurrency());
    }

    /**
     * 注册消费者处理器（不立即启动，仅用于自动启动）
     *
     * @param name        队列名称
     * @param consumer    消费者处理器
     * @param concurrency 并发数
     * @param <T>         消息类型
     */
    public <T> void register(String name, QueueConsumer<T> consumer, int concurrency) {
        handlers.put(name, consumer);
        this.concurrency.put(name, concurrency);
        if (log.isDebugEnabled())
            log.debug("Consumer handler registered: queue={}, concurrency={}", name, concurrency);
    }

    /**
     * 注册消费者处理器（单并发，不立即启动）
     *
     * @param name     队列名称
     * @param consumer 消费者处理器
     * @param <T>      消息类型
     */
    public <T> void register(String name, QueueConsumer<T> consumer) {
        register(name, consumer, 1);
    }

    /**
     * 获取队列配置
     */
    public QueueConfig getConfig(String name) {
        return configs.get(name);
    }

    /**
     * 获取或创建队列配置
     */
    public QueueConfig getConfig(String name, Class<?> type) {
        return configs.computeIfAbsent(name, k -> QueueConfig.builder().name(name).type(type).build());
    }

    // ==================== 生产者方法 ====================

    /**
     * 发送消息到队列（简单方式）
     *
     * @param name 队列名称
     * @param body 消息体
     * @param <T>  消息类型
     * @return 是否发送成功
     */
    public <T> boolean send(String name, T body) {
        return send(name, QueueMessage.of(body));
    }

    /**
     * 发送消息到队列
     *
     * @param name    队列名称
     * @param message 消息对象
     * @param <T>     消息类型
     * @return 是否发送成功
     */
    public <T> boolean send(String name, QueueMessage<T> message) {
        QueueConfig config = getConfig(name, message.getBody().getClass());
        return send(config, message);
    }

    /**
     * 发送消息到队列（使用配置）
     *
     * @param config  队列配置
     * @param message 消息对象
     * @param <T>     消息类型
     * @return 是否发送成功
     */
    public <T> boolean send(QueueConfig config, QueueMessage<T> message) {
        try {
            String name = config.getName();
            boolean success;
            switch (config.getQueueType()) {
                case MEMORY:
                    success = sendToMemoryQueue(name, message, config.getCapacity());
                    break;
                case PRIORITY:
                    success = sendToPriorityQueue(name, message);
                    break;
                case DELAY:
                    success = sendToDelayQueue(name, message);
                    break;
                case REDIS_LIST:
                    success = sendToRedisList(name, message);
                    break;
                case REDIS_STREAM:
                    success = sendToRedisStream(name, message);
                    break;
                default:
                    log.warn("Unknown queue type: {}", config.getQueueType());
                    return false;
            }
            // 发送成功后，检查是否需要自动启动消费者
            if (success && config.isAuto()) {
                tryStart(name, config);
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to send message to queue {}: {}", config.getName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 尝试自动启动消费者
     * 仅当消费者未运行且已注册处理器时才会启动
     *
     * @param name   队列名称
     * @param config 队列配置
     */
    @SuppressWarnings("unchecked")
    private void tryStart(String name, QueueConfig config) {
        AtomicBoolean running = this.running.get(name);
        // 只有当消费者未运行时才自动启动
        if (running == null || !running.get()) {
            QueueConsumer<?> handler = handlers.get(name);
            if (handler != null) {
                int concurrency = this.concurrency.getOrDefault(name, config.getConcurrency());
                if (log.isDebugEnabled())
                    log.debug("Auto starting consumer: queue={}, concurrency={}", name, concurrency);
                start(config, (QueueConsumer<Object>) handler, concurrency);
            }
        }
    }

    /**
     * 发送延迟消息
     *
     * @param name        队列名称
     * @param body        消息体
     * @param delayMillis 延迟毫秒数
     * @param <T>         消息类型
     * @return 是否发送成功
     */
    public <T> boolean sendDelay(String name, T body, long delayMillis) {
        return send(name, QueueMessage.ofDelay(body, delayMillis));
    }

    /**
     * 发送定时消息
     *
     * @param name      队列名称
     * @param body      消息体
     * @param executeAt 执行时间戳（毫秒）
     * @param <T>       消息类型
     * @return 是否发送成功
     */
    public <T> boolean sendScheduled(String name, T body, long executeAt) {
        return send(name, QueueMessage.ofScheduled(body, executeAt));
    }

    /**
     * 发送优先级消息
     *
     * @param name     队列名称
     * @param body     消息体
     * @param priority 优先级（数字越小优先级越高）
     * @param <T>      消息类型
     * @return 是否发送成功
     */
    public <T> boolean sendPriority(String name, T body, int priority) {
        return send(name, QueueMessage.ofPriority(body, priority));
    }

    /**
     * 批量发送消息
     *
     * @param name   队列名称
     * @param bodies 消息体列表
     * @param <T>    消息类型
     * @return 成功发送的数量
     */
    public <T> int sendBatch(String name, List<T> bodies) {
        int successCount = 0;
        for (T body : bodies) {
            if (send(name, body)) {
                successCount++;
            }
        }
        return successCount;
    }

    // ==================== 消费者方法 ====================

    /**
     * 消费单条消息（阻塞式）
     *
     * @param name 队列名称
     * @param <T>  消息类型
     * @return 消息对象，如果超时返回null
     */
    public <T> QueueMessage<T> poll(String name) {
        return poll(name, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * 消费单条消息（带超时）
     *
     * @param name    队列名称
     * @param timeout 超时时间
     * @param unit    时间单位
     * @param <T>     消息类型
     * @return 消息对象，如果超时返回null
     */
    public <T> QueueMessage<T> poll(String name, long timeout, TimeUnit unit) {
        QueueConfig config = getConfig(name, Object.class);
        try {
            switch (config.getQueueType()) {
                case MEMORY:
                    return pollFromMemoryQueue(name, timeout, unit);
                case PRIORITY:
                    return pollFromPriorityQueue(name, timeout, unit);
                case DELAY:
                    return pollFromDelayQueue(name, timeout, unit);
                case REDIS_LIST:
                    return pollFromRedisList(name, timeout, unit);
                case REDIS_STREAM:
                    return pollFromRedisStream(config, timeout, unit);
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("Failed to poll message from queue {}: {}", name, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 批量消费消息
     *
     * @param name     队列名称
     * @param maxCount 最大消费数量
     * @param <T>      消息类型
     * @return 消息列表
     */
    public <T> List<QueueMessage<T>> pollBatch(String name, int maxCount) {
        List<QueueMessage<T>> messages = new ArrayList<>();
        for (int i = 0; i < maxCount; i++) {
            QueueMessage<T> message = poll(name);
            if (message == null) {
                break;
            }
            messages.add(message);
        }
        return messages;
    }

    /**
     * 启动消费者（异步消费）
     *
     * @param name     队列名称
     * @param consumer 消费者
     * @param <T>      消息类型
     */
    public <T> void start(String name, QueueConsumer<T> consumer) {
        QueueConfig config = getConfig(name, Object.class);
        start(config, consumer, 1);
    }

    /**
     * 启动消费者（异步消费，指定并发数）
     *
     * @param name        队列名称
     * @param consumer    消费者
     * @param concurrency 并发消费者数量
     * @param <T>         消息类型
     */
    public <T> void start(String name, QueueConsumer<T> consumer, int concurrency) {
        QueueConfig config = getConfig(name, Object.class);
        start(config, consumer, concurrency);
    }

    /**
     * 启动消费者（支持空闲自动停止）
     * <p>消费者线程通过 {@link TagTaskExecutor} 执行，在线程池管理界面中可查看、监控和中断。</p>
     *
     * @param config      队列配置
     * @param consumer    消费者
     * @param concurrency 并发消费者数量
     * @param <T>         消息类型
     */
    public <T> void start(QueueConfig config, QueueConsumer<T> consumer, int concurrency) {
        String name = config.getName();
        AtomicBoolean running = this.running.computeIfAbsent(name, k -> new AtomicBoolean(false));
        if (running.compareAndSet(false, true)) {
            // 初始化最后消息时间
            last.put(name, System.currentTimeMillis());
            if (config.isAuto())
                // 启动空闲检测器
                checker(name, config);
            for (int i = 0; i < concurrency; i++) {
                final int consumerId = i;
                // 使用 TagRunnable 包装消费者循环，纳入 TagTaskExecutor 统一管理
                TagRunnable task = new TagRunnable() {
                    @Override
                    public boolean can() {
                        // 队列消费者不受系统启动状态限制 — 消费者只在有消息时才启动
                        return true;
                    }

                    @Override
                    protected boolean applyStaggerDelay() {
                        // 队列消费者无需错峰延迟 — 消费者应立即开始消费
                        return true;
                    }

                    @Override
                    @TagValue(type = QueueService.class, method = "start", tag = "队列任务处理")
                    public void exe() {
                        if (log.isDebugEnabled())
                            log.debug("Consumer started: queue={}, consumerId={}, idleTimeout={}{}", name, consumerId, config.getIdleTime(), config.getIdleUnit().name().toLowerCase());
                        while (running.get() && !isCancelled()) {
                            try {
                                QueueMessage<T> message = poll(name, config.getTimeout(), config.getUnit());
                                if (message != null) {
                                    // 更新最后消息处理时间
                                    last.put(name, System.currentTimeMillis());
                                    process(config, message, consumer);
                                }
                            } catch (Exception e) {
                                if (isCancelled()) {
                                    if (log.isDebugEnabled())
                                        log.debug("Consumer interrupted: queue={}, consumerId={}", name, consumerId);
                                    break;
                                }
                                log.error("Consumer error: queue={}, consumerId={}, error={}", name, consumerId, e.getMessage(), e);
                            }
                        }
                        if (log.isDebugEnabled())
                            log.debug("Consumer stopped: queue={}, consumerId={}", name, consumerId);
                    }
                };
                // 设置任务元数据 — 在管理界面中识别消费者
                task.setTag("队列消费者");
                task.setName(name + "#" + consumerId);
                task.setMethod(name);
                task.setType(QueueService.class);
                tagTaskExecutor.execute(task);
            }
        } else {
            // 消费者已运行，只需更新最后消息时间（触发活跃状态）
            last.put(name, System.currentTimeMillis());
            if (log.isDebugEnabled())
                log.debug("Consumer already running, refreshed activity: queue={}", name);
        }
    }

    /**
     * 启动空闲检测器
     * <p>定期检查消费者是否空闲超时，如果超时则自动停止消费者。
     * 使用 {@link ScheduledFuture#cancel(boolean)} 优雅停止检测（替代异常中断方式）。</p>
     *
     * @param name   队列名称
     * @param config 队列配置
     */
    private void checker(String name, QueueConfig config) {
        // 取消已有的检测器（防止重复创建）
        cancelChecker(name);
        long idleTimeoutMillis = config.getIdleUnit().toMillis(config.getIdleTime());
        // 检查间隔为空闲超时的一半，最小1秒，最大30秒
        long checkIntervalMillis = Math.max(1000, Math.min(idleTimeoutMillis / 2, 30000));
        ScheduledFuture<?> future = idleCheckScheduler.scheduleAtFixedRate(() -> {
            try {
                AtomicBoolean running = this.running.get(name);
                if (running == null || !running.get()) {
                    // 消费者已停止，取消此检测器
                    cancelChecker(name);
                    return;
                }
                Long lastTime = last.get(name);
                if (lastTime == null) {
                    return;
                }
                long idleTime = System.currentTimeMillis() - lastTime;
                long queueSize = size(name);
                // 只有当队列为空且空闲时间超过阈值时才停止
                if (queueSize == 0 && idleTime > idleTimeoutMillis) {
                    if (log.isDebugEnabled())
                        log.debug("Consumer idle timeout, auto stopping: queue={}, idleTime={}ms, threshold={}ms", name, idleTime, idleTimeoutMillis);
                    stop(name);
                }
            } catch (Exception e) {
                log.error("Idle checker error: queue={}, error={}", name, e.getMessage(), e);
            }
        }, checkIntervalMillis, checkIntervalMillis, TimeUnit.MILLISECONDS);
        idleCheckerFutures.put(name, future);
    }

    /**
     * 取消指定队列的空闲检测器
     */
    private void cancelChecker(String name) {
        ScheduledFuture<?> future = idleCheckerFutures.remove(name);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 停止消费者
     * <p>设置运行标志为 false，消费者线程将在下次循环检查时优雅退出。
     * 同时取消该队列的空闲检测器。</p>
     *
     * @param name 队列名称
     */
    public void stop(String name) {
        AtomicBoolean running = this.running.get(name);
        if (running != null) {
            running.set(false);
            if (log.isDebugEnabled())
                log.debug("Consumer stop requested: queue={}", name);
        }
        // 取消空闲检测器
        cancelChecker(name);
    }

    /**
     * 使用 Supplier 模式消费（类似 CacheService 的 compute）
     * 如果队列中有消息则消费，否则使用 supplier 获取默认值
     *
     * @param name     队列名称
     * @param supplier 默认值提供者
     * @param <T>      消息类型
     * @return 消息体或默认值
     */
    public <T> T consume(String name, Supplier<T> supplier) {
        QueueMessage<T> message = poll(name);
        if (message != null) {
            return message.getBody();
        }
        return supplier.get();
    }

    /**
     * 使用 Consumer 模式处理消息
     * 如果队列中有消息则用 consumer 处理
     *
     * @param name     队列名称
     * @param consumer 消息处理器
     * @param <T>      消息类型
     * @return 是否处理了消息
     */
    public <T> boolean consume(String name, Consumer<T> consumer) {
        QueueMessage<T> message = poll(name);
        if (message != null) {
            consumer.accept(message.getBody());
            return true;
        }
        return false;
    }

    // ==================== 队列管理方法 ====================

    /**
     * 获取队列长度
     *
     * @param name 队列名称
     * @return 队列长度
     */
    public long size(String name) {
        QueueConfig config = configs.get(name);
        if (config == null) {
            BlockingQueue<?> memoryQueue = queues.get(name);
            if (memoryQueue != null) {
                return memoryQueue.size();
            }
            return 0;
        }

        switch (config.getQueueType()) {
            case MEMORY:
                BlockingQueue<?> queue = queues.get(name);
                return queue != null ? queue.size() : 0;
            case PRIORITY:
                PriorityBlockingQueue<?> pQueue = priorities.get(name);
                return pQueue != null ? pQueue.size() : 0;
            case DELAY:
            case REDIS_LIST:
                return getRedisListSize(name);
            case REDIS_STREAM:
                return getRedisStreamSize(name);
            default:
                return 0;
        }
    }

    /**
     * 清空队列
     *
     * @param name 队列名称
     */
    public void clear(String name) {
        QueueConfig config = configs.get(name);
        if (config == null) {
            BlockingQueue<?> memoryQueue = queues.get(name);
            if (memoryQueue != null) {
                memoryQueue.clear();
            }
            return;
        }

        switch (config.getQueueType()) {
            case MEMORY:
                BlockingQueue<?> queue = queues.get(name);
                if (queue != null) {
                    queue.clear();
                }
                break;
            case PRIORITY:
                PriorityBlockingQueue<?> pQueue = priorities.get(name);
                if (pQueue != null) {
                    pQueue.clear();
                }
                break;
            case DELAY:
            case REDIS_LIST:
            case REDIS_STREAM:
                clearRedisQueue(name);
                break;
        }
        log.info("Queue cleared: {}", name);
    }

    /**
     * 检查队列是否为空
     *
     * @param name 队列名称
     * @return 是否为空
     */
    public boolean isEmpty(String name) {
        return size(name) == 0;
    }

    // ==================== 私有方法 - 内存队列 ====================

    @SuppressWarnings("unchecked")
    private <T> boolean sendToMemoryQueue(String name, QueueMessage<T> message, int capacity) {
        BlockingQueue<QueueMessage<?>> queue = queues.computeIfAbsent(name, k -> capacity > 0 ? new LinkedBlockingQueue<>(capacity) : new LinkedBlockingQueue<>());
        return queue.offer(message);
    }

    @SuppressWarnings("unchecked")
    private <T> QueueMessage<T> pollFromMemoryQueue(String name, long timeout, TimeUnit unit) throws InterruptedException {
        BlockingQueue<QueueMessage<?>> queue = queues.get(name);
        if (queue == null) {
            return null;
        }
        if (timeout <= 0) {
            return (QueueMessage<T>) queue.poll();
        }
        return (QueueMessage<T>) queue.poll(timeout, unit);
    }

    // ==================== 私有方法 - 优先级队列 ====================

    @SuppressWarnings("unchecked")
    private <T> boolean sendToPriorityQueue(String name, QueueMessage<T> message) {
        PriorityBlockingQueue<QueueMessage<?>> queue = priorities.computeIfAbsent(name, k -> new PriorityBlockingQueue<>(100, Comparator.comparingInt(QueueMessage::getPriority)));
        return queue.offer(message);
    }

    @SuppressWarnings("unchecked")
    private <T> QueueMessage<T> pollFromPriorityQueue(String name, long timeout, TimeUnit unit) throws InterruptedException {
        PriorityBlockingQueue<QueueMessage<?>> queue = priorities.get(name);
        if (queue == null) {
            return null;
        }
        if (timeout <= 0) {
            return (QueueMessage<T>) queue.poll();
        }
        return (QueueMessage<T>) queue.poll(timeout, unit);
    }

    // ==================== 私有方法 - 延迟队列 ====================

    private <T> boolean sendToDelayQueue(String name, QueueMessage<T> message) {
        if (!isRedisEnabled()) {
            log.warn("Redis is not enabled, delay queue not available");
            return false;
        }
        try {
            String key = "queue:delay:" + name;
            double score = message.getDelay() != null ? message.getDelay() : System.currentTimeMillis();
            String json = gson.toJson(message);
            redisTemplate.opsForZSet().add(key, json, score);
            return true;
        } catch (Exception e) {
            log.error("Failed to send to delay queue: {}", e.getMessage(), e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> QueueMessage<T> pollFromDelayQueue(String name, long timeout, TimeUnit unit) {
        if (!isRedisEnabled()) {
            return null;
        }
        try {
            String key = "queue:delay:" + name;
            long now = System.currentTimeMillis();

            // 获取已到期的消息
            Set<Object> messages = redisTemplate.opsForZSet().rangeByScore(key, 0, now, 0, 1);
            if (messages == null || messages.isEmpty()) {
                if (timeout > 0) {
                    Thread.sleep(Math.min(unit.toMillis(timeout), 100));
                }
                return null;
            }

            Object first = messages.iterator().next();
            // 尝试原子移除
            Long removed = redisTemplate.opsForZSet().remove(key, first);
            if (removed != null && removed > 0) {
                Type type = new TypeToken<QueueMessage<T>>() {
                }.getType();
                return gson.fromJson(first.toString(), type);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to poll from delay queue: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 私有方法 - Redis List 队列 ====================

    private <T> boolean sendToRedisList(String name, QueueMessage<T> message) {
        if (!isRedisEnabled()) {
            log.warn("Redis is not enabled, falling back to memory queue");
            return sendToMemoryQueue(name, message, 10000);
        }
        try {
            String key = "queue:list:" + name;
            String json = gson.toJson(message);
            redisTemplate.opsForList().rightPush(key, json);
            return true;
        } catch (Exception e) {
            log.error("Failed to send to Redis list: {}", e.getMessage(), e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> QueueMessage<T> pollFromRedisList(String name, long timeout, TimeUnit unit) {
        if (!isRedisEnabled()) {
            try {
                return pollFromMemoryQueue(name, timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        try {
            String key = "queue:list:" + name;
            Object result;
            if (timeout > 0) {
                result = redisTemplate.opsForList().leftPop(key, timeout, unit);
            } else {
                result = redisTemplate.opsForList().leftPop(key);
            }
            if (result == null) {
                return null;
            }
            Type type = new TypeToken<QueueMessage<T>>() {
            }.getType();
            return gson.fromJson(result.toString(), type);
        } catch (Exception e) {
            log.error("Failed to poll from Redis list: {}", e.getMessage(), e);
            return null;
        }
    }

    private long getRedisListSize(String name) {
        if (!isRedisEnabled()) {
            return 0;
        }
        try {
            String key = "queue:list:" + name;
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get Redis list size: {}", e.getMessage(), e);
            return 0;
        }
    }

    // ==================== 私有方法 - Redis Stream 队列 ====================

    private <T> boolean sendToRedisStream(String name, QueueMessage<T> message) {
        if (!isRedisEnabled()) {
            log.warn("Redis is not enabled, falling back to memory queue");
            return sendToMemoryQueue(name, message, 10000);
        }
        try {
            String key = "queue:stream:" + name;
            Map<String, String> fields = new HashMap<>();
            fields.put("data", gson.toJson(message));
            fields.put("id", message.getId());
            fields.put("timestamp", String.valueOf(message.getTimestamp()));
            redisTemplate.opsForStream().add(key, fields);
            return true;
        } catch (Exception e) {
            log.error("Failed to send to Redis stream: {}", e.getMessage(), e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> QueueMessage<T> pollFromRedisStream(QueueConfig config, long timeout, TimeUnit unit) {
        if (!isRedisEnabled()) {
            try {
                return pollFromMemoryQueue(config.getName(), timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        try {
            String key = "queue:stream:" + config.getName();
            // 简化实现：使用 XREAD 而非消费者组
            List<?> records = redisTemplate.opsForStream().read(StreamOffset.fromStart(key));
            if (records == null || records.isEmpty()) {
                if (timeout > 0) {
                    Thread.sleep(Math.min(unit.toMillis(timeout), 100));
                }
                return null;
            }
            // 获取第一条记录并删除
            @SuppressWarnings("unchecked")
            MapRecord<String, Object, Object> record = (MapRecord<String, Object, Object>) records.get(0);
            redisTemplate.opsForStream().delete(key, record.getId());
            Object data = record.getValue().get("data");
            if (data != null) {
                Type type = new TypeToken<QueueMessage<T>>() {
                }.getType();
                return gson.fromJson(data.toString(), type);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to poll from Redis stream: {}", e.getMessage(), e);
            return null;
        }
    }

    private long getRedisStreamSize(String name) {
        if (!isRedisEnabled()) {
            return 0;
        }
        try {
            String key = "queue:stream:" + name;
            Long size = redisTemplate.opsForStream().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get Redis stream size: {}", e.getMessage(), e);
            return 0;
        }
    }

    private void clearRedisQueue(String name) {
        if (!isRedisEnabled()) {
            return;
        }
        try {
            redisTemplate.delete("queue:list:" + name);
            redisTemplate.delete("queue:stream:" + name);
            redisTemplate.delete("queue:delay:" + name);
        } catch (Exception e) {
            log.error("Failed to clear Redis queue: {}", e.getMessage(), e);
        }
    }

    // ==================== 私有方法 - 消息处理 ====================

    private <T> void process(QueueConfig config, QueueMessage<T> message, QueueConsumer<T> consumer) {
        try {
            boolean success = consumer.consume(message);
            if (!success) {
                failed(config, message, consumer, null);
            }
        } catch (Exception e) {
            log.error("Message processing failed: queue={}, messageId={}, error={}", config.getName(), message.getId(), e.getMessage(), e);
            consumer.onError(message, e);
            failed(config, message, consumer, e);
        }
    }

    private <T> void failed(QueueConfig config, QueueMessage<T> message, QueueConsumer<T> consumer, Throwable error) {
        message.incrementRetry();
        if (message.getRetry() < config.getRetries()) {
            // 重新入队
            log.warn("Message will be retried: queue={}, messageId={}, retryCount={}", config.getName(), message.getId(), message.getRetry());
            send(config, message);
        } else if (config.isDeadLetter()) {
            // 发送到死信队列
            log.warn("Message sent to dead letter queue: queue={}, messageId={}", config.getName(), message.getId());
            dead(config, message);
            consumer.onDead(message);
        } else {
            log.error("Message discarded after max retries: queue={}, messageId={}, error={}", config.getName(), message.getId(), error.getMessage());
        }
    }

    private <T> void dead(QueueConfig config, QueueMessage<T> message) {
        String deadName = config.getDeadName();
        QueueConfig deadConfig = QueueConfig.builder().name(deadName).type(config.getType()).queueType(config.getQueueType()).build();
        send(deadConfig, message);
    }

    // ==================== 管理方法 ====================

    /**
     * 获取所有队列信息
     */
    public List<Map<String, Object>> getAllQueueInfo() {
        List<Map<String, Object>> result = new ArrayList<>();
        // 添加已注册的配置队列
        for (Map.Entry<String, QueueConfig> entry : configs.entrySet()) {
            result.add(buildQueueInfo(entry.getKey(), entry.getValue()));
        }
        // 添加内存队列（未注册配置的）
        for (String name : queues.keySet()) {
            if (!configs.containsKey(name)) {
                result.add(buildQueueInfo(name, null));
            }
        }
        // 添加优先级队列（未注册配置的）
        for (String name : priorities.keySet()) {
            if (!configs.containsKey(name)) {
                Map<String, Object> info = buildQueueInfo(name, null);
                info.put("queueType", "PRIORITY");
                result.add(info);
            }
        }
        return result;
    }

    /**
     * 获取单个队列信息
     */
    public Map<String, Object> getQueueInfo(String name) {
        QueueConfig config = configs.get(name);
        if (config == null && !queues.containsKey(name) && !priorities.containsKey(name)) {
            return null;
        }
        return buildQueueInfo(name, config);
    }

    /**
     * 构建队列信息
     */
    private Map<String, Object> buildQueueInfo(String name, QueueConfig config) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", name);
        info.put("size", size(name));
        info.put("consumerRunning", isConsumerRunning(name));
        if (config != null) {
            info.put("queueType", config.getQueueType().name());
            info.put("capacity", config.getCapacity());
            info.put("maxRetries", config.getRetries());
            info.put("deadLetterEnabled", config.isDeadLetter());
            info.put("deadLetterQueue", config.getDeadName());
            info.put("timeout", config.getTimeout());
            info.put("timeoutUnit", config.getUnit().name());
            info.put("messageType", config.getType() != null ? config.getType().getSimpleName() : "Object");
        } else {
            info.put("queueType", "MEMORY");
            info.put("capacity", 0);
            info.put("maxRetries", 3);
            info.put("deadLetterEnabled", false);
            info.put("messageType", "Object");
        }
        // 死信队列大小
        if (config != null && config.isDeadLetter()) {
            info.put("deadLetterSize", size(config.getDeadName()));
        }
        return info;
    }

    /**
     * 检查消费者是否运行中
     */
    public boolean isConsumerRunning(String name) {
        AtomicBoolean running = this.running.get(name);
        return running != null && running.get();
    }

    /**
     * 预览消息（不消费）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> peekMessages(String name, int page, int limit) {
        Map<String, Object> result = new HashMap<>();
        List<Object> messages = new ArrayList<>();
        long total = 0;
        BlockingQueue<QueueMessage<?>> memoryQueue = queues.get(name);
        if (memoryQueue != null) {
            List<QueueMessage<?>> list = new ArrayList<>(memoryQueue);
            total = list.size();
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, list.size());
            if (start < list.size()) {
                messages.addAll(list.subList(start, end));
            }
        }
        PriorityBlockingQueue<QueueMessage<?>> priorityQueue = priorities.get(name);
        if (priorityQueue != null) {
            List<QueueMessage<?>> list = new ArrayList<>(priorityQueue);
            list.sort(Comparator.comparingInt(QueueMessage::getPriority));
            total = list.size();
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, list.size());
            if (start < list.size()) {
                messages.addAll(list.subList(start, end));
            }
        }
        // Redis队列预览
        if (isRedisEnabled() && messages.isEmpty()) {
            try {
                String listKey = "queue:list:" + name;
                Long listSize = redisTemplate.opsForList().size(listKey);
                if (listSize != null && listSize > 0) {
                    total = listSize;
                    int start = (page - 1) * limit;
                    int end = Math.min(start + limit, listSize.intValue()) - 1;
                    List<Object> redisMessages = redisTemplate.opsForList().range(listKey, start, end);
                    if (redisMessages != null) {
                        for (Object msg : redisMessages) {
                            try {
                                Type type = new TypeToken<QueueMessage<Object>>() {
                                }.getType();
                                messages.add(gson.fromJson(msg.toString(), type));
                            } catch (Exception e) {
                                messages.add(msg);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to peek Redis messages: {}", e.getMessage());
            }
        }
        result.put("list", messages);
        result.put("total", total);
        return result;
    }

    /**
     * 删除队列
     */
    public void delete(String name) {
        // 先停止消费者
        stop(name);
        // 清空队列
        clear(name);
        // 移除配置
        configs.remove(name);
        // 移除内存队列
        queues.remove(name);
        priorities.remove(name);
        if (log.isDebugEnabled())
            log.debug("Queue deleted: {}", name);
    }

    /**
     * 获取消费者状态列表
     */
    public List<Map<String, Object>> getConsumerStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, AtomicBoolean> entry : running.entrySet()) {
            Map<String, Object> status = new HashMap<>();
            status.put("queueName", entry.getKey());
            status.put("running", entry.getValue().get());
            status.put("queueSize", size(entry.getKey()));
            result.add(status);
        }
        return result;
    }

    /**
     * 启动简单消费者（用于管理页面测试）
     */
    public void start(String name, int concurrency) {
        QueueConfig config = getConfig(name, Object.class);
        start(config, message -> {
            log.info("Simple consumer processed message: queue={}, id={}, body={}", name, message.getId(), message.getBody());
            return true;
        }, concurrency);
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        // 队列总数
        Set<String> allQueues = new HashSet<>();
        allQueues.addAll(configs.keySet());
        allQueues.addAll(queues.keySet());
        allQueues.addAll(priorities.keySet());
        stats.put("totalQueues", allQueues.size());
        // 消息总数
        long totalMessages = 0;
        for (String name : allQueues) {
            totalMessages += size(name);
        }
        stats.put("totalMessages", totalMessages);
        // 运行中的消费者数
        long runningConsumers = running.values().stream().filter(AtomicBoolean::get).count();
        stats.put("runningConsumers", runningConsumers);
        // 队列类型统计
        Map<String, Integer> typeStats = new HashMap<>();
        for (QueueConfig config : configs.values()) {
            String type = config.getQueueType().name();
            typeStats.put(type, typeStats.getOrDefault(type, 0) + 1);
        }
        // 未注册配置的内存队列
        int memoryOnlyCount = 0;
        for (String name : queues.keySet()) {
            if (!configs.containsKey(name)) {
                memoryOnlyCount++;
            }
        }
        typeStats.put("MEMORY", typeStats.getOrDefault("MEMORY", 0) + memoryOnlyCount);
        stats.put("typeStats", typeStats);
        // Redis状态
        stats.put("redisEnabled", isRedisEnabled());
        return stats;
    }

    /**
     * 重试死信队列消息
     */
    public int retryDeadLetterMessages(String queueName) {
        String deadLetterName = queueName.endsWith(":dead") ? queueName : queueName + ":dead";
        String targetQueue = queueName.endsWith(":dead") ? queueName.replace(":dead", "") : queueName;
        int count = 0;
        int maxRetry = 100;
        while (count < maxRetry) {
            QueueMessage<?> message = poll(deadLetterName, 100, TimeUnit.MILLISECONDS);
            if (message == null) {
                break;
            }
            // 重置重试次数
            message.setRetry(0);
            send(targetQueue, message);
            count++;
        }
        return count;
    }

    /**
     * 移动消息到另一个队列
     */
    public int moveMessages(String sourceQueue, String targetQueue, int count) {
        int moved = 0;
        for (int i = 0; i < count; i++) {
            QueueMessage<?> message = poll(sourceQueue, 100, TimeUnit.MILLISECONDS);
            if (message == null) {
                break;
            }
            if (send(targetQueue, message)) {
                moved++;
            }
        }
        return moved;
    }

    // ==================== 工具方法 ====================

    private boolean isRedisEnabled() {
        return redisUtils != null && redisUtils.isOpen();
    }

    @PreDestroy
    public void shutdown() {
        if (log.isDebugEnabled())
            log.debug("Shutting down QueueService...");
        // 停止所有消费者（设置运行标志为 false，消费者线程将优雅退出）
        running.values().forEach(r -> r.set(false));
        // 取消所有空闲检测器
        idleCheckerFutures.values().forEach(f -> f.cancel(false));
        idleCheckerFutures.clear();
        // 关闭空闲检测调度器
        idleCheckScheduler.shutdown();
        try {
            if (!idleCheckScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                idleCheckScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            idleCheckScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        // 注意：消费者线程由 TagTaskExecutor 管理，其生命周期由 TagTaskExecutor.destroy() 处理
        if (log.isDebugEnabled())
            log.debug("QueueService shutdown completed");
    }
}
