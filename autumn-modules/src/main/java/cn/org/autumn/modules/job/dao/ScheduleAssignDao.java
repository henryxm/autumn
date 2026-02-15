package cn.org.autumn.modules.job.dao;

import cn.org.autumn.modules.job.entity.ScheduleAssignEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 定时分配
 * 
 * @author User
 * @email henryxm@163.com
 * @date 2026-02
 */
@Mapper
@Repository
public interface ScheduleAssignDao extends BaseMapper<ScheduleAssignEntity> {
	
}
