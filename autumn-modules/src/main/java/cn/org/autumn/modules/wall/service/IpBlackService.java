package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.wall.dao.IpBlackDao;
import cn.org.autumn.modules.wall.entity.RData;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.site.WallFactory;
import cn.org.autumn.utils.IPUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IpBlackService extends WallCounter<IpBlackDao, IpBlackEntity> implements LoadFactory.Load, LoopJob.FiveSecond {
    private static final Logger log = LoggerFactory.getLogger(IpBlackService.class);

    @Autowired
    IpWhiteService ipWhiteService;

    @Autowired
    WallFactory wallFactory;

    @Autowired
    @Lazy
    ShieldService shieldService;

    /**
     * 一个ip地址统计刷新周期内，ip访问次数大于该值后，把ip地址加入到黑名单
     */
    private int lastCount = 500;

    /**
     * 缓存的黑名单列表，一个刷新周期内，从数据库加载一次
     */
    private Set<String> ipBlackList = new HashSet<>();

    private Set<String> ipBlackSectionList = new HashSet<>();

    /**
     * 一个ip地址统计刷新周期内，统计所有的ip地址及其访问的次数
     */
    private static Map<String, Integer> allIp;

    public Map<String, Integer> getAllIp() {
        return allIp;
    }

    /**
     * 为了提高效率，在黑客大量攻击的时候，不能频繁进行数据库访问，通过定时器定时加载IP地址黑名单数据，提高效率。
     */
    public void load() {
        try {
            ipBlackList = baseMapper.getIps(0);
            Set<String> tmpSection = new HashSet<>();
            for (String ip : ipBlackList) {
                if (ip.contains("/")) {
                    tmpSection.add(ip);
                }
            }
            ipBlackSectionList = tmpSection;
        } catch (Exception e) {
            log.error("加载IP黑名单数据出错：", e);
        }
    }

    public boolean isBlack(String ip) {
        if (ipBlackList.contains(ip)) {
            return true;
        }
        for (String section : ipBlackSectionList) {
            boolean is = IPUtils.isInRange(ip, section);
            if (is) {
                return true;
            }
        }
        return false;
    }

    public boolean isBlack(String ip, String agent) {
        try {
            if (!wallFactory.isIpBlackEnable())
                return false;
            if (StringUtils.isBlank(ip))
                return false;
            if (ipBlackList.contains(ip)) {
                RData rData = new RData();
                rData.setUserAgent(agent);
                count(ip, rData);
                return true;
            }
            for (String section : ipBlackSectionList) {
                boolean is = IPUtils.isInRange(ip, section);
                if (is) {
                    RData rData = new RData();
                    rData.setUserAgent(agent);
                    count(section, rData);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("无法判断IP黑名单:{}", e.getMessage());
            return false;
        }
    }

    /**
     * 根据IP被访问的次数进行过滤
     * equal = 0 表示，只获取等于多少次数的ip
     * equal = -1 表示获取，小于某一个次数的ip
     * equal = 1 表示获取大于某一个次数的ip
     *
     * @param count
     * @param equal
     * @return
     */
    public static Map<String, Integer> getAllIp(Integer count, Integer equal) {
        if (0 == count) {
            return allIp;
        }
        Map<String, Integer> map = new HashMap<>();
        if (null != allIp && !allIp.isEmpty()) {
            for (Map.Entry<String, Integer> kv : allIp.entrySet()) {
                if (0 == equal) {
                    if (Objects.equals(kv.getValue(), count)) {
                        map.put(kv.getKey(), kv.getValue());
                    }
                } else if (equal == -1) {
                    if (kv.getValue() < count) {
                        map.put(kv.getKey(), kv.getValue());
                    }
                } else {
                    if (kv.getValue() > count) {
                        map.put(kv.getKey(), kv.getValue());
                    }
                }
            }
        }
        return map;
    }

    /**
     * 对ip地址访问进行计数
     *
     * @param ip
     */
    public void countIp(String ip, String agent) {
        if (StringUtils.isEmpty(ip))
            return;
        if (null == allIp)
            allIp = new HashMap<>();
        if (allIp.containsKey(ip)) {
            Integer count = allIp.get(ip);
            if (null == count)
                return;
            count++;
            allIp.replace(ip, count);
            if (count > lastCount) {
                saveBlackIp(ip, agent, count, "触发IP黑名单策略:" + lastCount + "次/5秒");
            }
        } else
            allIp.put(ip, 1);
    }

    /**
     * 保存ip地址到黑名单
     *
     * @param ip
     * @return
     */
    public boolean saveBlackIp(String ip, String agent, Integer count, String tag) {
        try {
            if (ipWhiteService.isWhite(ip, agent))
                return false;
            if (isBlack(ip, agent)) {
                IpBlackEntity ipBlackEntity = baseMapper.getByIp(ip);
                if (null != ipBlackEntity) {
                    Long c = ipBlackEntity.getCount();
                    c = c + count;
                    ipBlackEntity.setCount(c);
                    ipBlackEntity.setUpdateTime(new Date());
                    updateById(ipBlackEntity);
                }
                return false;
            }
            IpBlackEntity ipBlackEntity = new IpBlackEntity();
            ipBlackEntity.setIp(ip);
            ipBlackEntity.setCount(count.longValue());
            ipBlackEntity.setAvailable(0);
            ipBlackEntity.setTag(tag);
            ipBlackEntity.setCreateTime(new Date());
            insertOrUpdate(ipBlackEntity);
            if (!ipBlackList.contains(ip)) {
                ipBlackList.add(ip);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 定时清空对ip地址的检测
     */
    public void refresh() {
        lastCount = getLastCount();
        if (null != allIp)
            allIp.clear();
    }

    public int getLastCount() {
        IpBlackEntity black = getByIp("0.0.0.0");
        if (null == black) {
            black = selectById(1);
            if (null == black) {
                black = new IpBlackEntity();
                black.setAvailable(1);
            }
            black.setIp("0.0.0.0");
            black.setCount(500L);
            black.setTag("IP黑名单策略5秒自动拉黑次数");
            black.setCreateTime(new Date());
            black.setUserAgent("");
            insertOrUpdate(black);
        }
        if (null == black.getCount() || black.getCount().intValue() < 50) {
            black.setCount(500L);
            black.setTag("IP黑名单策略5秒自动拉黑次数");
            black.setUserAgent("");
            updateById(black);
        }
        return black.getCount().intValue();
    }

    @Override
    public String ico() {
        return "fa-list-alt";
    }

    public boolean hasIp(String ip) {
        Integer integer = baseMapper.hasIp(ip);
        if (null != integer && integer > 0)
            return true;
        return false;
    }

    public IpBlackEntity getByIp(String ip) {
        return baseMapper.getByIp(ip);
    }

    public IpBlackEntity create(String ip, String tag, String description) {
        IpBlackEntity blackEntity = null;
        try {
            blackEntity = getByIp(ip);
            if (null == blackEntity) {
                blackEntity = new IpBlackEntity();
                blackEntity.setIp(ip);
                blackEntity.setTag(tag);
                blackEntity.setDescription(description);
                blackEntity.setCreateTime(new Date());
                blackEntity.setAvailable(0);
                blackEntity.setCount(0L);
                blackEntity.setToday(0L);
                if (!ipBlackList.contains(ip)) {
                    ipBlackList.add(ip);
                }
                insert(blackEntity);
            }
        } catch (Exception e) {
            //do nothing
        }
        return blackEntity;
    }

    @Override
    public void onFiveSecond() {
        if (wallFactory.isIpBlackEnable()) {
            refresh();
            load();
        }
    }

    @Override
    protected void save(String key, RData rData) {
        baseMapper.count(key, rData.getUserAgent(), rData.getCount());
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