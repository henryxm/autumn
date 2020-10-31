package cn.org.autumn.modules.spm.dao;

import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 超级位置模型
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
@Mapper
@Repository
public interface SuperPositionModelDao extends BaseMapper<SuperPositionModelEntity> {
	
}
