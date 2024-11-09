package cn.org.autumn.modules.wall.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.wall.site.WallSite;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.wall.dao.ShieldDao;
import cn.org.autumn.modules.wall.entity.ShieldEntity;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

@Service
public class ShieldService extends ModuleService<ShieldDao, ShieldEntity> implements LoopJob.OneMinute, LoopJob.OneDay {

    Logger log = LoggerFactory.getLogger(getClass());

    static boolean print = false;

    static boolean attack = false;

    static String html = null;

    static Set<String> uris = null;

    static Set<String> ips = new HashSet<>();

    @Autowired
    WallSite wallSite;

    public boolean print() {
        print = !print;
        return print;
    }

    public boolean attack() {
        attack = !attack;
        return attack;
    }

    public boolean isPrint() {
        return print;
    }

    public boolean isAttack() {
        return attack;
    }

    public void put(String ip) {
        ips.add(ip);
    }

    public boolean shield(String uri, String ip) {
        if (null == uris)
            uris = baseMapper.gets();
        if (uris.isEmpty())
            return false;
        if (uris.contains(uri)) {
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

    public String getHtml() throws IOException {
        if (null == html || html.isEmpty()) {
            html = IOUtils.resourceToString("/templates/shield.html", Charset.defaultCharset());
        }
        return html;
    }

    @Override
    public void onOneMinute() {
        uris = null;
    }

    @Override
    public void onOneDay() {
        ips.clear();
    }
}
