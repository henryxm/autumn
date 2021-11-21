package cn.org.autumn.modules.wall.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.job.task.LoopJob;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class WallCounter<M extends BaseMapper<T>, T> extends ModuleService<M, T> implements LoopJob.OneMinute, LoopJob.OneDay {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, Integer> counter = new HashMap<>();

    protected abstract void count(String key, Integer count);

    protected abstract void clear();

    protected abstract boolean has(String key);

    protected boolean create() {
        return false;
    }

    protected void create(String key) {
    }

    public void count(String key) {
        if (counter.containsKey(key)) {
            counter.replace(key, counter.get(key) + 1);
        } else
            counter.put(key, 1);
    }

    public void count() {
        Iterator<Map.Entry<String, Integer>> iterator = counter.entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            if (create() && !has(entry.getKey())) {
                create(entry.getKey());
            }
            count(entry.getKey(), entry.getValue());
            iterator.remove();
        }
    }

    @Override
    public void onOneMinute() {
        try {
            count();
        } catch (Exception e) {
            log.error("Wall Counter:" + getClass().getSimpleName() + ", Exception:" + e.getClass().getSimpleName() + ", Msg:" + e.getMessage());
        }
    }

    @Override
    public void onOneDay() {
        clear();
    }
}