package cn.org.autumn.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 无 Session API 调用的统一用户上下文（精简字段）。
 * <ul>
 *   <li>{@link #uuid}：当前调用者（真人或机器人）</li>
 *   <li>{@link #owner}：数据权限主体（真人为自身 uuid，机器人为主人 uuid）</li>
 *   <li>{@link #robot}：是否机器人调用</li>
 * </ul>
 * 控制器直接声明本类型参数即可注入；是否必填由 {@link cn.org.autumn.annotation.Authenticated} 控制。
 */
@Getter
@Setter
public class UserContext implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 调用者业务 uuid */
    private String uuid;

    /** 数据归属 uuid（权限过滤、业务 FK 默认用此字段） */
    private String owner;

    /** 是否机器人 */
    private boolean robot;

    /** 展示昵称 */
    private String nickname;

    /** 头像 */
    private String icon;

    /** 账号状态（与 sys_user / bot_robot 一致） */
    private int status;

    /**
     * 数据权限主体，等价于 {@link #getOwner()}。
     */
    public String getSubject() {
        return owner;
    }

    public boolean isActive() {
        return status >= 1;
    }
}
