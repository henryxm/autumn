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
public class IpWhiteService extends WallCounter<IpWhiteDao, IpWhiteEntity> implements LoadFactory.Load, LoopJob.Job {

    private static final Logger log = LoggerFactory.getLogger(IpWhiteService.class);

    private List<String> ipWhiteList;
    private List<String> ipWhiteSectionList;

    /**
     * 为了提高效率，在黑客大量攻击的时候，不能频繁进行数据库访问，通过定时器定时加载IP地址黑名单数据，提高效率。
     */
    public void load() {
        try {
            List<String> tmp = new ArrayList<>();
            List<String> tmpSection = new ArrayList<>();
            List<IpWhiteEntity> cache = baseMapper.selectByMap(new HashMap<>());
            if (null != cache && cache.size() > 0) {
                for (IpWhiteEntity ipWhiteEntity : cache) {
                    if (StringUtils.isEmpty(ipWhiteEntity.getIp()))
                        continue;
                    String ip = ipWhiteEntity.getIp();
                    if (StringUtils.isNotEmpty(ip) && !tmp.contains(ip)) {
                        Integer forbidden = ipWhiteEntity.getForbidden();
                        if (null == forbidden || 0 == forbidden) {
                            if (ip.contains("/")) {
                                tmpSection.add(ip);
                            } else {
                                tmp.add(ip);
                            }
                        }
                    }
                }
            }
            ipWhiteList = tmp;
            ipWhiteSectionList = tmpSection;
        } catch (Exception e) {
            log.error("加载IP白名单数据出错：", e);
        }
    }

    public void put(String ip) {
        if (null == ipWhiteList) {
            ipWhiteList = new ArrayList<>();
            ipWhiteList.add(ip);
        } else {
            if (!ipWhiteList.contains(ip))
                ipWhiteList.add(ip);
        }
    }

    public IpWhiteEntity getByIp(String ip) {
        return baseMapper.getByIp(ip);
    }

    public boolean hasIp(String ip) {
        Integer integer = baseMapper.hasIp(ip);
        if (null != integer && integer > 0)
            return true;
        return false;
    }

    public boolean isWhite(String ip) {
        try {
            if (StringUtils.isEmpty(ip))
                return false;

            if (null != ipWhiteList && ipWhiteList.size() > 0) {
                if (ipWhiteList.contains(ip)) {
                    count(ip);
                    return true;
                }
            }

            /**
             * 如果IP在白名单段校验
             */
            if (null != ipWhiteSectionList && ipWhiteSectionList.size() > 0) {
                for (String section : ipWhiteSectionList) {
                    boolean is = IPUtils.isInRange(ip, section);
                    if (is) {
                        count(section);
                        return true;
                    }
                }
            }
            if (hasIp(ip)) {
                put(ip);
                count(ip);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("无法判断IP白名单：", e);
            return false;
        }
    }

    @Override
    public String ico() {
        return "fa-list-ul";
    }

    public void init() {
        super.init();
        LoopJob.onOneMinute(this);
        try {
            addLocal();
        } catch (IOException e) {
            log.debug("addLocal:", e);
        }
    }

    @Override
    public void runJob() {
        load();
        count();
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
    protected void count(String key, Integer count) {
        baseMapper.count(key, count);
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