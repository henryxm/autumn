package cn.org.autumn.modules.opl.dao;

import cn.org.autumn.modules.opl.dao.sql.OplDaoSql;
import cn.org.autumn.modules.opl.entity.OpenTokenEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface OpenTokenDao extends BaseMapper<OpenTokenEntity> {

    @SelectProvider(type = OplDaoSql.class, method = "openTokenByAccessToken")
    OpenTokenEntity getByAccessToken(@Param("accessToken") String accessToken);

    @SelectProvider(type = OplDaoSql.class, method = "openTokenByRefreshToken")
    OpenTokenEntity getByRefreshToken(@Param("refreshToken") String refreshToken);

    @SelectProvider(type = OplDaoSql.class, method = "openTokenByAuthCode")
    OpenTokenEntity getByAuthCode(@Param("authCode") String authCode);
}
