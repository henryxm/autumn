package cn.org.autumn.opl.spi;

import cn.org.autumn.opl.model.OpenPlatformEvent;

/**
 * OPL 领域事件订阅（类似 {@link cn.org.autumn.handler.RobotMessageSubscriber}）。
 */
public interface OpenPlatformSubscriber {

    /**
     * 订阅事件，逗号分隔；{@code *} 表示全部（见 {@link cn.org.autumn.opl.OplConstants.Event}）。
     */
    String events();

    void onEvent(OpenPlatformEvent event);
}
