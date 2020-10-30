package cn.org.autumn.modules.job.service;

import cn.org.autumn.modules.job.entity.ScheduleJobLogEntity;
import cn.org.autumn.modules.job.service.gen.ScheduleJobLogServiceGen;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

@Service
public class ScheduleJobLogService extends ScheduleJobLogServiceGen {

    @Override
    public int menuOrder(){
        return super.menuOrder();
    }

    @Override
    public String ico(){
        return super.ico();
    }

    private int wEveryCount = 0;

    public void clear() {
        baseMapper.clear();
    }

    public void clear(int i) {
        if (wEveryCount < i) {
            wEveryCount++;
            return;
        }
        wEveryCount = 0;
        baseMapper.clear();
    }
}
