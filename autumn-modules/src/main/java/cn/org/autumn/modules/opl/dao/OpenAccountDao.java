package cn.org.autumn.modules.opl.dao;

import cn.org.autumn.modules.opl.dao.sql.OplDaoSql;
import cn.org.autumn.modules.opl.entity.OpenAccountEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface OpenAccountDao extends BaseMapper<OpenAccountEntity> {

    @SelectProvider(type = OplDaoSql.class, method = "openAccountByUser")
    OpenAccountEntity getByUser(@Param("user") String user);

    @SelectProvider(type = OplDaoSql.class, method = "openAccountByUuid")
    OpenAccountEntity getByUuid(@Param("uuid") String uuid);
}
