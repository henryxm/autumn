package cn.org.autumn.modules.bugshut.controller;

import jakarta.websocket.server.ServerEndpoint;

/**
 * 测试夹具：模拟业务仓 controller 包下的 JSR-356 端点（不得被管理 IP 门禁命中）。
 */
@ServerEndpoint("/bugsocket-fixture/{id}")
public class BugshutSocketFixture {

    public void onOpen() {
        // no-op
    }
}
