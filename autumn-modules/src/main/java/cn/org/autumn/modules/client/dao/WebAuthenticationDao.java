package cn.org.autumn.modules.client.dao;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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
    Integer hasClientId(@Param("clientId") String clientId);
}
