package cn.org.autumn.modules.client.dao;

import cn.org.autumn.modules.client.entity.WebOauthCombineEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 授权登录合并
 *
 * @author User
 * @email henryxm@163.com
 * @date 2023-04
 */
@Mapper
@Repository
public interface WebOauthCombineDao extends BaseMapper<WebOauthCombineEntity> {

    @Select("select * from client_web_oauth_combine where client_id = #{clientId} limit 1")
    WebOauthCombineEntity getByClientId(@Param("clientId") String clientId);
}
