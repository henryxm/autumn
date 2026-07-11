package cn.org.autumn.modules.qrc.dao;

import cn.org.autumn.modules.qrc.dao.sql.ClientGrantDaoSql;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ClientGrantDao extends BaseMapper<ClientGrantEntity> {

    @SelectProvider(type = ClientGrantDaoSql.class, method = "getByUuid")
    ClientGrantEntity getByUuid(@Param("uuid") String uuid);

    @SelectProvider(type = ClientGrantDaoSql.class, method = "getByClientId")
    ClientGrantEntity getByClientId(@Param("clientId") String clientId);
}
