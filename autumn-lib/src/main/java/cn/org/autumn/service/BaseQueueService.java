package cn.org.autumn.service;

import cn.org.autumn.config.QueueConfig;
import cn.org.autumn.model.DefaultEntity;
import cn.org.autumn.model.Parameterized;
import cn.org.autumn.model.QueueMessage;
import cn.org.autumn.table.annotation.Table;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public abstract class BaseQueueService<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements Parameterized {

    @Autowired
    protected QueueService queueService;

    public Class<?> getModelClass() {
        try {
            return type(1, Table.class);
        } catch (Exception e) {
            log.error("解析错误:{}", e.getMessage());
            return DefaultEntity.class;
        }
    }

    // ==================== 队列服务相关方法 ====================

    public long getIdleTime() {
        return 60L;
    }

    public TimeUnit getIdleUnit() {
        return TimeUnit.SECONDS;
    }

    public boolean isAutoQueue() {
        return true;
    }

    public long getIdleTime(String suffix) {
        return 60L;
    }

    public TimeUnit getIdleUnit(String suffix) {
        return TimeUnit.SECONDS;
    }

    public boolean isAutoQueue(String suffix) {
        return true;
    }

    public <X> long getIdleTime(Class<X> clazz) {
        return 60L;
    }

    public <X> TimeUnit getIdleUnit(Class<X> clazz) {
        return TimeUnit.SECONDS;
    }

    public <X> boolean isAutoQueue(Class<X> clazz) {
        return true;
    }

    public <X> long getIdleTime(String suffix, Class<X> clazz) {
        return 60L;
    }

    public <X> TimeUnit getIdleUnit(String suffix, Class<X> clazz) {
        return TimeUnit.SECONDS;
    }

    public <X> boolean isAutoQueue(String suffix, Class<X> clazz) {
        return true;
    }

    /**
     * 获取默认队列名称
     * 基于实体类名称自动生成，格式：{entityName}Queue
     *
     * @return 队列名称
     */
    public String getQueueName() {
        return getModelClass().getSimpleName().replace("Entity", "").toLowerCase() + "queue";
    }

    /**
     * 获取指定后缀的队列名称
     *
     * @param suffix 队列名称后缀
     * @return 队列名称
     */
    public String getQueueName(String suffix) {
        String name = getModelClass().getSimpleName().replace("Entity", "").toLowerCase();
        return name + (suffix != null && !suffix.isEmpty() ? ":" + suffix : "") + "queue";
    }

    public <X> String getQueueName(Class<X> clazz) {
        String name = getQueueName();
        if (null != clazz)
            name = name + ":" + clazz.getSimpleName().toLowerCase();
        return name;
    }

    public <X> String getQueueName(String suffix, Class<X> clazz) {
        String name = getQueueName(suffix);
        if (null != clazz)
            name = name + ":" + clazz.getSimpleName().toLowerCase();
        return name;
    }

    /**
     * 获取队列配置
     * 如果配置不存在，则创建默认配置
     *
     * @return 队列配置
     */
    public QueueConfig getQueueConfig() {
        return QueueConfig.builder().name(getQueueName()).type(getModelClass()).auto(isAutoQueue()).idleTime(getIdleTime()).idleUnit(getIdleUnit()).build();
    }

    public <X> QueueConfig getQueueConfig(String suffix) {
        return QueueConfig.builder().name(getQueueName(suffix)).type(getModelClass()).auto(isAutoQueue(suffix)).idleTime(getIdleTime(suffix)).idleUnit(getIdleUnit(suffix)).build();
    }

    public <X> QueueConfig getQueueConfig(Class<X> clazz) {
        return QueueConfig.builder().name(getQueueName(clazz)).type(clazz).auto(isAutoQueue(clazz)).idleTime(getIdleTime(clazz)).idleUnit(getIdleUnit(clazz)).build();
    }

    public <X> QueueConfig getQueueConfig(String suffix, Class<X> clazz) {
        return QueueConfig.builder().name(getQueueName(suffix, clazz)).type(clazz).auto(isAutoQueue(suffix, clazz)).idleTime(getIdleTime(suffix, clazz)).idleUnit(getIdleUnit(suffix, clazz)).build();
    }

    /**
     * 注册队列配置和消费者处理器
     * 当 autoStart=true 时，发送消息会自动启动消费者
     * 子类需要重写 onQueueMessage 方法来处理消息
     *
     * @param config 队列配置（建议使用 getQueueConfig 创建）
     */
    public void register(QueueConfig config) {
        if (!config.isAuto())
            queueService.register(config);
        else {
            queueService.register(config, new QueueConsumer<T>() {
                @Override
                public boolean consume(QueueMessage<T> message) {
                    try {
                        T entity = message.getBody();
                        return onQueueMessage(entity);
                    } catch (Exception e) {
                        log.error("Queue message processing failed: {}", e.getMessage(), e);
                        return false;
                    }
                }

                @Override
                public void onError(QueueMessage<T> message, Throwable throwable) {
                    try {
                        T body = message.getBody();
                        onErrorMessage(body, throwable);
                    } catch (Exception e) {
                        log.error("Queue error message processing failed: {}", e.getMessage());
                    }
                }

                @Override
                public void onDead(QueueMessage<T> message) {
                    try {
                        T body = message.getBody();
                        onDeadMessage(body);
                    } catch (Exception e) {
                        log.error("Queue dead message processing failed: {}", e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * 注册默认的自动启动/停止队列
     * 使用默认的60秒空闲超时
     * 子类需要重写 onQueue 方法来处理消息
     */
    public void register() {
        register(getQueueConfig());
    }

    public void register(String suffix) {
        QueueConfig config = getQueueConfig(suffix);
        queueService.register(config, new QueueConsumer<T>() {
            @Override
            public boolean consume(QueueMessage<T> message) {
                try {
                    T body = message.getBody();
                    return onQueueMessage(suffix, body);
                } catch (Exception e) {
                    log.error("Queue message processing failed: {}", e.getMessage());
                    return false;
                }
            }

            @Override
            public void onError(QueueMessage<T> message, Throwable throwable) {
                try {
                    T body = message.getBody();
                    onErrorMessage(suffix, body, throwable);
                } catch (Exception e) {
                    log.error("Queue error message processing failed: {}", e.getMessage());
                }
            }

            @Override
            public void onDead(QueueMessage<T> message) {
                try {
                    T body = message.getBody();
                    onDeadMessage(suffix, body);
                } catch (Exception e) {
                    log.error("Queue dead message processing failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * 注册自定义类型的自动启动/停止队列（默认后缀为空）
     *
     * @param clazz 消息类型
     * @param <X>   消息类型泛型
     */
    public <X> void register(Class<X> clazz) {
        QueueConfig config = getQueueConfig(clazz);
        queueService.register(config, new QueueConsumer<X>() {
            @Override
            public boolean consume(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    return onQueueMessage(clazz, body);
                } catch (Exception e) {
                    log.error("Queue message processing failed: {}", e.getMessage());
                    return false;
                }
            }

            @Override
            public void onError(QueueMessage<X> message, Throwable throwable) {
                try {
                    X body = message.getBody();
                    onErrorMessage(clazz, body, throwable);
                } catch (Exception e) {
                    log.error("Queue error message processing failed: {}", e.getMessage());
                }
            }

            @Override
            public void onDead(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    onDeadMessage(clazz, body);
                } catch (Exception e) {
                    log.error("Queue dead message processing failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * 注册自定义类型的自动启动/停止队列
     * 子类需要重写 onQueueMessage(Class, X) 方法来处理消息
     *
     * @param suffix 队列后缀
     * @param clazz  消息类型
     * @param <X>    消息类型泛型
     */
    public <X> void register(String suffix, Class<X> clazz) {
        QueueConfig config = getQueueConfig(suffix, clazz);
        queueService.register(config, new QueueConsumer<X>() {
            @Override
            public boolean consume(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    return onQueueMessage(suffix, clazz, body);
                } catch (Exception e) {
                    log.error("Queue message processing failed: {}", e.getMessage());
                    return false;
                }
            }

            @Override
            public void onError(QueueMessage<X> message, Throwable throwable) {
                try {
                    X body = message.getBody();
                    onErrorMessage(suffix, clazz, body, throwable);
                } catch (Exception e) {
                    log.error("Queue error message processing failed: {}", e.getMessage());
                }
            }

            @Override
            public void onDead(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    onDeadMessage(suffix, clazz, body);
                } catch (Exception e) {
                    log.error("Queue dead message processing failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * 注册自定义类型的自动启动/停止队列（带处理器）
     *
     * @param suffix  队列后缀
     * @param clazz   消息类型
     * @param handler 消息处理器
     * @param <X>     消息类型泛型
     */
    public <X> void register(String suffix, Class<X> clazz, Function<X, Boolean> handler) {
        QueueConfig config = getQueueConfig(suffix, clazz);
        queueService.register(config, new QueueConsumer<X>() {
            @Override
            public boolean consume(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    return handler.apply(body);
                } catch (Exception e) {
                    log.error("Queue message processing failed: {}", e.getMessage(), e);
                    return false;
                }
            }

            @Override
            public void onError(QueueMessage<X> message, Throwable throwable) {
                try {
                    X body = message.getBody();
                    onErrorMessage(suffix, clazz, body, throwable);
                } catch (Exception e) {
                    log.error("Queue error message processing failed: {}", e.getMessage());
                }
            }

            @Override
            public void onDead(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    onDeadMessage(suffix, clazz, body);
                } catch (Exception e) {
                    log.error("Queue dead message processing failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * 注册自定义类型的自动启动/停止队列（带处理器，默认后缀为空）
     *
     * @param clazz   消息类型
     * @param handler 消息处理器
     * @param <X>     消息类型泛型
     */
    public <X> void register(Class<X> clazz, Function<X, Boolean> handler) {
        QueueConfig config = getQueueConfig(clazz);
        queueService.register(config, new QueueConsumer<X>() {
            @Override
            public boolean consume(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    return handler.apply(body);
                } catch (Exception e) {
                    log.error("Queue message processing failed: {}", e.getMessage(), e);
                    return false;
                }
            }

            @Override
            public void onError(QueueMessage<X> message, Throwable throwable) {
                try {
                    X body = message.getBody();
                    onErrorMessage(clazz, body, throwable);
                } catch (Exception e) {
                    log.error("Queue error message processing failed: {}", e.getMessage());
                }
            }

            @Override
            public void onDead(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    onDeadMessage(clazz, body);
                } catch (Exception e) {
                    log.error("Queue dead message processing failed: {}", e.getMessage());
                }
            }
        });
    }

    // ==================== 发送消息方法 ====================

    /**
     * 发送实体到自动队列（自动启动消费者，空闲后自动停止）
     * 使用此方法前需要先调用 registerAutoQueue() 注册消费者处理器
     *
     * @param entity 实体对象
     * @return 是否发送成功
     */
    public boolean sendQueue(T entity) {
        String name = getQueueName();
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(config);
        }
        // 如果没有注册自动队列配置，回退到普通发送
        return queueService.send(name, entity);
    }

    /**
     * 发送实体到指定后缀的自动队列
     *
     * @param suffix 队列后缀
     * @param entity 实体对象
     * @return 是否发送成功
     */
    public boolean sendQueue(String suffix, T entity) {
        String name = getQueueName(suffix);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig(suffix);
            if (config.isAuto())
                register(suffix);
        }
        return queueService.send(name, entity);
    }

    /**
     * 发送任意消息到默认队列
     *
     * @param message 消息对象
     * @param <V>     消息类型
     * @return 是否发送成功
     */
    public <V> boolean sendMessage(V message) {
        String name = getQueueName();
        QueueConfig config = queueService.getConfig(name);
        if (null == config) {
            config = getQueueConfig();
            if (config.isAuto())
                register(config);
        }
        return queueService.send(name, message);
    }

    /**
     * 发送任意消息到指定后缀的队列
     *
     * @param suffix  队列后缀
     * @param message 消息对象
     * @param <V>     消息类型
     * @return 是否发送成功
     */
    public <V> boolean sendMessage(String suffix, V message) {
        String name = getQueueName(suffix);
        QueueConfig config = queueService.getConfig(name);
        if (null == config) {
            config = getQueueConfig(suffix);
            if (config.isAuto())
                register(suffix);
        }
        return queueService.send(name, message);
    }

    /**
     * 发送自定义类型消息到默认自动队列（支持自动启动/停止）
     *
     * @param clazz   消息类型
     * @param message 消息对象
     * @param <X>     消息类型泛型
     * @return 是否发送成功
     */
    public <X> boolean sendQueue(Class<X> clazz, X message) {
        String name = getQueueName(clazz);
        QueueConfig config = queueService.getConfig(name);
        if (config != null) {
            return queueService.send(config, QueueMessage.of(message));
        } else {
            config = getQueueConfig(clazz);
            if (config.isAuto())
                register(clazz);
        }
        // 如果没有注册自动队列配置，回退到普通发送
        return queueService.send(name, message);
    }

    /**
     * 发送自定义类型消息到自动队列（支持自动启动/停止）
     * 使用此方法前需要先调用 registerAutoQueue(suffix, clazz, idleTimeoutSeconds) 注册
     *
     * @param suffix 队列后缀
     * @param clazz  消息类型
     * @param entity 消息对象
     * @param <X>    消息类型泛型
     * @return 是否发送成功
     */
    public <X> boolean sendQueue(String suffix, Class<X> clazz, X entity) {
        String name = getQueueName(suffix, clazz);
        QueueConfig config = queueService.getConfig(name);
        if (config != null) {
            return queueService.send(config, QueueMessage.of(entity));
        } else {
            config = getQueueConfig(suffix, clazz);
            if (config.isAuto())
                register(suffix, clazz);
        }
        // 如果没有注册自动队列配置，回退到普通发送
        return queueService.send(name, entity);
    }

    /**
     * 发送延迟消息到默认队列
     *
     * @param entity      实体对象
     * @param delayMillis 延迟毫秒数
     * @return 是否发送成功
     */
    public boolean sendDelay(T entity, long delayMillis) {
        String name = getQueueName();
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(config);
        }
        return queueService.sendDelay(name, entity, delayMillis);
    }

    /**
     * 发送延迟消息到指定后缀的队列
     *
     * @param suffix      队列后缀
     * @param entity      实体对象
     * @param delayMillis 延迟毫秒数
     * @return 是否发送成功
     */
    public boolean sendDelay(String suffix, T entity, long delayMillis) {
        String name = getQueueName(suffix);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(suffix);
        }
        return queueService.sendDelay(name, entity, delayMillis);
    }

    public <X> boolean sendDelay(Class<X> clazz, X entity, long delayMillis) {
        String name = getQueueName(clazz);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(clazz);
        }
        return queueService.sendDelay(name, entity, delayMillis);
    }

    public <X> boolean sendDelay(String suffix, Class<X> clazz, X entity, long delayMillis) {
        String name = getQueueName(suffix, clazz);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(suffix, clazz);
        }
        return queueService.sendDelay(name, entity, delayMillis);
    }

    /**
     * 发送定时消息到默认队列
     *
     * @param entity    实体对象
     * @param executeAt 执行时间戳（毫秒）
     * @return 是否发送成功
     */
    public boolean sendScheduled(T entity, long executeAt) {
        String name = getQueueName();
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(config);
        }
        return queueService.sendScheduled(name, entity, executeAt);
    }

    public boolean sendScheduled(String suffix, T entity, long executeAt) {
        String name = getQueueName(suffix);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(suffix);
        }
        return queueService.sendScheduled(name, entity, executeAt);
    }

    public <X> boolean sendScheduled(Class<X> clazz, X entity, long executeAt) {
        String name = getQueueName(clazz);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(clazz);
        }
        return queueService.sendScheduled(name, entity, executeAt);
    }

    public <X> boolean sendScheduled(String suffix, Class<X> clazz, X entity, long executeAt) {
        String name = getQueueName(suffix, clazz);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(suffix, clazz);
        }
        return queueService.sendScheduled(name, entity, executeAt);
    }

    /**
     * 发送优先级消息到默认队列
     *
     * @param entity   实体对象
     * @param priority 优先级（数字越小优先级越高）
     * @return 是否发送成功
     */
    public boolean sendPriority(T entity, int priority) {
        String name = getQueueName();
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(config);
        }
        return queueService.sendPriority(name, entity, priority);
    }

    public boolean sendPriority(String suffix, T entity, int priority) {
        String name = getQueueName(suffix);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(suffix);
        }
        return queueService.sendPriority(name, entity, priority);
    }

    public <X> boolean sendPriority(Class<X> clazz, T entity, int priority) {
        String name = getQueueName(clazz);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(clazz);
        }
        return queueService.sendPriority(name, entity, priority);
    }

    public <X> boolean sendPriority(String suffix, Class<X> clazz, T entity, int priority) {
        String name = getQueueName(suffix, clazz);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(suffix, clazz);
        }
        return queueService.sendPriority(name, entity, priority);
    }

    /**
     * 批量发送实体到默认队列
     *
     * @param entities 实体列表
     * @return 成功发送的数量
     */
    public int sendBatch(List<T> entities) {
        String name = getQueueName();
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register();
        }
        return queueService.sendBatch(name, entities);
    }

    /**
     * 批量发送实体到指定后缀的队列
     *
     * @param suffix   队列后缀
     * @param entities 实体列表
     * @return 成功发送的数量
     */
    public int sendBatch(String suffix, List<T> entities) {
        String name = getQueueName(suffix);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(suffix);
        }
        return queueService.sendBatch(name, entities);
    }

    public <X> int sendBatch(Class<X> clazz, List<X> entities) {
        String name = getQueueName(clazz);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(clazz);
        }
        return queueService.sendBatch(name, entities);
    }

    public <X> int sendBatch(String suffix, Class<X> clazz, List<X> entities) {
        String name = getQueueName(suffix, clazz);
        QueueConfig config = queueService.getConfig(name);
        if (config == null) {
            config = getQueueConfig();
            if (config.isAuto())
                register(suffix, clazz);
        }
        return queueService.sendBatch(name, entities);
    }

    // ==================== 消费消息方法 ====================

    /**
     * 从默认队列消费一条消息
     *
     * @return 实体对象，如果队列为空返回null
     */
    public T pollQueue() {
        return pollQueue("");
    }

    /**
     * 从指定后缀的队列消费一条消息
     *
     * @param suffix 队列后缀
     * @return 实体对象，如果队列为空返回null
     */
    public T pollQueue(String suffix) {
        String name = getQueueName(suffix);
        QueueMessage<T> message = queueService.poll(name);
        return message != null ? message.getBody() : null;
    }

    public <X> X pollQueue(Class<X> clazz) {
        return pollQueue("", clazz);
    }

    public <X> X pollQueue(String suffix, Class<X> clazz) {
        QueueMessage<X> message = queueService.poll(getQueueName(suffix, clazz));
        return message != null ? message.getBody() : null;
    }

    /**
     * 从默认队列消费一条消息（带超时）
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 实体对象，如果超时返回null
     */
    public T pollQueue(long timeout, TimeUnit unit) {
        return pollQueue("", timeout, unit);
    }

    public T pollQueue(String suffix, long timeout, TimeUnit unit) {
        String name = getQueueName(suffix);
        QueueMessage<T> message = queueService.poll(name, timeout, unit);
        return message != null ? message.getBody() : null;
    }

    public <X> X pollQueue(Class<X> clazz, long timeout, TimeUnit unit) {
        return pollQueue("", clazz, timeout, unit);
    }

    public <X> X pollQueue(String suffix, Class<X> clazz, long timeout, TimeUnit unit) {
        String name = getQueueName(suffix, clazz);
        QueueMessage<X> message = queueService.poll(name, timeout, unit);
        return message != null ? message.getBody() : null;
    }

    /**
     * 从默认队列批量消费消息
     *
     * @param maxCount 最大消费数量
     * @return 实体列表
     */
    public List<T> pollBatch(int maxCount) {
        return pollBatch("", maxCount);
    }

    public List<T> pollBatch(String suffix, int maxCount) {
        String name = getQueueName(suffix);
        List<QueueMessage<T>> messages = queueService.pollBatch(name, maxCount);
        List<T> result = new ArrayList<>();
        for (QueueMessage<T> message : messages) {
            if (message != null && message.getBody() != null) {
                result.add(message.getBody());
            }
        }
        return result;
    }

    public <X> List<X> pollBatch(Class<X> clazz, int maxCount) {
        return pollBatch("", clazz, maxCount);
    }

    public <X> List<X> pollBatch(String suffix, Class<X> clazz, int maxCount) {
        String name = getQueueName(suffix, clazz);
        List<QueueMessage<X>> messages = queueService.pollBatch(name, maxCount);
        List<X> result = new ArrayList<>();
        for (QueueMessage<X> message : messages) {
            if (message != null && message.getBody() != null) {
                result.add(message.getBody());
            }
        }
        return result;
    }

    /**
     * 使用 Consumer 处理队列消息
     * 如果队列中有消息则用 consumer 处理
     *
     * @param consumer 消息处理器
     * @return 是否处理了消息
     */
    public boolean consume(Consumer<T> consumer) {
        return queueService.consume(getQueueName(), consumer);
    }

    /**
     * 使用 Consumer 处理指定后缀队列的消息
     *
     * @param suffix   队列后缀
     * @param consumer 消息处理器
     * @return 是否处理了消息
     */
    public boolean consume(String suffix, Consumer<T> consumer) {
        return queueService.consume(getQueueName(suffix), consumer);
    }

    public boolean consume(Class<T> clazz, Consumer<T> consumer) {
        return queueService.consume(getQueueName(clazz), consumer);
    }

    public boolean consume(String suffix, Class<T> clazz, Consumer<T> consumer) {
        return queueService.consume(getQueueName(suffix, clazz), consumer);
    }

    // ==================== 异步消费者方法 ====================

    /**
     * 启动默认队列的异步消费者
     * 子类需要重写 onQueueMessage 方法来处理消息
     */
    public void startQueue() {
        startQueue(1);
    }

    /**
     * 启动默认队列的异步消费者（指定并发数）
     *
     * @param concurrency 并发消费者数量
     */
    public void startQueue(int concurrency) {
        queueService.start(getQueueName(), new QueueConsumer<T>() {
            @Override
            public boolean consume(QueueMessage<T> message) {
                try {
                    T entity = message.getBody();
                    return onQueueMessage(entity);
                } catch (Exception e) {
                    log.error("Queue message processing failed: {}", e.getMessage());
                    return false;
                }
            }

            @Override
            public void onError(QueueMessage<T> message, Throwable throwable) {
                try {
                    T entity = message.getBody();
                    onErrorMessage(entity, throwable);
                } catch (Exception e) {
                    log.error("Queue error message processing failed: {}", e.getMessage());
                }
            }

            @Override
            public void onDead(QueueMessage<T> message) {
                try {
                    T entity = message.getBody();
                    onDeadMessage(entity);
                } catch (Exception e) {
                    log.error("Queue dead message processing failed: {}", e.getMessage());
                }
            }
        }, concurrency);
    }

    /**
     * 启动指定后缀队列的异步消费者
     *
     * @param suffix      队列后缀
     * @param concurrency 并发消费者数量
     * @param handler     消息处理器
     */
    public void startQueue(String suffix, int concurrency, Function<T, Boolean> handler) {
        queueService.start(getQueueName(suffix), new QueueConsumer<T>() {
            @Override
            public boolean consume(QueueMessage<T> message) {
                try {
                    T entity = message.getBody();
                    return handler.apply(entity);
                } catch (Exception e) {
                    log.error("Queue message processing failed: {}", e.getMessage());
                    return false;
                }
            }

            @Override
            public void onError(QueueMessage<T> message, Throwable throwable) {
                try {
                    T body = message.getBody();
                    onErrorMessage(suffix, body, throwable);
                } catch (Exception e) {
                    log.error("Queue error message processing failed: {}", e.getMessage());
                }
            }

            @Override
            public void onDead(QueueMessage<T> message) {
                try {
                    T body = message.getBody();
                    onDeadMessage(suffix, body);
                } catch (Exception e) {
                    log.error("Queue dead message processing failed: {}", e.getMessage());
                }
            }
        }, concurrency);
    }

    /**
     * 启动支持自定义类型的异步消费者
     *
     * @param suffix      队列后缀
     * @param clazz       消息类型
     * @param concurrency 并发消费者数量
     * @param <X>         消息类型泛型
     */
    public <X> void startQueue(String suffix, Class<X> clazz, int concurrency) {
        queueService.start(getQueueName(suffix, clazz), new QueueConsumer<X>() {
            @Override
            public boolean consume(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    return onQueueMessage(clazz, body);
                } catch (Exception e) {
                    log.error("Queue message processing failed: {}", e.getMessage());
                    return false;
                }
            }

            @Override
            public void onError(QueueMessage<X> message, Throwable throwable) {
                try {
                    X body = message.getBody();
                    onErrorMessage(clazz, body, throwable);
                } catch (Exception e) {
                    log.error("Queue error message processing failed: {}", e.getMessage());
                }
            }

            @Override
            public void onDead(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    onDeadMessage(clazz, body);
                } catch (Exception e) {
                    log.error("Queue dead message processing failed: {}", e.getMessage());
                }
            }
        }, concurrency);
    }

    /**
     * 启动支持自定义类型的异步消费者（带处理器）
     *
     * @param suffix      队列后缀
     * @param clazz       消息类型
     * @param concurrency 并发消费者数量
     * @param handler     消息处理器
     * @param <X>         消息类型泛型
     */
    public <X> void startQueue(String suffix, Class<X> clazz, int concurrency, Function<X, Boolean> handler) {
        queueService.start(getQueueName(suffix, clazz), new QueueConsumer<X>() {
            @Override
            public boolean consume(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    return handler.apply(body);
                } catch (Exception e) {
                    log.error("Queue message processing failed: {}", e.getMessage());
                    return false;
                }
            }

            @Override
            public void onError(QueueMessage<X> message, Throwable throwable) {
                try {
                    X body = message.getBody();
                    onErrorMessage(clazz, body, throwable);
                } catch (Exception e) {
                    log.error("Queue error message processing failed: {}", e.getMessage());
                }
            }

            @Override
            public void onDead(QueueMessage<X> message) {
                try {
                    X body = message.getBody();
                    onDeadMessage(clazz, body);
                } catch (Exception e) {
                    log.error("Queue dead message processing failed: {}", e.getMessage());
                }
            }
        }, concurrency);
    }

    /**
     * 停止默认队列的消费者
     */
    public void stopQueue() {
        queueService.stop(getQueueName());
    }

    /**
     * 停止指定后缀队列的消费者
     *
     * @param suffix 队列后缀
     */
    public void stopQueue(String suffix) {
        queueService.stop(getQueueName(suffix));
    }

    /**
     * 队列消息处理回调方法（实体类型）
     * 子类可以重写此方法来处理队列消息
     * 当使用 startQueueConsumer() 启动消费者时，消息会回调到此方法
     *
     * @param entity 实体对象
     * @return 处理结果：true 表示成功，false 表示失败（将触发重试）
     */
    protected boolean onQueueMessage(T entity) {
        log.warn("Queue message received but onQueueMessage not implemented: {}", entity);
        return true;
    }

    protected void onErrorMessage(T entity, Throwable throwable) {
        log.warn("Queue error message received but onErrorMessage not implemented: {}", entity);
    }

    protected void onDeadMessage(T entity) {
        log.warn("Queue dead message received but onDeadMessage not implemented: {}", entity);
    }

    protected <X> boolean onQueueMessage(String suffix, X message) {
        log.warn("Queue message received but onQueueMessage(String) not implemented: suffix={}, message={}", suffix, message);
        return true;
    }

    protected <X> void onErrorMessage(String suffix, X message, Throwable throwable) {
        log.warn("Queue error message received but onErrorMessage(String, Throwable) not implemented: suffix={}, message={}", suffix, message);
    }

    protected <X> void onDeadMessage(String suffix, X message) {
        log.warn("Queue message received but onDeadMessage(String) not implemented: suffix={}, message={}", suffix, message);
    }

    /**
     * 队列消息处理回调方法（自定义类型）
     * 子类可以重写此方法来处理不同类型的队列消息
     * 当使用 startQueueConsumer(suffix, type, concurrency) 启动消费者时，消息会回调到此方法
     *
     * @param type    消息类型
     * @param message 消息对象
     * @param <X>     消息类型泛型
     * @return 处理结果：true 表示成功，false 表示失败（将触发重试）
     */
    protected <X> boolean onQueueMessage(Class<X> type, X message) {
        log.warn("Queue message received but onQueueMessage(Class, X) not implemented: type={}, message={}", type.getSimpleName(), message);
        return true;
    }

    protected <X> void onErrorMessage(Class<X> type, X message, Throwable throwable) {
        log.warn("Queue error message received but onErrorMessage(Class, X, Throwable) not implemented: type={}, message={}", type.getSimpleName(), message);
    }

    protected <X> void onDeadMessage(Class<X> type, X message) {
        log.warn("Queue message received but onDeadMessage(Class, X) not implemented: type={}, message={}", type.getSimpleName(), message);
    }

    protected <X> boolean onQueueMessage(String suffix, Class<X> type, X message) {
        log.warn("Queue message received but onQueueMessage(Class, X) not implemented: suffix={}, type={}, message={}", suffix, type.getSimpleName(), message);
        return true;
    }

    protected <X> void onErrorMessage(String suffix, Class<X> type, X message, Throwable throwable) {
        log.warn("Queue error message received but onErrorMessage(Class, X, Throwable) not implemented: suffix={}, type={}, message={}", suffix, type.getSimpleName(), message);
    }

    protected <X> void onDeadMessage(String suffix, Class<X> type, X message) {
        log.warn("Queue message received but onDeadMessage(Class, X) not implemented: suffix={}, type={}, message={}", suffix, type.getSimpleName(), message);
    }
    // ==================== 队列管理方法 ====================

    /**
     * 获取默认队列长度
     *
     * @return 队列长度
     */
    public long getQueueSize() {
        return queueService.size(getQueueName());
    }

    /**
     * 获取指定后缀队列的长度
     *
     * @param suffix 队列后缀
     * @return 队列长度
     */
    public long getQueueSize(String suffix) {
        return queueService.size(getQueueName(suffix));
    }

    /**
     * 检查默认队列是否为空
     *
     * @return 是否为空
     */
    public boolean isQueueEmpty() {
        return queueService.isEmpty(getQueueName());
    }

    /**
     * 清空默认队列
     */
    public void clearQueue() {
        queueService.clear(getQueueName());
    }

    /**
     * 清空指定后缀的队列
     *
     * @param suffix 队列后缀
     */
    public void clearQueue(String suffix) {
        queueService.clear(getQueueName(suffix));
    }
}
