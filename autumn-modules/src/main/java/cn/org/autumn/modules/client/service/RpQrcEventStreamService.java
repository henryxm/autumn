package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.model.RpQrcPendingSession;
import cn.org.autumn.modules.client.model.RpQrcStreamEvent;
import cn.org.autumn.modules.qrc.support.QrcSseEventStreamSupport;
import cn.org.autumn.service.RedisListenerService;
import com.alibaba.fastjson.JSON;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** RP 联邦扫码 SSE 推送：本地订阅 + Redis Pub/Sub 跨节点广播。 */
@Slf4j
@Service
public class RpQrcEventStreamService {

    public static final String REDIS_CHANNEL = "rp:qrc:sse";

    private final QrcSseEventStreamSupport<RpQrcStreamEvent> streamSupport = new QrcSseEventStreamSupport<>(
            "RP QRC",
            REDIS_CHANNEL,
            RpQrcStreamEvent::getUuid,
            RpQrcStreamEvent::getStatus,
            JSON::toJSONString,
            json -> JSON.parseObject(json, RpQrcStreamEvent.class));

    @Autowired(required = false)
    private RedisListenerService redisListenerService;

    @PostConstruct
    public void initRedisRelay() {
        streamSupport.bindRedisRelay(redisListenerService);
    }

    public SseEmitter subscribe(String uuid, RpQrcPendingSession pending) {
        if (pending == null) {
            throw new IllegalArgumentException("扫码会话不存在或已过期");
        }
        return streamSupport.subscribe(uuid, pending.getExpiredAt(), RpQrcStreamEvent.from(pending));
    }

    public void publish(RpQrcPendingSession pending) {
        if (pending == null) {
            return;
        }
        streamSupport.publish(RpQrcStreamEvent.from(pending));
    }
}
