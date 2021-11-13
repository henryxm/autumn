package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.wall.dao.HostDao;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.entity.HostEntity;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HostService extends WallCounter<HostDao, HostEntity> implements LoadFactory.Load, LoopJob.Job {

    private static final Logger log = LoggerFactory.getLogger(HostService.class);

    /**
     * 黑名单列表
     */
    private List<String> blackHostList;

    public void load() {
        try {
            List<String> tmp = new ArrayList<>();
            List<HostEntity> cache = baseMapper.selectByMap(new HashMap<>());
            Map<String, HostEntity> eMap = new HashMap<>();
            if (null != cache && cache.size() > 0) {
                for (HostEntity hostEntity : cache) {
                    if (StringUtils.isNotEmpty(hostEntity.getHost()) && !tmp.contains(hostEntity.getHost())) {
                        Integer forbidden = hostEntity.getForbidden();
                        if (null != forbidden && 1 == forbidden)
                            tmp.add(hostEntity.getHost().toLowerCase());
                    }
                    eMap.put(hostEntity.getHost().toLowerCase(), hostEntity);
                }
            }
            blackHostList = tmp;
        } catch (Exception e) {
            log.error("加载HOST列表出错：", e);
        }
    }

    /**
     * 主机访问黑名单
     *
     * @param host
     * @return
     */
    public boolean isBlack(String host) {
        try {
            if (null != blackHostList && blackHostList.size() > 0 && StringUtils.isNotEmpty(host)) {
                if (blackHostList.contains(host)) {
                    count(host);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("无法判断HOST黑名单：", e);
            return false;
        }
    }

    @Override
    public String ico() {
        return "fa-ioxhost";
    }

    public void init() {
        super.init();
        LoopJob.onOneMinute(this);
    }

    public boolean hasHost(String host) {
        Integer integer = baseMapper.hasHost(host);
        if (null != integer && integer > 0)
            return true;
        return false;
    }

    public HostEntity getByHost(String host) {
        return baseMapper.getByHost(host);
    }

    public HostEntity create(String host, String tag, String description) {
        HostEntity hostEntity = null;
        try {
            hostEntity = getByHost(host);
            if (null == hostEntity) {
                hostEntity = new HostEntity();
                hostEntity.setHost(host);
                hostEntity.setCount(0L);
                hostEntity.setForbidden(0);
                hostEntity.setTag(tag);
                hostEntity.setCreateTime(new Date());
                hostEntity.setDescription(description);
                insert(hostEntity);
            }
        } catch (Exception e) {
            //do nothing
        }
        return hostEntity;
    }

    @Override
    public void runJob() {
        load();
    }

    @Override
    protected void count(String key, Integer count) {
        baseMapper.count(key, count);
    }

    @Override
    protected void clear() {
        baseMapper.clear();
    }

    @Override
    protected boolean has(String key) {
        return hasHost(key);
    }

    @Override
    protected boolean create() {
        return true;
    }

    @Override
    protected void create(String key) {
        create(key, "", "自动写入");
    }
}