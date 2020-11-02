package cn.org.autumn.modules.wall.service;

import cn.org.autumn.config.PostLoad;
import cn.org.autumn.config.PostLoadFactory;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import cn.org.autumn.modules.wall.service.gen.IpWhiteServiceGen;
import cn.org.autumn.utils.IPUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class IpWhiteService extends IpWhiteServiceGen implements PostLoad, LoopJob.Job {

    private static final Logger log = LoggerFactory.getLogger(IpWhiteService.class);

    private List<String> ipWhiteList;
    private List<String> ipWhiteSectionList;
    @Autowired
    PostLoadFactory postLoadFactory;

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

    public boolean isWhite(String ip) {
        try {
            if (StringUtils.isEmpty(ip))
                return false;

            if (null != ipWhiteList && ipWhiteList.size() > 0) {
                if (ipWhiteList.contains(ip))
                    return true;
            }

            /**
             * 如果IP在白名单段校验
             */
            if (null != ipWhiteSectionList && ipWhiteSectionList.size() > 0) {
                for (String section : ipWhiteSectionList) {
                    boolean is = IPUtils.isInRange(ip, section);
                    if (is)
                        return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("无法判断IP白名单：", e);
            return false;
        }
    }

    @Override
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-list-ul";
    }

    public void init() {
        super.init();
        postLoadFactory.register(this);
        LoopJob.onOneMinute(this);
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("wall_ipwhite_table_comment", "IP白名单", "IP black list");
        languageService.addLanguageColumnItem("wall_ipwhite_column_id", "id");
        languageService.addLanguageColumnItem("wall_ipwhite_column_ip", "IP地址", "IP address");
        languageService.addLanguageColumnItem("wall_ipwhite_column_count", "访问次数", "Visit count");
        languageService.addLanguageColumnItem("wall_ipwhite_column_forbidden", "禁用", "Forbidden");
        languageService.addLanguageColumnItem("wall_ipwhite_column_tag", "标签说明", "Tag");
        languageService.addLanguageColumnItem("wall_ipwhite_column_description", "描述信息", "Description");
        languageService.addLanguageColumnItem("wall_ipwhite_column_create_time", "创建时间", "Create time");
        languageService.addLanguageColumnItem("wall_ipwhite_column_update_time", "更新时间", "Update time");
        super.addLanguageColumnItem();
    }

    @Override
    public void runJob() {
        load();
    }
}
