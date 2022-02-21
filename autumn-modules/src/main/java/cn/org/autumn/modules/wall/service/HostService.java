package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.dao.HostDao;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.modules.wall.entity.HostEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HostService extends WallCounter<HostDao, HostEntity> implements LoadFactory.Load, LoopJob.FiveSecond {

    private static final Logger log = LoggerFactory.getLogger(HostService.class);

    /**
     * 黑名单列表
     */
    private List<String> blackHostList = new ArrayList<>();

    public void load() {
        blackHostList = baseMapper.getHosts(1);
    }

    // 主机访问黑名单
    public boolean isBlack(String host) {
        try {
            if (blackHostList.contains(host)) {
                count(host, "");
                return true;
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

    public boolean hasHost(String host) {
        Integer integer = baseMapper.hasHost(host);
        return null != integer && integer > 0;
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
    public void onFiveSecond() {
        load();
    }

    @Override
    protected void count(String key, String userAgent, String host, Integer count) {
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