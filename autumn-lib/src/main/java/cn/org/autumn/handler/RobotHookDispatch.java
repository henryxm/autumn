package cn.org.autumn.handler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(RobotHookDispatch.class)
public interface RobotHookDispatch {
    void dispatch(String robot, String event, Object payload);
}
