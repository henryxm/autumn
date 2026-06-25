package cn.org.autumn.modules.bot.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.QueueConfig;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.handler.RobotHookDispatch;
import cn.org.autumn.model.RobotMessage;
import cn.org.autumn.modules.bot.dao.RobotDao;
import cn.org.autumn.modules.bot.dto.RobotMessagePushResult;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.model.RobotMessageDispatchTask;
import cn.org.autumn.modules.bot.support.RobotHookSupport;
import cn.org.autumn.modules.bot.support.RobotMessageSupport;
import cn.org.autumn.modules.bot.support.RobotScopes;
import cn.org.autumn.site.RobotMessageFactory;
import cn.org.autumn.utils.Uuid;
import com.google.gson.Gson;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 机器人入站消息：API 同步校验并入队，由框架 {@link cn.org.autumn.service.QueueService} 异步分发给订阅者与 Hook。
 */
@Slf4j
@Service
@DependsOn("tableInit")
public class RobotMessageService extends ModuleService<RobotDao, RobotEntity> {

    private static final String MESSAGE_DISPATCH_SUFFIX = "dispatch";

    @Autowired
    @Lazy
    private RobotService robotService;

    @Autowired
    @Lazy
    private RobotMessageFactory robotMessageFactory;

    @Autowired
    @Lazy
    private RobotHookDispatch robotHookDispatch;

    @Autowired
    @Lazy
    private RobotQuotaService robotQuotaService;

    @Autowired
    @Lazy
    private RobotMessageRateLimiter robotMessageRateLimiter;

    @Autowired
    @Lazy
    private RobotMessageIdempotencyService robotMessageIdempotencyService;

    @Autowired
    private Gson gson;

    @Override
    public String ico() {
        return "fa-envelope";
    }

    @Override
    public <X> boolean isAutoQueue(String suffix, Class<X> clazz) {
        if (MESSAGE_DISPATCH_SUFFIX.equals(suffix) && RobotMessageDispatchTask.class.equals(clazz))
            return true;
        return super.isAutoQueue(suffix, clazz);
    }

    @Override
    public <X> long getIdleTime(String suffix, Class<X> clazz) {
        if (MESSAGE_DISPATCH_SUFFIX.equals(suffix) && RobotMessageDispatchTask.class.equals(clazz))
            return 120L;
        return super.getIdleTime(suffix, clazz);
    }

    @Override
    public <X> TimeUnit getIdleUnit(String suffix, Class<X> clazz) {
        if (MESSAGE_DISPATCH_SUFFIX.equals(suffix) && RobotMessageDispatchTask.class.equals(clazz))
            return TimeUnit.SECONDS;
        return super.getIdleUnit(suffix, clazz);
    }

    @Override
    public <X> QueueConfig getQueueConfig(String suffix, Class<X> clazz) {
        if (MESSAGE_DISPATCH_SUFFIX.equals(suffix) && RobotMessageDispatchTask.class.equals(clazz))
            return dispatchQueueConfig(suffix, clazz, robotQuotaService.getGlobal().getMessageDispatchRetries());
        return super.getQueueConfig(suffix, clazz);
    }

    @PostConstruct
    public void initMessageDispatchQueue() {
        register(MESSAGE_DISPATCH_SUFFIX, RobotMessageDispatchTask.class, this::consumeMessageDispatch);
    }

    /**
     * 校验并入队；订阅者与 Hook 在队列消费端执行。
     */
    public RobotMessagePushResult push(String robotUuid, String ownerUuid, String type, String data, String idempotencyKey)
            throws Exception {
        RobotMessageSupport.validateIdempotencyKey(idempotencyKey);
        return robotMessageIdempotencyService.executeOnce(robotUuid, idempotencyKey,
                () -> pushInternal(robotUuid, ownerUuid, type, data, idempotencyKey));
    }

