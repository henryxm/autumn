package cn.org.autumn.modules.wall.service;

import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.entity.HostEntity;
import cn.org.autumn.modules.wall.service.gen.HostServiceGen;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HostService extends HostServiceGen implements LoadFactory.Load, LoopJob.Job {

    private static final Logger log = LoggerFactory.getLogger(HostService.class);

    /**
     * 黑名单列表
     */
    private List<String> blackHostList;

    /**
     * 主机访问统计
     */
    private Map<String, Integer> hosts;

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

            /**
             * 将HOST的访问次数更新到数据库
             */
            if (null != hosts && hosts.size() > 0) {
                for (Map.Entry<String, Integer> kv : hosts.entrySet()) {
                    String h = kv.getKey().toLowerCase();
                    Integer c = kv.getValue();
                    if (eMap.containsKey(h)) {
                        HostEntity hostEntity = eMap.get(h);
                        Long cc = 0L;
                        if (null != hostEntity.getCount())
                            cc = hostEntity.getCount();
                        cc = cc + c;
                        hostEntity.setCount(cc);
                        updateById(hostEntity);
                    } else {
                        try {
                            if (StringUtils.isNotEmpty(h)) {
                                HostEntity hostEntity = new HostEntity();
                                hostEntity.setHost(h);
                                hostEntity.setCount(c.longValue());
                                hostEntity.setDescription("程序自动写入");
                                hostEntity.setForbidden(0);
                                hostEntity.setTag(h);
                                insert(hostEntity);
                            }
                        } catch (Exception e) {
                            log.error("Host增加失败", e);
                        }
                    }
                    hosts.replace(h, 0);
                }
            }
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
                if (blackHostList.contains(host))
                    return true;
            }
            return false;
        } catch (Exception e) {
            log.error("无法判断HOST黑名单：", e);
            return false;
        }
    }

    public void countHost(String host) {
        if (StringUtils.isEmpty(host))
            return;
        host = host.trim().toLowerCase();
        if (null == hosts)
            hosts = new HashMap<>();
        if (hosts.containsKey(host)) {
            Integer count = hosts.get(host);
            count++;
            hosts.replace(host, count);
        } else
            hosts.put(host, 1);
    }

    @Override
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-ioxhost";
    }

    public void init() {
        super.init();
        LoopJob.onOneMinute(this);
    }

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {"wall_host_table_comment", "主机统计", "Host Visit"},
                {"wall_host_column_id", "id"},
                {"wall_host_column_host", "主机地址", "Host address"},
                {"wall_host_column_count", "访问次数", "Visit count"},
                {"wall_host_column_forbidden", "禁用", "Forbidden"},
                {"wall_host_column_tag", "标签说明", "Tag"},
                {"wall_host_column_description", "描述信息", "Description"},
        };
        return items;
    }

    @Override
    public void runJob() {
        load();
    }
}
