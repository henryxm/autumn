package cn.org.autumn.modules.opl.dao;

import cn.org.autumn.modules.opl.dao.sql.OplDaoSql;
import cn.org.autumn.modules.opl.entity.OpenCodeEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface OpenCodeDao extends BaseMapper<OpenCodeEntity> {

    @SelectProvider(type = OplDaoSql.class, method = "openCodeByCode")
    OpenCodeEntity getByCode(@Param("code") String code);
}
