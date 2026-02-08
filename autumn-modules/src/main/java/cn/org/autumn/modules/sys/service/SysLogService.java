package cn.org.autumn.modules.sys.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import cn.org.autumn.modules.job.task.LoopJob;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.sys.dao.SysLogDao;
import cn.org.autumn.modules.sys.entity.SysLogEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Null;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SysLogService extends ServiceImpl<SysLogDao, SysLogEntity> implements LoopJob.OneWeek {

    private static final Map<String, String> recent = new ConcurrentHashMap<>();

    public Map<String, String> getRecent() {
        return recent;
    }

    public String recent() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> kv : recent.entrySet()) {
            builder.append(kv.getKey()).append(":").append(kv.getValue());
            builder.append("<br/>");
        }
        return builder.toString();
    }

    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        EntityWrapper<SysLogEntity> entityEntityWrapper = new EntityWrapper<>();
        Page<SysLogEntity> page = this.selectPage(
                new Query<SysLogEntity>(params).getPage(),
                new EntityWrapper<SysLogEntity>().like(StringUtils.isNotBlank(key), "username", key)
        );
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
        return new PageUtils(page);
    }

    @Override
    public void onOneWeek() {
        baseMapper.clear();
    }

    @Deprecated()
    public String changeLevel(@Null String deprecated, String level, String name) {
        return changeLevel(level, name);
    }

    public String changeLevel(String level, String name) {
        try {
            if (StringUtils.isNotBlank(level)) {
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                log.debug("Set log singleLevel:{},singlePath:{}", level, name);
                if (StringUtils.isEmpty(name)) {
                    // 设置全局日志级别
                    ch.qos.logback.classic.Logger logger = loggerContext.getLogger("root");
                    logger.setLevel(Level.toLevel(level));
                } else {
                    // 设置某个类日志级别-可以实现定向日志级别调整
                    ch.qos.logback.classic.Logger vLogger = loggerContext.getLogger(name);
                    if (vLogger != null) {
                        vLogger.setLevel(Level.toLevel(level));
                        recent.put(name, level);
                    }
                }
            }
        } catch (Exception e) {
            log.error("修改级别:{}", e.getMessage());
        }
        return "success";
    }

    public List<Map<String, String>> listLoggers() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<ch.qos.logback.classic.Logger> loggers = loggerContext.getLoggerList();
        List<Map<String, String>> loggerInfoList = new ArrayList<>();
        for (ch.qos.logback.classic.Logger logger : loggers) {
            Map<String, String> loggerInfo = new HashMap<>();
            loggerInfo.put("name", logger.getName());
            loggerInfo.put("level", logger.getLevel() != null ? logger.getLevel().levelStr : "INHERITED");
            loggerInfoList.add(loggerInfo);
        }
        return loggerInfoList;
    }
}
