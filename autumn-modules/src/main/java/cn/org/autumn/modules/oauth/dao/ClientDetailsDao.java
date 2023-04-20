package cn.org.autumn.modules.oauth.dao;

import cn.org.autumn.config.ClientType;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
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

    @Select("select * from oauth_client_details where client_id = #{clientId} limit 1")
    ClientDetailsEntity findByClientId(@Param("clientId") String clientId);

    @Select("select count(*) from oauth_client_details where client_id = #{clientId}")
    int count(@Param("clientId") String clientId);

    @Select("select * from oauth_client_details where uuid = #{uuid} limit 1")
    ClientDetailsEntity getByUuid(@Param("uuid") String uuid);

    @Select("select * from oauth_client_details where client_secret = #{clientSecret} limit 1")
    ClientDetailsEntity findByClientSecret(@Param("clientSecret") String clientSecret);

    @Update("update oauth_client_details set client_type = #{clientType} where client_id = #{clientId}")
    void updateClientType(@Param("clientId") String clientId, @Param("clientType") ClientType clientType);
}
