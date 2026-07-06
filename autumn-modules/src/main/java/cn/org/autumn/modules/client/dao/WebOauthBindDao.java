package cn.org.autumn.modules.client.dao;

import cn.org.autumn.modules.client.dao.sql.WebOauthBindDaoSql;
import cn.org.autumn.modules.client.entity.WebOauthBindEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface WebOauthBindDao extends BaseMapper<WebOauthBindEntity> {

    @SelectProvider(type = WebOauthBindDaoSql.class, method = "getByAuthenticationAndUpper")
    WebOauthBindEntity getByAuthenticationAndUpper(@Param("authentication") String authentication, @Param("upper") String upper);

    @SelectProvider(type = WebOauthBindDaoSql.class, method = "getByAuthenticationAndUser")
    WebOauthBindEntity getByAuthenticationAndUser(@Param("authentication") String authentication, @Param("user") String user);
}
