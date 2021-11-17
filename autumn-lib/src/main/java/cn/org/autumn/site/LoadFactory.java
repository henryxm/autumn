package cn.org.autumn.site;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

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

    public boolean isDone() {
        return done;
    }

    public void load() {
        invoke(Before.class, "before");
        invoke(Load.class, "load");
        invoke(After.class, "after");
        invoke(Post.class, "post");
        done = true;
    }
}
