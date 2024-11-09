package cn.org.autumn.modules.wall.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.job.task.LoopJob;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.wall.dao.JumpDao;
import cn.org.autumn.modules.wall.entity.JumpEntity;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JumpService extends ModuleService<JumpDao, JumpEntity> implements LoopJob.OneMinute {

    static Map<String, String> jumps = null;

    static String html = null;

    public Map<String, String> getJumps() {
        Map<String, String> map = new HashMap<>();
        List<JumpEntity> list = baseMapper.gets();
        for (JumpEntity jumpEntity : list) {
            map.put(jumpEntity.getHost() + jumpEntity.getUri(), jumpEntity.getUrl());
        }
        return map;
    }

    public String getJump(String host, String uri) {
        if (null == jumps)
            jumps = getJumps();
        return jumps.get(host + uri);
    }

    public String getHtml(String url) throws IOException {
        if (null == html || html.isEmpty()) {
            html = IOUtils.resourceToString("/templates/direct.html", Charset.defaultCharset());
        }
        return html.replace("${url}", url);
    }

    @Override
    public void onOneMinute() {
        jumps = null;
    }
}
