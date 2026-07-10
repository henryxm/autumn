package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.modules.qrc.model.AsQrcStreamEvent;
import cn.org.autumn.modules.qrc.support.QrcSseEventStreamSupport;
import cn.org.autumn.service.RedisListenerService;
import com.alibaba.fastjson.JSON;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** B2 同源扫码 SSE 推送：本地订阅 + Redis Pub/Sub 跨节点广播。 */
@Slf4j
@Service
public class AsQrcEventStreamService {

    public static final String REDIS_CHANNEL = "as:qrc:sse";

    private final QrcSseEventStreamSupport<AsQrcStreamEvent> streamSupport = new QrcSseEventStreamSupport<>(
            "AS QRC",
            REDIS_CHANNEL,
            AsQrcStreamEvent::getUuid,
            AsQrcStreamEvent::getStatus,
            JSON::toJSONString,
            json -> JSON.parseObject(json, AsQrcStreamEvent.class));

    @Autowired(required = false)
    private RedisListenerService redisListenerService;

    @PostConstruct
    public void initRedisRelay() {
        streamSupport.bindRedisRelay(redisListenerService);
    }

    public SseEmitter subscribe(String uuid, long expiredAt, AsQrcStreamEvent catchUp) {
        return streamSupport.subscribe(uuid, expiredAt, catchUp);
    }

    public void publish(AsQrcStreamEvent event) {
        streamSupport.publish(event);
    }
}
