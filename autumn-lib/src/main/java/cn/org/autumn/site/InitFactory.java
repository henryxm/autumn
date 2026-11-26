package cn.org.autumn.site;

import cn.org.autumn.config.InitHandler;
import cn.org.autumn.table.SysConfigSchemaGate;
import cn.org.autumn.table.TableInit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InitFactory extends Factory {

    @Autowired
    private TableInit tableInit;

    @Autowired
    private SysConfigSchemaGate schemaGate;

    private boolean done = false;

    List<InitHandler> initHandlers = null;

    //在初始化数据前执行
    public interface Before {
        @Order(DEFAULT_ORDER)
        void before();
    }

    //初始化
    public interface Init {
        @Order(DEFAULT_ORDER)
        void init();
    }

    //在初始化完成后执行
    public interface After {
        @Order(DEFAULT_ORDER)
        void after();
    }

    //在After后执行
    public interface Post {
        @Order(DEFAULT_ORDER)
        void post();
    }

    public interface Must {
        @Order(DEFAULT_ORDER)
        void must();
    }

    /**
     * Init 相位结束后再刷一次延后 put（由 SysConfigService 等实现）。
     * 保证早于 {@link Init} 入队、或 Init 中因瞬时缺表重新入队的种子在 After 前落库。
     */
    public interface ConfigFlush {
        @Order(DEFAULT_ORDER)
        void flushDeferredConfig();
    }

    public boolean isDone() {
        return done;
    }

    public List<InitHandler> getInitHandlers() {
        if (null == initHandlers)
            initHandlers = getOrderList(InitHandler.class);
        return initHandlers;
    }

    /**
     * 一票否决逻辑，只要有一个实例返回false,即不执行
     *
     * @return
     */
    public boolean canBefore() {
        if (null != getInitHandlers() && !getInitHandlers().isEmpty()) {
            for (InitHandler initHandler : getInitHandlers()) {
                if (!initHandler.canBefore())
                    return false;
            }
        }
        return true;
    }

    public boolean canInit() {
        if (null != getInitHandlers() && !getInitHandlers().isEmpty()) {
            for (InitHandler initHandler : getInitHandlers()) {
                if (!initHandler.canInit())
                    return false;
            }
        }
        return true;
    }

    public boolean canAfter() {
        if (null != getInitHandlers() && !getInitHandlers().isEmpty()) {
            for (InitHandler initHandler : getInitHandlers()) {
                if (!initHandler.canAfter())
                    return false;
            }
        }
        return true;
    }

    public boolean canPost() {
        if (null != getInitHandlers() && !getInitHandlers().isEmpty()) {
            for (InitHandler initHandler : getInitHandlers()) {
                if (!initHandler.canPost())
                    return false;
            }
        }
        return true;
    }

    public void init() {
        if (!tableInit.isInit())
            return;
        // 防御 PostConstruct 未跑到；失败可重试（TableInit 未置 ensured）
        tableInit.ensureCreated();
        if (!schemaGate.isSchemaReady()) {
            // 兼容旧行为：autumn.table.init=true 时仍执行 Init；缺表由 SysConfig soft-defer 兜底，不硬阻断
            log.warn("InitFactory: schema not marked ready after ensureCreated; continuing Init with SysConfig soft-defer");
        }
        if (canBefore())
            invoke(Before.class, "before");
        if (canInit())
            invoke(Init.class, "init");
        invoke(ConfigFlush.class, "flushDeferredConfig");
        if (canAfter())
            invoke(After.class, "after");
        if (canPost())
            invoke(Post.class, "post");
        invoke(Must.class, "must");
        done = true;
    }
}
