package cn.org.autumn.modules.spm.dao;

import cn.org.autumn.modules.spm.entity.VisitLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 访问统计
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface VisitLogDao extends BaseMapper<VisitLogEntity> {
	
}
