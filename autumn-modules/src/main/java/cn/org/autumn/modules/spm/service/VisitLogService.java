package cn.org.autumn.modules.spm.service;

import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import cn.org.autumn.modules.spm.entity.VisitLogEntity;
import cn.org.autumn.modules.spm.service.gen.VisitLogServiceGen;

import cn.org.autumn.utils.IPUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VisitLogService extends VisitLogServiceGen implements LoopJob.OneDay {

    public Map<String, List<String>> map = new HashMap<>();

    private boolean isUv(HttpServletRequest request, SuperPositionModelEntity superPositionModelEntity) {
        String ip = IPUtils.getIp(request);
        if (StringUtils.isBlank(ip) || null == superPositionModelEntity)
            return false;
        if (map.containsKey(superPositionModelEntity.toSpmString())) {
            List<String> list = map.get(superPositionModelEntity.toSpmString());
            if (list.contains(ip))
                return false;
            else
                list.add(ip);
        } else {
            List<String> l = new ArrayList<>();
            l.add(ip);
            map.put(superPositionModelEntity.toSpmString(), l);
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

    @Override
    public int menuOrder() {
        return super.menuOrder();
    }

    @Override
    public String ico() {
        return "fa-universal-access";
    }

    @Override
    public void onOneDay() {
        map.clear();
    }
}