    private RobotMessagePushResult pushInternal(String robotUuid, String ownerUuid, String type, String data,
                                                String idempotencyKey) throws CodeException {
        if (StringUtils.isBlank(robotUuid))
            throw new CodeException("机器人不可用");
        robotMessageRateLimiter.check(robotUuid);
        RobotMessageSupport.validateType(type);
        String payload = RobotMessageSupport.normalizePayload(data);
        RobotEntity robot = robotService.getByUuid(robotUuid);
        if (robot == null || !robot.isActive())
            throw new CodeException("机器人未启用");
        if (StringUtils.isNotBlank(ownerUuid) && !ownerUuid.equals(robot.getOwner()))
            throw new CodeException("无权使用该机器人");
        RobotMessageSupport.assertScope(robot.getScopes(), RobotScopes.MESSAGE_PUSH);

        String messageId = resolveMessageId(idempotencyKey);
        long timestamp = System.currentTimeMillis();
        touchLastUsed(robot);

        RobotMessageDispatchTask task = new RobotMessageDispatchTask(
                messageId, robotUuid, robot.getOwner(), type.trim(), payload, timestamp);
        if (!enqueueMessageDispatch(task))
            throw new CodeException("消息入队失败，请稍后重试");
        return RobotMessagePushResult.accepted(messageId, task.getType());
    }

    private String resolveMessageId(String idempotencyKey) {
        if (StringUtils.isNotBlank(idempotencyKey))
            return idempotencyKey.trim();
        return Uuid.uuid();
    }

    public boolean enqueueMessageDispatch(RobotMessageDispatchTask task) {
        if (task == null || StringUtils.isBlank(task.getMessageId()))
            return false;
        return sendQueue(MESSAGE_DISPATCH_SUFFIX, RobotMessageDispatchTask.class, task);
    }

    private boolean consumeMessageDispatch(RobotMessageDispatchTask task) {
        if (task == null || StringUtils.isBlank(task.getMessageId()))
            return true;
        RobotEntity robot = robotService.getByUuid(task.getRobot());
        if (robot == null || !robot.isActive()) {
            log.warn("Inbound message skipped, robot unavailable: messageId={}, robot={}", task.getMessageId(), task.getRobot());
            return true;
        }
        RobotMessage message = task.toMessage();
        try {
            robotMessageFactory.dispatch(message);
        } catch (Exception e) {
            log.warn("Inbound message subscription dispatch error: messageId={}, type={}, {}", task.getMessageId(), task.getType(), e.getMessage());
        }
        try {
            robotHookDispatch.dispatch(task.getRobot(), task.getType(), buildHookPayload(message));
        } catch (Exception e) {
            log.warn("Inbound message Hook delivery error: messageId={}, type={}, {}", task.getMessageId(), task.getType(), e.getMessage());
        }
        return true;
    }

    @Override
    protected <X> void onDeadMessage(String suffix, Class<X> type, X message) {
        if (MESSAGE_DISPATCH_SUFFIX.equals(suffix) && message instanceof RobotMessageDispatchTask) {
            RobotMessageDispatchTask task = (RobotMessageDispatchTask) message;
            log.error("Inbound message moved to dead letter: messageId={}, robot={}, type={}", task.getMessageId(), task.getRobot(), task.getType());
        } else {
            super.onDeadMessage(suffix, type, message);
        }
    }

    private Map<String, Object> buildHookPayload(RobotMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", message.getMessageId());
        payload.put("type", message.getType());
        payload.put("owner", message.getOwner());
        RobotHookSupport.putJsonField(payload, "payload", message.getData(), gson);
        return payload;
    }

    private <X> QueueConfig dispatchQueueConfig(String suffix, Class<X> clazz, int retries) {
        QueueConfig base = super.getQueueConfig(suffix, clazz);
        int retryTimes = retries > 0 ? retries : 3;
        return QueueConfig.builder()
                .name(base.getName())
                .type(base.getType())
                .auto(base.isAuto())
                .idleTime(getIdleTime(suffix, clazz))
                .idleUnit(getIdleUnit(suffix, clazz))
                .retries(retryTimes)
                .deadLetter(true)
                .build();
    }

    private void touchLastUsed(RobotEntity robot) {
        Date now = new Date();
        robot.setLastUsedTime(now);
        robot.setUpdateTime(now);
        robotService.updateById(robot);
    }
}
