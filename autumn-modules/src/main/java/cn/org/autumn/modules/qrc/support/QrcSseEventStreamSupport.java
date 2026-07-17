package cn.org.autumn.modules.qrc.support;

import cn.org.autumn.handler.MessageHandler;
import cn.org.autumn.service.RedisListenerService;
import com.alibaba.fastjson2.JSON;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 扫码 SSE 订阅/推送公共实现：本节点先投递 + Redis Pub/Sub 跨节点广播（同节点可能收到中继重复事件，前端需幂等）。 */
@Slf4j
public class QrcSseEventStreamSupport<T> {

    private final String label;
    private final String redisChannel;
    private final Function<T, String> uuidExtractor;
    private final Function<T, String> statusExtractor;
    private final Function<T, String> jsonSerializer;
    private final Function<String, T> jsonParser;
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    private RedisListenerService redisListenerService;

    public QrcSseEventStreamSupport(
            String label,
            String redisChannel,
            Function<T, String> uuidExtractor,
            Function<T, String> statusExtractor,
            Function<T, String> jsonSerializer,
            Function<String, T> jsonParser) {
        this.label = label;
        this.redisChannel = redisChannel;
        this.uuidExtractor = uuidExtractor;
        this.statusExtractor = statusExtractor;
        this.jsonSerializer = jsonSerializer;
        this.jsonParser = jsonParser;
    }

    public void bindRedisRelay(RedisListenerService redisListenerService) {
        this.redisListenerService = redisListenerService;
        if (redisListenerService == null) {
            return;
        }
        redisListenerService.subscribe(redisChannel, new MessageHandler() {
            @Override
            public void handle(String channel, String message) {
                if (StringUtils.isBlank(message)) {
                    return;
                }
                try {
                    T event = jsonParser.apply(message);
                    if (event == null || StringUtils.isBlank(uuidExtractor.apply(event))) {
                        return;
                    }
                    dispatchLocal(uuidExtractor.apply(event), event, false);
                } catch (Exception e) {
                    log.debug("{} SSE relay parse failed: {}", label, e.getMessage());
                }
            }
        });
    }

    public SseEmitter subscribe(String uuid, long expiredAt, T catchUp) {
        if (StringUtils.isBlank(uuid) || catchUp == null) {
            throw new IllegalArgumentException("扫码会话不存在或已过期");
        }
        long timeout = Math.max(30_000L, expiredAt - System.currentTimeMillis());
        SseEmitter emitter = new SseEmitter(timeout);
        subscribers.computeIfAbsent(uuid, key -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("{} SSE subscribe uuid={} status={} localSubscribers={}", label, uuid, statusExtractor.apply(catchUp), subscribers.get(uuid).size());
        emitter.onCompletion(() -> removeEmitter(uuid, emitter));
        emitter.onTimeout(() -> removeEmitter(uuid, emitter));
        emitter.onError(e -> removeEmitter(uuid, emitter));
        sendToEmitter(emitter, catchUp);
        return emitter;
    }

    public void publish(T event) {
        if (event == null || StringUtils.isBlank(uuidExtractor.apply(event))) {
            return;
        }
        String uuid = uuidExtractor.apply(event);
        String json = jsonSerializer.apply(event);
        log.debug("{} SSE publish uuid={} status={}", label, uuid, statusExtractor.apply(event));
        // 本节点先投递，避免仅走 Redis 异步中继时浏览器迟收 COMPLETED，同时 poll 已读到终态。
        dispatchLocal(uuid, event, false);
        if (redisListenerService != null) {
            redisListenerService.publish(redisChannel, json);
        }
    }

    private void dispatchLocal(String uuid, T event, boolean warnIfNoSubscribers) {
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.get(uuid);
        if (emitters == null || emitters.isEmpty()) {
            if (warnIfNoSubscribers) {
                log.warn("{} SSE publish uuid={} status={} but no local subscribers (browser SSE may be broken; use poll fallback)", label, uuid, statusExtractor.apply(event));
            } else {
                log.debug("{} SSE publish uuid={} status={} but no local subscribers", label, uuid, statusExtractor.apply(event));
            }
            return;
        }
        Iterator<SseEmitter> iterator = emitters.iterator();
        while (iterator.hasNext()) {
            SseEmitter emitter = iterator.next();
            if (!sendToEmitter(emitter, event)) {
                removeEmitter(uuid, emitter);
            }
        }
    }

    private boolean sendToEmitter(SseEmitter emitter, T event) {
        if (emitter == null || event == null) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name("status").data(jsonSerializer.apply(event), MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException e) {
            log.warn("{} SSE send failed uuid={}: {}", label, uuidExtractor.apply(event), e.getMessage());
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
            return false;
        }
    }

    private void removeEmitter(String uuid, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.get(uuid);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            subscribers.remove(uuid, emitters);
        }
    }
}
