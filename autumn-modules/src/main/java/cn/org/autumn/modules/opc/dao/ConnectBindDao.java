package cn.org.autumn.modules.opc.dao;

import cn.org.autumn.modules.opc.dao.sql.OpcDaoSql;
import cn.org.autumn.modules.opc.entity.ConnectBindEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ConnectBindDao extends BaseMapper<ConnectBindEntity> {

    @SelectProvider(type = OpcDaoSql.class, method = "connectBindByConnectAppAndUser")
    ConnectBindEntity getByConnectAppAndUser(@Param("connectApp") String connectApp, @Param("user") String user);

    @SelectProvider(type = OpcDaoSql.class, method = "connectBindByConnectAppAndOpenId")
    ConnectBindEntity getByConnectAppAndOpenId(@Param("connectApp") String connectApp, @Param("openId") String openId);

    @SelectProvider(type = OpcDaoSql.class, method = "connectBindByConnectAppAndUnionId")
    ConnectBindEntity getByConnectAppAndUnionId(@Param("connectApp") String connectApp, @Param("unionId") String unionId);
}
