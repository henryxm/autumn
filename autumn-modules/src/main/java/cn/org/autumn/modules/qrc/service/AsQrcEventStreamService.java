package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.handler.MessageHandler;
import cn.org.autumn.modules.qrc.model.AsQrcStreamEvent;
import cn.org.autumn.service.RedisListenerService;
import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** B2 同源扫码 SSE 推送：本地订阅 + Redis Pub/Sub 跨节点广播。 */
@Slf4j
@Service
public class AsQrcEventStreamService {

    public static final String REDIS_CHANNEL = "as:qrc:sse";

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    @Autowired(required = false)
    private RedisListenerService redisListenerService;

    @PostConstruct
    public void initRedisRelay() {
        if (redisListenerService == null) {
            return;
        }
        redisListenerService.subscribe(REDIS_CHANNEL, new MessageHandler() {
            @Override
            public void handle(String channel, String message) {
                if (StringUtils.isBlank(message)) {
                    return;
                }
                try {
                    AsQrcStreamEvent event = JSON.parseObject(message, AsQrcStreamEvent.class);
                    if (event != null && StringUtils.isNotBlank(event.getUuid())) {
                        dispatchLocal(event.getUuid(), event);
                    }
                } catch (Exception e) {
                    log.debug("AS QRC SSE relay parse failed: {}", e.getMessage());
                }
            }
        });
    }

    public SseEmitter subscribe(String uuid, long expiredAt, AsQrcStreamEvent catchUp) {
        if (StringUtils.isBlank(uuid) || catchUp == null) {
            throw new IllegalArgumentException("扫码会话不存在或已过期");
        }
        long timeout = Math.max(30_000L, expiredAt - System.currentTimeMillis());
        SseEmitter emitter = new SseEmitter(timeout);
        subscribers.computeIfAbsent(uuid, key -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("AS QRC SSE subscribe uuid={} status={} localSubscribers={}", uuid, catchUp.getStatus(), subscribers.get(uuid).size());
        emitter.onCompletion(() -> removeEmitter(uuid, emitter));
        emitter.onTimeout(() -> removeEmitter(uuid, emitter));
        emitter.onError(e -> removeEmitter(uuid, emitter));
        sendToEmitter(emitter, catchUp);
        return emitter;
    }

    public void publish(AsQrcStreamEvent event) {
        if (event == null || StringUtils.isBlank(event.getUuid())) {
            return;
        }
        log.debug("AS QRC SSE publish uuid={} status={} exchange={}", event.getUuid(), event.getStatus(), event.getExchange());
        dispatchLocal(event.getUuid(), event);
        if (redisListenerService != null) {
            redisListenerService.publish(REDIS_CHANNEL, gson.toJson(event));
        }
    }

    private void dispatchLocal(String uuid, AsQrcStreamEvent event) {
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.get(uuid);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("AS QRC SSE publish uuid={} status={} but no local subscribers", uuid, event == null ? null : event.getStatus());
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

    private boolean sendToEmitter(SseEmitter emitter, AsQrcStreamEvent event) {
        if (emitter == null || event == null) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name("status").data(JSON.toJSONString(event), MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException e) {
            log.warn("AS QRC SSE send failed uuid={}: {}", event.getUuid(), e.getMessage());
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
