package cn.org.autumn.modules.client.dao;

import cn.org.autumn.modules.client.dao.sql.WebOauthCombineDaoSql;
import cn.org.autumn.modules.client.entity.WebOauthCombineEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
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

    @SelectProvider(type = WebOauthCombineDaoSql.class, method = "getByClientId")
    WebOauthCombineEntity getByClientId(@Param("clientId") String clientId);

    @SelectProvider(type = WebOauthCombineDaoSql.class, method = "count")
    int count(@Param("clientId") String clientId);
}
