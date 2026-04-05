package cn.org.autumn.modules.client.dao;

import cn.org.autumn.config.ClientType;
import cn.org.autumn.modules.client.dao.sql.WebAuthenticationDaoSql;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

/**
 * 网站客户端
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface WebAuthenticationDao extends BaseMapper<WebAuthenticationEntity> {

    @SelectProvider(type = WebAuthenticationDaoSql.class, method = "getByClientId")
    WebAuthenticationEntity getByClientId(@Param("clientId") String clientId);

    @SelectProvider(type = WebAuthenticationDaoSql.class, method = "count")
    int count(@Param("clientId") String clientId);

    @SelectProvider(type = WebAuthenticationDaoSql.class, method = "countClientType")
    int countClientType(@Param("clientType") ClientType clientType);

    @SelectProvider(type = WebAuthenticationDaoSql.class, method = "getByUuid")
    WebAuthenticationEntity getByUuid(@Param("uuid") String uuid);

    @SelectProvider(type = WebAuthenticationDaoSql.class, method = "hasClientId")
    Integer hasClientId(@Param("clientId") String clientId);

    @UpdateProvider(type = WebAuthenticationDaoSql.class, method = "updateClientType")
    void updateClientType(@Param("clientId") String clientId, @Param("clientType") ClientType clientType);
}
