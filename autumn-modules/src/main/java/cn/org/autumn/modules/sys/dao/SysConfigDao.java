package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysConfigDaoSql;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SysConfigDao extends BaseMapper<SysConfigEntity> {

    /**
     * 根据key，查询value
     */
    @SelectProvider(type = SysConfigDaoSql.class, method = "queryByKey")
    SysConfigEntity queryByKey(@Param("paramKey") String paramKey);

    @SelectProvider(type = SysConfigDaoSql.class, method = "hasKey")
    Integer hasKey(@Param("paramKey") String paramKey);

    /**
     * 根据key，更新value
     */
    @UpdateProvider(type = SysConfigDaoSql.class, method = "updateValueByKey")
    int updateValueByKey(@Param("paramKey") String paramKey, @Param("paramValue") String paramValue);

}
