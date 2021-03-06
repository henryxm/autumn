package cn.org.autumn.modules.wall.service;

import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.entity.UrlBlackEntity;
import cn.org.autumn.modules.wall.service.gen.UrlBlackServiceGen;
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
public class UrlBlackService extends UrlBlackServiceGen implements LoadFactory.Load, LoopJob.Job {

    private static final Logger log = LoggerFactory.getLogger(UrlBlackService.class);

    /**
     * URL 访问计数统计
     */
    private Map<Integer, Integer> allUrls;

    /**
     * URL 黑名单列表
     */
    private List<String> blackUrls;

    /**
     * 每个周期的URL访问数大于改值后，将其对应的IP地址拉入黑名单
     */
    public static Integer lastCount = 30;

    @Autowired
    IpBlackService ipBlackService;

    /**
     * 为了提高效率，在黑客大量攻击的时候，不能频繁进行数据库访问，通过定时器定时加载IP地址黑名单数据，提高效率。
     */
    public void load() {
        try {
            List<String> tmp = new ArrayList<>();
            List<UrlBlackEntity> cache = baseMapper.selectByMap(new HashMap<>());
            if (null != cache && cache.size() > 0) {
                for (UrlBlackEntity urlBlackEntity : cache) {
                    if (StringUtils.isNotEmpty(urlBlackEntity.getUrl()) && !tmp.contains(urlBlackEntity.getUrl())) {
                        tmp.add(urlBlackEntity.getUrl());
                    }
                }
            }
            blackUrls = tmp;
        } catch (Exception e) {
            log.error("加载URL黑名单数据出错：", e);
        }
    }

    public boolean isBlack(String url) {
        try {
            if (null != blackUrls && blackUrls.size() > 0 && StringUtils.isNotEmpty(url)) {
                if (blackUrls.contains(url))
                    return true;
            }
            return false;
        } catch (Exception e) {
            log.error("无法判断URL黑名单：", e);
            return false;
        }
    }

    public void countUrl(String url, String ip) {
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
                    ipBlackService.saveBlackIp(ip, 0, "触发URL黑名单策略");
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
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-th-list";
    }

    public void init() {
        super.init();
        LoopJob.onThreeSecond(this);
    }

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {"wall_urlblack_table_comment", "链接黑名单", "URL black list"},
                {"wall_urlblack_column_id", "id"},
                {"wall_urlblack_column_url", "URL地址", "URL address"},
                {"wall_urlblack_column_count", "访问次数", "Visit count"},
                {"wall_urlblack_column_forbidden", "禁用", "Forbidden"},
                {"wall_urlblack_column_tag", "标签说明", "Tag"},
                {"wall_urlblack_column_description", "描述信息", "Description"},
        };
        return items;
    }

    @Override
    public void runJob() {
        refresh(30);
    }
}
