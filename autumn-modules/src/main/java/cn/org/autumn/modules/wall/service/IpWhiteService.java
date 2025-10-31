package cn.org.autumn.modules.wall.service;

import cn.org.autumn.config.ClearHandler;
import cn.org.autumn.modules.wall.dao.IpWhiteDao;
import cn.org.autumn.modules.wall.entity.RData;
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.site.WallFactory;
import cn.org.autumn.utils.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

@Slf4j
@Service
public class IpWhiteService extends WallCounter<IpWhiteDao, IpWhiteEntity> implements InitFactory.Init, LoadFactory.Load, LoopJob.OneMinute, ClearHandler {

    private List<String> ipWhiteList = new ArrayList<>();

    private List<String> ipWhiteSectionList = new ArrayList<>();

    @Autowired
    WallFactory wallFactory;

    public List<String> getIpWhiteList() {
        return ipWhiteList;
    }

    public void removeIpWhite(String ip) {
        ipWhiteList.remove(ip);
    }

    public void removeByIp(String ip) {
        ipWhiteList.remove(ip);
        baseMapper.deleteById(getByIp(ip).getId());
    }

    public boolean hasTag(String tag) {
        return baseMapper.hasTag(tag) > 0;
    }

    /**
     * 为了提高效率，在黑客大量攻击的时候，不能频繁进行数据库访问，通过定时器定时加载IP地址黑名单数据，提高效率。
     */
    public void load() {
        try {
            ipWhiteList = baseMapper.getIps(0);
            List<String> tmpSection = new ArrayList<>();
            for (String ip : ipWhiteList) {
                if (StringUtils.isEmpty(ip))
                    continue;
                if (ip.contains("/")) {
                    tmpSection.add(ip);
                }
            }
            ipWhiteSectionList = tmpSection;
        } catch (Exception e) {
            log.error("加载IP白名单数据出错：{}", e.getMessage());
        }
    }

    public void put(String ip) {
        if (!ipWhiteList.contains(ip))
            ipWhiteList.add(ip);
    }

    public IpWhiteEntity getByIp(String ip) {
        return baseMapper.getByIp(ip);
    }

    public boolean hasIp(String ip) {
        return 0 < baseMapper.hasIp(ip);
    }

    public boolean isWhite(String ip) {
        if (ipWhiteList.contains(ip)) {
            return true;
        }
        for (String section : ipWhiteSectionList) {
            boolean is = IPUtils.isInRange(ip, section);
            if (is) {
                return true;
            }
        }
        return false;
    }

    public boolean isWhite(String ip, String agent) {
        try {
            if (!wallFactory.isIpWhiteEnable())
                return true;
            if (StringUtils.isEmpty(ip))
                return false;
            if (ipWhiteList.contains(ip)) {
                RData rData = new RData();
                rData.setUserAgent(agent);
                count(ip, rData);
                return true;
            }
            for (String section : ipWhiteSectionList) {
                boolean is = IPUtils.isInRange(ip, section);
                if (is) {
                    RData rData = new RData();
                    rData.setUserAgent(agent);
                    count(section, rData);
                    return true;
                }
            }
            if (hasIp(ip)) {
                put(ip);
                RData rData = new RData();
                rData.setUserAgent(agent);
                count(ip, rData);
                return true;
            }
            return false;
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("无法判断IP白名单：{}", e.getMessage());
            return false;
        }
    }

    @Override
    public String ico() {
        return "fa-list-ul";
    }

    @Override
    public void onOneMinute() {
        clear();
    }

    public IpWhiteEntity create(String ip, String tag, String description) {
        return create(ip, tag, description, "", false);
    }

    public IpWhiteEntity create(String ip, String tag, String description, boolean update) {
        return create(ip, tag, description, "", update);
    }

    public IpWhiteEntity create(String ip, String tag, String description, String userAgent, boolean update) {
        IpWhiteEntity whiteEntity = null;
        if ((IPUtils.isIp(ip) || IPUtils.isIPV6(ip)) && !IPUtils.isInternalKeepIp(ip)) {
            try {
                whiteEntity = getByIp(ip);
                if (null == whiteEntity) {
                    whiteEntity = new IpWhiteEntity();
                    whiteEntity.setIp(ip);
                    whiteEntity.setTag(tag);
                    whiteEntity.setUserAgent(userAgent);
                    whiteEntity.setDescription(description);
                    whiteEntity.setCreateTime(new Date());
                    whiteEntity.setForbidden(0);
                    whiteEntity.setCount(0L);
                    whiteEntity.setToday(0L);
                    insert(whiteEntity);
                } else {
                    whiteEntity.setTag(tag);
                    whiteEntity.setUserAgent(userAgent);
                    whiteEntity.setDescription(description);
                    whiteEntity.setUpdateTime(new Date());
                    whiteEntity.setCount(whiteEntity.getCount() + 1);
                    whiteEntity.setToday(whiteEntity.getToday() + 1);
                    updateById(whiteEntity);
                }
                put(ip);
            } catch (Exception e) {
                log.error("添加白名单IP:{}", e.getMessage());
            }
        }
        return whiteEntity;
    }

    private void addLocal() throws IOException {
        String hostname = InetAddress.getLocalHost().getHostName();
        InetAddress[] addresses = InetAddress.getAllByName(hostname);
        for (InetAddress address : addresses) {
            if (!address.isReachable(2000))
                continue;
            create(address.getHostAddress(), "localhost", "本地IP地址(自动)");
        }
    }

    @Override
    protected void save(String key, RData rData) {
        baseMapper.count(key, rData.getUserAgent(), rData.getCount());
    }

    @Override
    public void refresh() {
        baseMapper.refresh();
    }

    @Override
    public void clear() {
        if (wallFactory.isIpWhiteEnable())
            load();
    }

    @Override
    protected boolean has(String key) {
        return hasIp(key);
    }

    @Override
    public void init() {
        create("127.0.0.1", "", "");
        create(IPUtils.getIp(), "", "");
    }
}