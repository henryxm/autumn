package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.wall.dao.IpWhiteDao;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.utils.IPUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

@Service
public class IpWhiteService extends WallCounter<IpWhiteDao, IpWhiteEntity> implements LoadFactory.Load, LoopJob.FiveSecond {

    private static final Logger log = LoggerFactory.getLogger(IpWhiteService.class);

    private List<String> ipWhiteList = new ArrayList<>();
    private List<String> ipWhiteSectionList = new ArrayList<>();

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

    public boolean isWhite(String ip, String agent) {
        try {
            if (StringUtils.isEmpty(ip))
                return false;
            if (ipWhiteList.contains(ip)) {
                count(ip, agent);
                return true;
            }
            for (String section : ipWhiteSectionList) {
                boolean is = IPUtils.isInRange(ip, section);
                if (is) {
                    count(section, agent);
                    return true;
                }
            }
            if (hasIp(ip)) {
                put(ip);
                count(ip, agent);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("无法判断IP白名单：{}", e.getMessage());
            return false;
        }
    }

    @Override
    public String ico() {
        return "fa-list-ul";
    }

    @Override
    public void onFiveSecond() {
        load();
    }

    public IpWhiteEntity create(String ip, String tag, String description) {
        IpWhiteEntity whiteEntity = null;
        try {
            whiteEntity = getByIp(ip);
            if (null == whiteEntity) {
                whiteEntity = new IpWhiteEntity();
                whiteEntity.setIp(ip);
                whiteEntity.setTag(tag);
                whiteEntity.setDescription(description);
                whiteEntity.setCreateTime(new Date());
                whiteEntity.setForbidden(0);
                whiteEntity.setCount(0L);
                whiteEntity.setToday(0L);
                insert(whiteEntity);
            }
            put(ip);
        } catch (Exception e) {
            //do nothing
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
    protected void count(String key, String userAgent, String host, Integer count) {
        baseMapper.count(key, userAgent, count);
    }

    @Override
    protected void clear() {
        baseMapper.clear();
    }

    @Override
    protected boolean has(String key) {
        return hasIp(key);
    }
}