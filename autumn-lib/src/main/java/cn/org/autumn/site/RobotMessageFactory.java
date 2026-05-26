package cn.org.autumn.site;

import cn.org.autumn.handler.RobotMessageSubscriber;
import cn.org.autumn.model.RobotMessage;
import cn.org.autumn.utils.SubscriptionMatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 机器人入站消息分发：按 Order 调用所有 {@link RobotMessageSubscriber}。
 */
@Slf4j
@Component
public class RobotMessageFactory extends Factory {

    /**
     * @return 实际调用的订阅者数量
     */
    public int dispatch(RobotMessage message) {
        if (message == null || StringUtils.isBlank(message.getType()))
            return 0;
        List<RobotMessageSubscriber> subscribers = getOrderList(RobotMessageSubscriber.class, "receive", RobotMessage.class);
        if (subscribers == null || subscribers.isEmpty())
            return 0;
        int count = 0;
        for (RobotMessageSubscriber subscriber : subscribers) {
            if (subscriber == null)
                continue;
            try {
                if (!SubscriptionMatch.matches(subscriber.events(), message.getType()))
                    continue;
                subscriber.receive(message);
                count++;
            } catch (Exception e) {
                log.error("机器人消息订阅处理失败: {}, type={}", subscriber.getClass().getName(), message.getType(), e);
            }
        }
        return count;
    }
}
