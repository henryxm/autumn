package cn.org.autumn.modules.wall.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.utils.IPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.wall.dao.ShieldDao;
import cn.org.autumn.modules.wall.entity.ShieldEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

@Service
public class ShieldService extends ModuleService<ShieldDao, ShieldEntity> implements LoopJob.OneMinute, LoopJob.TenMinute {

    Logger log = LoggerFactory.getLogger(getClass());

    static boolean print = false;

    static Set<String> uris = null;

    static Set<String> ips = new HashSet<>();

    public boolean print() {
        print = !print;
        return print;
    }

    public void put(String ip) {
        ips.add(ip);
    }

    public boolean shield(HttpServletRequest request) {
        if (null == uris)
            uris = baseMapper.gets();
        if (uris.isEmpty())
            return false;
        if (uris.contains(request.getRequestURI())) {
            String ip = IPUtils.getIp(request);
            if (!ips.contains(ip)) {
                if (print)
                    log.info("防御拦截:{}", ip);
                return true;
            } else {
                if (print)
                    log.info("防御放行:{}", ip);
                return false;
            }
        }
        return false;
    }

    @Override
    public void onOneMinute() {
        uris = null;
    }

    @Override
    public void onTenMinute() {
        ips.clear();
    }
}
