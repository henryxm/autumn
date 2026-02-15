package cn.org.autumn.modules.client.dao;

import cn.org.autumn.config.ClientType;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
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

    @Select("select * from client_web_authentication where client_id = #{clientId} limit 1")
    WebAuthenticationEntity getByClientId(@Param("clientId") String clientId);

    @Select("select count(*) from client_web_authentication where client_id = #{clientId}")
    int count(@Param("clientId") String clientId);

    @Select("select count(*) from client_web_authentication where client_type = #{clientType}")
    int countClientType(@Param("clientType") ClientType clientType);

    @Select("select * from client_web_authentication where uuid = #{uuid} limit 1")
    WebAuthenticationEntity getByUuid(@Param("uuid") String uuid);

    @Select("select count(*) from client_web_authentication where client_id = #{clientId}")
    Integer hasClientId(@Param("clientId") String clientId);

    @Update("update client_web_authentication set client_type = #{clientType} where client_id = #{clientId}")
    void updateClientType(@Param("clientId") String clientId, @Param("clientType") ClientType clientType);
}
