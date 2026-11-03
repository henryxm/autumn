package cn.org.autumn.config;

/**
 * 机器人 Handler 共部署择优顺序（数值越小越优先）。
 */
public final class BotHandlerOrder {

    /** account 进程内（直调 risk） */
    public static final int LOCAL = 100;

    /** im 等远程（经 RiskApi） */
    public static final int REMOTE = 200;

    private BotHandlerOrder() {
    }
}
