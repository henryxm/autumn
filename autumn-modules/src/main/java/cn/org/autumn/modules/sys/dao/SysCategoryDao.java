package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysCategoryDaoSql;
import cn.org.autumn.modules.sys.entity.SysCategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

/**
 * 系统配置类型表
 *
 * @author User
 * @email henryxm@163.com
 * @date 2022-12
 */
@Mapper
@Repository
public interface SysCategoryDao extends BaseMapper<SysCategoryEntity> {

    @SelectProvider(type = SysCategoryDaoSql.class, method = "has")
    int has(@Param("category") String category);

    @SelectProvider(type = SysCategoryDaoSql.class, method = "getByCategory")
    SysCategoryEntity getByCategory(@Param("category") String category);
}
