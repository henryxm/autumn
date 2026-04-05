package cn.org.autumn.modules.oauth.dao;

import cn.org.autumn.config.ClientType;
import cn.org.autumn.modules.oauth.dao.sql.ClientDetailsDaoSql;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

/**
 * 客户端详情
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface ClientDetailsDao extends BaseMapper<ClientDetailsEntity> {

    @SelectProvider(type = ClientDetailsDaoSql.class, method = "findByClientId")
    ClientDetailsEntity findByClientId(@Param("clientId") String clientId);

    @SelectProvider(type = ClientDetailsDaoSql.class, method = "count")
    int count(@Param("clientId") String clientId);

    @SelectProvider(type = ClientDetailsDaoSql.class, method = "countClientType")
    int countClientType(@Param("clientType") ClientType clientType);

    @SelectProvider(type = ClientDetailsDaoSql.class, method = "getByUuid")
    ClientDetailsEntity getByUuid(@Param("uuid") String uuid);

    @SelectProvider(type = ClientDetailsDaoSql.class, method = "findByClientSecret")
    ClientDetailsEntity findByClientSecret(@Param("clientSecret") String clientSecret);

    @UpdateProvider(type = ClientDetailsDaoSql.class, method = "updateClientType")
    void updateClientType(@Param("clientId") String clientId, @Param("clientType") ClientType clientType);
}
