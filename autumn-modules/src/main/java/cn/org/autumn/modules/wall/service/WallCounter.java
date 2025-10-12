package cn.org.autumn.modules.wall.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.entity.RData;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class WallCounter<M extends BaseMapper<T>, T> extends ModuleService<M, T> implements LoopJob.OneMinute, LoopJob.OneDay {

    private final Map<String, RData> counter = new ConcurrentHashMap<>();

    protected abstract void save(String key, RData rData);

    protected abstract void refresh();

    protected abstract boolean has(String key);

    protected boolean create() {
        return false;
    }

    protected void create(String key) {
    }

    public void count(String key, RData rData) {
        if (counter.containsKey(key)) {
            if (null == rData)
                rData = new RData();
            if (null != counter.get(key))
                rData.setCount(counter.get(key).getCount() + 1);
            counter.replace(key, rData);
        } else {
            if (null == rData) {
                rData = new RData();
                rData.setCount(1);
            }
            counter.put(key, rData);
        }
    }

    public void count() {
        Iterator<Map.Entry<String, RData>> iterator = counter.entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<String, RData> entry = iterator.next();
            if (create() && !has(entry.getKey())) {
                create(entry.getKey());
            }
            save(entry.getKey(), entry.getValue());
            iterator.remove();
        }
    }

    @Override
    public void onOneMinute() {
        try {
            count();
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("Wall Counter:" + getClass().getSimpleName() + ", Exception:" + e.getClass().getSimpleName() + ", Msg:" + e.getMessage());
        }
    }

    @Override
    public void onOneDay() {
        refresh();
    }
}