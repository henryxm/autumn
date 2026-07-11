package cn.org.autumn.modules.auth.dao;

import cn.org.autumn.modules.auth.dao.sql.ScopeDefinitionDaoSql;
import cn.org.autumn.modules.auth.entity.ScopeDefinitionEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ScopeDefinitionDao extends BaseMapper<ScopeDefinitionEntity> {

    @SelectProvider(type = ScopeDefinitionDaoSql.class, method = "getByCode")
    ScopeDefinitionEntity getByCode(@Param("code") String code);

    @SelectProvider(type = ScopeDefinitionDaoSql.class, method = "getByUuid")
    ScopeDefinitionEntity getByUuid(@Param("uuid") String uuid);
}
