package cn.org.autumn.site;

import cn.org.autumn.table.TableInit;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

@Component
public class InitFactory extends Factory {

    @Autowired
    private TableInit tableInit;

    //在初始化数据前执行
    public interface Before {
        void before();
    }

    //初始化
    public interface Init {
        void init();
    }

    //在初始化完成后执行
    public interface After {
        void after();
    }

    //在After后执行
    public interface Post {
        void post();
    }

    public void init() {
        if (!tableInit.init)
            return;
        invoke(Before.class, "before");
        invoke(Init.class, "init");
        invoke(After.class, "after");
        invoke(Post.class, "post");
    }
}