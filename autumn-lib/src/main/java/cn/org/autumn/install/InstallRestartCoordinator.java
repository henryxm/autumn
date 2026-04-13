package cn.org.autumn.install;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

/**
 * 安装完成后由主线程阻塞等待，收到信号后关闭上下文并再次启动应用（无需用户手动重启）。
 */
@Component
@ConditionalOnProperty(prefix = "autumn.install", name = "mode", havingValue = "true")
public class InstallRestartCoordinator {

    private final CountDownLatch restartLatch = new CountDownLatch(1);

    public void awaitRestart() throws InterruptedException {
        restartLatch.await();
    }

    public void signalRestart() {
        restartLatch.countDown();
    }
}
