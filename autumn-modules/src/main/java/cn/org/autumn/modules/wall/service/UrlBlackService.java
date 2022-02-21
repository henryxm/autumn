package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.wall.dao.UrlBlackDao;
import cn.org.autumn.modules.wall.entity.RData;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.entity.UrlBlackEntity;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UrlBlackService extends WallCounter<UrlBlackDao, UrlBlackEntity> implements LoadFactory.Load, LoopJob.FiveSecond {

    private static final Logger log = LoggerFactory.getLogger(UrlBlackService.class);

    /**
     * URL 访问计数统计
     */
    private Map<Integer, Integer> allUrls;

    /**
     * URL 黑名单列表
     */
    private List<String> blackUrls = new ArrayList<>();

    /**
     * 每个周期的URL访问数大于改值后，将其对应的IP地址拉入黑名单
     */
    public static Integer lastCount = 300;

    @Autowired
    IpBlackService ipBlackService;

    /**
     * 为了提高效率，在黑客大量攻击的时候，不能频繁进行数据库访问，通过定时器定时加载IP地址黑名单数据，提高效率。
     */
    public void load() {
        blackUrls = baseMapper.getUrls(1);
    }

    public boolean isBlack(String url) {
        if (blackUrls.contains(url)) {
            count(url, null);
            return true;
        }
        return false;
    }

    public void countUrl(String url, String ip, String agent) {
        try {
            if (StringUtils.isEmpty(ip))
                return;
            if (null == allUrls)
                allUrls = new HashMap<>();

            String urlIp = url + ip;
            /**
             * 计算URL的hashCode，提高对特定字符串的查找效率
             */
            Integer hashCode = urlIp.hashCode();

            if (allUrls.containsKey(hashCode)) {
                int count = allUrls.get(hashCode) + 1;
                allUrls.replace(hashCode, count);
                if (count > lastCount) {
                    ipBlackService.saveBlackIp(ip, agent, 0, "触发URL黑名单策略");
                    allUrls.replace(hashCode, 0);
                }
            } else
                allUrls.put(hashCode, 1);
        } catch (Exception e) {
            log.error("URL黑名单计数错误:", e);
        }
    }

    /**
     * 定时清空对ip地址的检测
     *
     * @param times
     */
    public void refresh(Integer times) {
        lastCount = times;
        if (null != allUrls)
            allUrls.clear();
    }

    @Override
    public String ico() {
        return "fa-th-list";
    }

    public UrlBlackEntity getByUrl(String url) {
        return baseMapper.getByUrl(url);
    }

    public boolean hasUrl(String url) {
        Integer integer = baseMapper.hasUrl(url);
        if (null != integer && integer > 0)
            return true;
        return false;
    }

    public UrlBlackEntity creat(String url, String tag) {
        UrlBlackEntity urlBlackEntity = null;
        try {
            urlBlackEntity = getByUrl(url);
            if (null == urlBlackEntity) {
                urlBlackEntity = new UrlBlackEntity();
                urlBlackEntity.setUrl(url);
                urlBlackEntity.setCount(0L);
                urlBlackEntity.setToday(0L);
                urlBlackEntity.setForbidden(1);
                urlBlackEntity.setTag(tag);
                insert(urlBlackEntity);
            }
        } catch (Exception e) {
            //do nothing
        }
        return urlBlackEntity;
    }

    @Override
    public void onFiveSecond() {
        refresh(500);
        load();
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
        return hasUrl(key);
    }
}