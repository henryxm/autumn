package cn.org.autumn.modules.bot.service;

import cn.org.autumn.handler.RobotHookDispatch;
import cn.org.autumn.modules.bot.entity.RobotHookEntity;
import cn.org.autumn.modules.bot.model.RobotHookDispatchTask;
import cn.org.autumn.modules.bot.support.RobotHookSupport;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 机器人 Hook 事件投递：匹配 Hook 后写入框架队列，HTTP 组包在消费端完成。
 */
@Service
public class RobotHookDispatcher implements RobotHookDispatch {

    @Autowired
    @Lazy
    private RobotHookService robotHookService;

    @Autowired
    private Gson gson;

    public void dispatch(String robot, String event, Object payload) {
        if (StringUtils.isBlank(robot) || StringUtils.isBlank(event))
            return;
        List<RobotHookEntity> hooks = robotHookService.listActiveByRobot(robot);
        if (hooks == null || hooks.isEmpty())
            return;
        String timestamp = String.valueOf(System.currentTimeMillis());
        for (RobotHookEntity hook : hooks) {
            if (!hook.isActive() || !RobotHookSupport.matchesEvent(hook.getEvents(), event))
                continue;
            String dataJson = RobotHookSupport.toDataJson(gson, payload);
            robotHookService.dispatch(new RobotHookDispatchTask(hook.getOwner(), robot, hook.getUuid(), event, timestamp, dataJson));
        }
    }
}
