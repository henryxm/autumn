package cn.org.autumn.site;

import cn.org.autumn.config.InitHandler;
import cn.org.autumn.config.LoadHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LoadFactory extends Factory {

    private boolean done = false;

    public interface Before {
        @Order(DEFAULT_ORDER)
        void before();
    }

    public interface Load {
        @Order(DEFAULT_ORDER)
        void load();
    }

    public interface After {
        @Order(DEFAULT_ORDER)
        void after();
    }

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

    public void load() {
        List<LoadHandler> loadHandlers = getOrderList(LoadHandler.class);
        LoadHandler loadHandler = null;
        if (null != loadHandlers && loadHandlers.size() > 0)
            loadHandler = loadHandlers.get(0);
        if (null == loadHandler || loadHandler.isBefore())
            invoke(Before.class, "before");
        if (null == loadHandler || loadHandler.isLoad())
            invoke(Load.class, "load");
        if (null == loadHandler || loadHandler.isAfter())
            invoke(After.class, "after");
        if (null == loadHandler || loadHandler.isPost())
            invoke(Post.class, "post");
        invoke(Must.class, "must");
        done = true;
    }
}
