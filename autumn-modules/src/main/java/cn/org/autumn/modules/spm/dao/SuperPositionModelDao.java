package cn.org.autumn.modules.spm.dao;

import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
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

    @Select("select * from spm_super_position_model where resource_id = #{resourceId} limit 1")
    SuperPositionModelEntity getByResourceId(@Param("resourceId") String resourceId);

    @Select("select * from spm_super_position_model where url_key = #{urlKey} limit 1")
    SuperPositionModelEntity getByUrlKey(@Param("urlKey") String urlKey);
}
