package cn.org.autumn.modules.opl.dao;

import cn.org.autumn.modules.opl.dao.sql.OplDaoSql;
import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface OpenAppDao extends BaseMapper<OpenAppEntity> {

    @SelectProvider(type = OplDaoSql.class, method = "openAppByAppId")
    OpenAppEntity getByAppId(@Param("appId") String appId);

    @SelectProvider(type = OplDaoSql.class, method = "openAppByAccount")
    List<OpenAppEntity> listByAccount(@Param("account") String account);
}
