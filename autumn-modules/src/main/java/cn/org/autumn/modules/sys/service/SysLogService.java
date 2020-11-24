/**
 * Copyright 2018 Autumn.org.cn http://www.autumn.org.cn
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.org.autumn.modules.sys.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.table.TableInit;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.sys.dao.SysLogDao;
import cn.org.autumn.modules.sys.entity.SysLogEntity;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;


@Service
public class SysLogService extends ServiceImpl<SysLogDao, SysLogEntity> implements LoopJob.Job {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TableInit tableInit;

    @PostConstruct
    public void init() {
        LoopJob.onOneWeek(this);
        if (!tableInit.init)
            return;

    }

    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");

        Page<SysLogEntity> page = this.selectPage(
                new Query<SysLogEntity>(params).getPage(),
                new EntityWrapper<SysLogEntity>().like(StringUtils.isNotBlank(key), "username", key)
        );

        return new PageUtils(page);
    }

    @Override
    public void runJob() {
        baseMapper.clear();
    }

    public String changeLevel(String rootLevel, String singleLevel, String singlePath) {
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            logger.debug("set log rootLevel:{},singleLevel:{},singlePath:{}", rootLevel, singleLevel,
                    singlePath);
            if (!StringUtils.isEmpty(rootLevel)) {
                // 设置全局日志级别
                ch.qos.logback.classic.Logger logger = loggerContext.getLogger("root");
                logger.setLevel(Level.toLevel(rootLevel));
            }
            if (!StringUtils.isEmpty(singleLevel)) {
                // 设置某个类日志级别-可以实现定向日志级别调整
                ch.qos.logback.classic.Logger vLogger = loggerContext.getLogger(singlePath);
                if (vLogger != null) {
                    vLogger.setLevel(Level.toLevel(singleLevel));
                }
            }
        } catch (Exception e) {

        }
        return "success";
    }
}
