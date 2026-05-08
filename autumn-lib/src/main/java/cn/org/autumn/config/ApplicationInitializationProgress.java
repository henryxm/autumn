package cn.org.autumn.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 应用启动阶段状态，供安装完成页轮询 {@code /install/bootstrap-status} 与排障。
 * <p>
 * 在 {@code autumn.install.mode=true} 时进入 {@link Phase#WIZARD}；正常启动时依次经过 INIT → LOAD → UPGRADE → REFRESH → DONE。
 * <p>
 * {@link #languageCacheReady}：多语言服务将库表词条载入内存缓存后置为 true，供页面/Freemarker 与 {@code /client/health} 就绪判断；
 * 安装向导阶段通常不跑完整 Load，此标记可能仍为 false，调用方需结合 {@link #getPhase()}。
 */
@Component
public class ApplicationInitializationProgress {

    public enum Phase {
        IDLE("准备中"),
        WIZARD("等待安装向导"),
        INIT("正在执行系统初始化（Init）…"),
        LOAD("正在加载基础数据（Load）…"),
        UPGRADE("正在执行升级与域变更（Upgrade）…"),
        REFRESH("正在刷新缓存（Refresh）…"),
        DONE("启动完成"),
        FAILED("启动失败");

        private final String defaultLabel;

        Phase(String defaultLabel) {
            this.defaultLabel = defaultLabel;
        }

        public String getDefaultLabel() {
            return defaultLabel;
        }
    }

    private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.IDLE);
    private volatile String message;
    private volatile String failedDetail;
    private volatile long startedAt;
    private volatile long finishedAt;
    /** 失败前所处阶段的进度，供进度条停留在合理位置 */
    private volatile int failedAtPercent = 30;

    /**
     * {@code LanguageService} 已完成至少一次成功的 DB → 内存缓存加载（允许条目数为 0）。
     * 安装向导 {@link Phase#WIZARD} 下通常不跑 {@code LoadFactory}，此标记可能一直为 false，调用方应结合 {@link #getPhase()}。
     */
    private volatile boolean languageCacheReady;

    public Phase getPhase() {
        return phase.get();
    }

    public boolean isLanguageCacheReady() {
        return languageCacheReady;
    }

    public void markLanguageCacheReady() {
        languageCacheReady = true;
    }

    public void clearLanguageCacheReady() {
        languageCacheReady = false;
    }

    public String getMessage() {
        String m = message;
        return m != null ? m : phase.get().getDefaultLabel();
    }

    public String getFailedDetail() {
        return failedDetail;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    public void resetForNewRun() {
        phase.set(Phase.IDLE);
        message = null;
        failedDetail = null;
        startedAt = 0;
        finishedAt = 0;
        failedAtPercent = 30;
        languageCacheReady = false;
    }

    public void enterWizardWaiting() {
        failedDetail = null;
        languageCacheReady = false;
        phase.set(Phase.WIZARD);
        message = Phase.WIZARD.getDefaultLabel();
        finishedAt = 0;
    }

    public void beginInitialization() {
        failedDetail = null;
        failedAtPercent = 30;
        startedAt = System.currentTimeMillis();
        finishedAt = 0;
        languageCacheReady = false;
        enter(Phase.INIT, Phase.INIT.getDefaultLabel());
    }

    public void enterLoad() {
        enter(Phase.LOAD, Phase.LOAD.getDefaultLabel());
    }

    public void enterUpgrade() {
        enter(Phase.UPGRADE, Phase.UPGRADE.getDefaultLabel());
    }

    public void enterRefresh() {
        enter(Phase.REFRESH, Phase.REFRESH.getDefaultLabel());
    }

    public void markDone() {
        phase.set(Phase.DONE);
        message = Phase.DONE.getDefaultLabel();
        finishedAt = System.currentTimeMillis();
    }

    public void markFailed(Throwable e) {
        Phase was = phase.get();
        if (was != Phase.FAILED) {
            failedAtPercent = percentForNonTerminal(was);
        }
        languageCacheReady = false;
        phase.set(Phase.FAILED);
        message = Phase.FAILED.getDefaultLabel();
        failedDetail = rootMessage(e);
        finishedAt = System.currentTimeMillis();
    }

    private void enter(Phase p, String msg) {
        phase.set(p);
        message = msg;
    }

    private static String rootMessage(Throwable e) {
        if (e == null) {
            return null;
        }
        Throwable c = e;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        String m = c.getMessage();
        return m != null ? m : c.getClass().getSimpleName();
    }

    public int getPercentForPhase() {
        Phase p = phase.get();
        if (p == Phase.FAILED) {
            return Math.min(failedAtPercent, 95);
        }
        return percentForNonTerminal(p);
    }

    private static int percentForNonTerminal(Phase p) {
        if (p == null) {
            return 0;
        }
        switch (p) {
            case IDLE:
                return 0;
            case WIZARD:
                return 8;
            case INIT:
                return 24;
            case LOAD:
                return 44;
            case UPGRADE:
                return 64;
            case REFRESH:
                return 86;
            case DONE:
                return 100;
            default:
                return 0;
        }
    }
}
