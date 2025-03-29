package cn.org.autumn.site;

import cn.org.autumn.config.InitHandler;
import cn.org.autumn.table.TableInit;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InitFactory extends Factory {

    @Autowired
    private TableInit tableInit;

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
        if (null != getInitHandlers() && getInitHandlers().size() > 0) {
            for (InitHandler initHandler : getInitHandlers()) {
                if (!initHandler.canBefore())
                    return false;
            }
        }
        return true;
    }

    public boolean canInit() {
        if (null != getInitHandlers() && getInitHandlers().size() > 0) {
            for (InitHandler initHandler : getInitHandlers()) {
                if (!initHandler.canInit())
                    return false;
            }
        }
        return true;
    }

    public boolean canAfter() {
        if (null != getInitHandlers() && getInitHandlers().size() > 0) {
            for (InitHandler initHandler : getInitHandlers()) {
                if (!initHandler.canAfter())
                    return false;
            }
        }
        return true;
    }

    public boolean canPost() {
        if (null != getInitHandlers() && getInitHandlers().size() > 0) {
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
        if (canBefore())
            invoke(Before.class, "before");
        if (canInit())
            invoke(Init.class, "init");
        if (canAfter())
            invoke(After.class, "after");
        if (canPost())
            invoke(Post.class, "post");
        invoke(Must.class, "must");
        done = true;
    }
}