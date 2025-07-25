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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ShieldService extends ModuleService<ShieldDao, ShieldEntity> implements LoopJob.FiveSecond, LoopJob.OneMinute, LoopJob.OneDay {

    Logger log = LoggerFactory.getLogger(getClass());

    static boolean print = false;

    static boolean attack = false;

    static String html = null;

    static Set<String> uris = null;

    static Set<String> ips = new HashSet<>();

    static List<String> visit = new ArrayList<>();

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
        try {
            if (null == ip) {
                log.error("空IP地址");
                return false;
            }
            if (null == uris)
                uris = baseMapper.gets();
            visit.add(ip);
            if (visit.size() > 10000)
                enable();
            if (uris.isEmpty()) {
                return false;
            }
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
        } catch (Exception e) {
            log.error("防御护盾:{}", e.getMessage());
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
    public void onFiveSecond() {
        visit.clear();
    }

    @Override
    public void onOneMinute() {
        uris = null;
    }

    @Override
    public void onOneDay() {
        ips.clear();
    }

    public void enable() {
        ShieldEntity shield = baseMapper.get();
        if (null != shield) {
            if (shield.getAuto() < 10000)
                shield.setAuto(10000);
            if (visit.size() < shield.getAuto())
                return;
            shield.setEnable(true);
            updateById(shield);
        } else {
            create();
        }
        attack = true;
        uris = baseMapper.gets();
    }

    public void disable() {
        ShieldEntity shield = baseMapper.get();
        if (null != shield) {
            shield.setEnable(false);
            if (shield.getAuto() < 10000)
                shield.setAuto(10000);
            updateById(shield);
        } else {
            create();
        }
        uris = baseMapper.gets();
    }

    public void create() {
        ShieldEntity shield = new ShieldEntity();
        shield.setEnable(false);
        shield.setUri("/");
        shield.setAuto(10000);
        insert(shield);
    }

    public boolean toggleEnable() {
        ShieldEntity shield = baseMapper.get();
        if (shield == null) {
            create();
            shield = baseMapper.get();
        }
        boolean newEnable = !shield.isEnable();
        shield.setEnable(newEnable);
        updateById(shield);
        uris = baseMapper.gets();
        return newEnable;
    }

    public boolean getEnable() {
        ShieldEntity shield = baseMapper.get();
        return shield != null && shield.isEnable();
    }

    @Override
    public void init() {
        if (baseMapper.has() > 0)
            return;
        create();
    }

    public static synchronized boolean getPrint() {
        return print;
    }

    public static synchronized void setPrint(boolean value) {
        print = value;
    }

    public static synchronized boolean getAttack() {
        return attack;
    }

    public static synchronized void setAttack(boolean value) {
        attack = value;
    }
}
