package cn.org.autumn.site;

import org.springframework.stereotype.Component;

@Component
public class LoadFactory extends Factory {

    public interface Before {
        void before();
    }

    public interface Load {
        void load();
    }

    public interface After {
        void after();
    }

    public interface Post {
        void post();
    }

    public void load() {
        invoke(Before.class, "before");
        invoke(Load.class, "load");
        invoke(After.class, "after");
        invoke(Post.class, "post");
    }
}
