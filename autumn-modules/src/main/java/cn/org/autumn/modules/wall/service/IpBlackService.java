package cn.org.autumn.modules.wall.service;

import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.wall.service.gen.IpBlackServiceGen;
import cn.org.autumn.utils.IPUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IpBlackService extends IpBlackServiceGen implements LoadFactory.Load, LoopJob.Job {
    private static final Logger log = LoggerFactory.getLogger(IpBlackService.class);

    /**
     * 一个ip地址统计刷新周期内，ip访问次数大于该值后，把ip地址加入到黑名单
     */
    private int lastCount = 150;

    /**
     * 缓存的黑名单列表，一个刷新周期内，从数据库加载一次
     */
    private List<String> ipBlackList;

    private List<String> ipBlackSectionList;

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
            List<String> tmp = new ArrayList<>();
            List<String> tmpSection = new ArrayList<>();
            List<IpBlackEntity> cache = baseMapper.selectByMap(new HashMap<>());
            if (null != cache && cache.size() > 0) {
                for (IpBlackEntity ipBlackEntity : cache) {
                    String ip = ipBlackEntity.getIp();
                    if (StringUtils.isNotEmpty(ip) && !tmp.contains(ip)) {
                        Integer available = ipBlackEntity.getAvailable();
                        if (null != available && 1 == available)
                            continue;
                        if (ip.contains("/")) {
                            tmpSection.add(ip);
                        } else {
                            tmp.add(ip);
                        }
                    }
                }
            }
            ipBlackList = tmp;
            ipBlackSectionList = tmpSection;
        } catch (Exception e) {
            log.error("加载IP黑名单数据出错：", e);
        }
    }

    public boolean isBlack(String ip) {
        try {
            if (null != ipBlackList && ipBlackList.size() > 0 && StringUtils.isNotEmpty(ip)) {
                if (ipBlackList.contains(ip))
                    return true;

                if (null != ipBlackSectionList && ipBlackSectionList.size() > 0) {
                    for (String section : ipBlackSectionList) {
                        boolean is = IPUtils.isInRange(ip, section);
                        if (is)
                            return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("无法判断IP黑名单：", e);
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
        if (null != allIp && allIp.size() > 0) {
            for (Map.Entry<String, Integer> kv : allIp.entrySet()) {
                if (0 == equal) {
                    if (kv.getValue() == count) {
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
    public void countIp(String ip) {
        if (StringUtils.isEmpty(ip))
            return;
        if (null == allIp)
            allIp = new HashMap<>();
        if (allIp.containsKey(ip)) {
            Integer count = allIp.get(ip);
            count++;
            allIp.replace(ip, count);
            if (count > lastCount) {
                saveBlackIp(ip, count, "触发IP黑名单策略");
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
    public boolean saveBlackIp(String ip, Integer count, String tag) {
        try {
            if (isBlack(ip)) {
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
     *
     * @param times
     */
    public void refresh(Integer times) {
        lastCount = times;
        if (null != allIp)
            allIp.clear();
    }

    @Override
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-list-alt";
    }

    public void init() {
        super.init();
        LoopJob.onFiveSecond(this);
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("wall_ipblack_table_comment", "IP黑名单", "IP black list");
        languageService.addLanguageColumnItem("wall_ipblack_column_id", "id");
        languageService.addLanguageColumnItem("wall_ipblack_column_ip", "IP地址", "IP address");
        languageService.addLanguageColumnItem("wall_ipblack_column_count", "访问次数", "Visit count");
        languageService.addLanguageColumnItem("wall_ipblack_column_available", "可用", "Available");
        languageService.addLanguageColumnItem("wall_ipblack_column_tag", "标签说明", "Tag");
        languageService.addLanguageColumnItem("wall_ipblack_column_description", "描述信息", "Description");
        languageService.addLanguageColumnItem("wall_ipblack_column_create_time", "创建时间", "Create time");
        languageService.addLanguageColumnItem("wall_ipblack_column_update_time", "更新时间", "Update time");
        super.addLanguageColumnItem();
    }

    @Override
    public void runJob() {
        refresh(150);
        load();
    }
}
