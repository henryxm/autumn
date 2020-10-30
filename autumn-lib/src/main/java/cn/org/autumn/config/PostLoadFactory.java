package cn.org.autumn.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PostLoadFactory {

    public List<PostLoad> loadList;

    public void register(PostLoad postLoad) {
        if (null == loadList)
            loadList = new ArrayList<>();
        if (null != postLoad)
            loadList.add(postLoad);
    }

    public void load() {
        for (PostLoad postLoad : loadList) {
            postLoad.load();
        }
    }
}
