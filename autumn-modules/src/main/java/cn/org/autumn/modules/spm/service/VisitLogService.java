package cn.org.autumn.modules.spm.service;

import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import cn.org.autumn.modules.spm.entity.VisitLogEntity;
import cn.org.autumn.modules.spm.service.gen.VisitLogServiceGen;

import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.utils.IPUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VisitLogService extends VisitLogServiceGen implements LoopJob.Job {

    public Map<String, List<String>> map = new HashMap<>();

    private boolean isUv(HttpServletRequest request, SuperPositionModelEntity superPositionModelEntity) {
        String ip = IPUtils.getIp(request);
        if (map.containsKey(superPositionModelEntity.toString())) {
            List<String> list = map.get(superPositionModelEntity.toString());
            if (list.contains(ip))
                return false;
            else
                list.add(ip);
        } else {
            List l = new ArrayList();
            l.add(ip);
            map.put(superPositionModelEntity.toString(), l);
        }
        return true;
    }

    public void put(HttpServletRequest request, SuperPositionModelEntity superPositionModelEntity) {
        VisitLogEntity visitLogEntity = new VisitLogEntity();
        visitLogEntity.setUniqueVisitor(0);
        visitLogEntity.setPageView(0);
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(superPositionModelEntity.getProductId())) {
            map.put("product_id", superPositionModelEntity.getProductId());
            visitLogEntity.setProductId(superPositionModelEntity.getProductId());
        }
        if (StringUtils.isNotEmpty(superPositionModelEntity.getChannelId())) {
            map.put("channel_id", superPositionModelEntity.getChannelId());
            visitLogEntity.setChannelId(superPositionModelEntity.getChannelId());
        }
        if (StringUtils.isNotEmpty(superPositionModelEntity.getPageId())) {
            map.put("page_id", superPositionModelEntity.getPageId());
            visitLogEntity.setPageId(superPositionModelEntity.getPageId());
        }
        if (StringUtils.isNotEmpty(superPositionModelEntity.getSiteId())) {
            map.put("site_id", superPositionModelEntity.getSiteId());
            visitLogEntity.setSiteId(superPositionModelEntity.getSiteId());
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

        String day = simpleDateFormat.format(new Date());

        map.put("day_string", day);
        visitLogEntity.setDayString(day);
        visitLogEntity.setCreateTime(new Date());

        List<VisitLogEntity> logEntities = selectByMap(map);

        if (logEntities.size() > 0)
            visitLogEntity = logEntities.get(0);

        visitLogEntity.setPageView(visitLogEntity.getPageView() + 1);
        if (isUv(request, superPositionModelEntity))
            visitLogEntity.setUniqueVisitor(visitLogEntity.getUniqueVisitor() + 1);

        insertOrUpdate(visitLogEntity);
    }

    public void init() {
        super.init();
        LoopJob.onOneDay(this);
    }

    public void addLanguageColumnItem() {
        languageService.addLanguageColumnItem("spm_visitlog_table_comment", "访问统计", "Visit log");
        languageService.addLanguageColumnItem("spm_visitlog_column_id", "id");
        languageService.addLanguageColumnItem("spm_visitlog_column_site_id", "网站ID", "Site id");
        languageService.addLanguageColumnItem("spm_visitlog_column_page_id", "网页ID", "Page id");
        languageService.addLanguageColumnItem("spm_visitlog_column_channel_id", "频道ID", "Channel id");
        languageService.addLanguageColumnItem("spm_visitlog_column_product_id", "产品ID", " Product id");
        languageService.addLanguageColumnItem("spm_visitlog_column_unique_visitor", "独立访客(UV)", "Unique visitor");
        languageService.addLanguageColumnItem("spm_visitlog_column_page_view", "访问量(PV)", "Page view");
        languageService.addLanguageColumnItem("spm_visitlog_column_day_string", "当天", "Current day");
        languageService.addLanguageColumnItem("spm_visitlog_column_create_time", "创建时间", "Create time");
        super.addLanguageColumnItem();
    }

    @Override
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-universal-access";
    }

    @Override
    public void runJob() {
        map.clear();
    }
}
