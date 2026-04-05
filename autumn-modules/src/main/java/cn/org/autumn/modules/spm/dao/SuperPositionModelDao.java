package cn.org.autumn.modules.spm.dao;

import cn.org.autumn.modules.spm.dao.sql.SuperPositionModelDaoSql;
import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
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

    @SelectProvider(type = SuperPositionModelDaoSql.class, method = "getByResourceId")
    SuperPositionModelEntity getByResourceId(@Param("resourceId") String resourceId);

    @SelectProvider(type = SuperPositionModelDaoSql.class, method = "getByUrlKey")
    SuperPositionModelEntity getByUrlKey(@Param("urlKey") String urlKey);
}
