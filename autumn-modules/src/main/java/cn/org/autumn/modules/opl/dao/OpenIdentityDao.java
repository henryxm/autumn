package cn.org.autumn.modules.opl.dao;

import cn.org.autumn.modules.opl.dao.sql.OplDaoSql;
import cn.org.autumn.modules.opl.entity.OpenIdentityEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface OpenIdentityDao extends BaseMapper<OpenIdentityEntity> {

    @SelectProvider(type = OplDaoSql.class, method = "openIdentityByAppIdAndUser")
    OpenIdentityEntity getByAppIdAndUser(@Param("appId") String appId, @Param("user") String user);

    @SelectProvider(type = OplDaoSql.class, method = "openIdentityByOpenId")
    OpenIdentityEntity getByOpenId(@Param("openId") String openId);

}
