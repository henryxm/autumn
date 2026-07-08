package cn.org.autumn.modules.opc.dao;

import cn.org.autumn.modules.opc.dao.sql.OpcDaoSql;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ConnectAppDao extends BaseMapper<ConnectAppEntity> {

    @SelectProvider(type = OpcDaoSql.class, method = "connectAppByAppId")
    ConnectAppEntity getByAppId(@Param("appId") String appId);

    @SelectProvider(type = OpcDaoSql.class, method = "connectAppByUser")
    List<ConnectAppEntity> listByUser(@Param("user") String user);

    @SelectProvider(type = OpcDaoSql.class, method = "connectAppCountByHashInUse")
    int countByHashInUse(@Param("hash") String hash);

    @SelectProvider(type = OpcDaoSql.class, method = "connectAppListPageLoginActive")
    List<ConnectAppEntity> listPageLoginActive();

    @SelectProvider(type = OpcDaoSql.class, method = "connectAppCountSecretByAppId")
    int countSecretByAppId(@Param("appId") String appId);
}
