package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.dao.sql.WallCounterSql;
import cn.org.autumn.modules.wall.dao.sql.WallDaoSql;
import cn.org.autumn.modules.wall.entity.IpAdminWhiteEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Mapper
@Repository
public interface IpAdminWhiteDao extends BaseMapper<IpAdminWhiteEntity> {

    @SelectProvider(type = WallDaoSql.class, method = "ipAdminWhiteGetByIp")
    IpAdminWhiteEntity getByIp(@Param("ip") String ip);

    @SelectProvider(type = WallDaoSql.class, method = "ipAdminWhiteHasIp")
    int hasIp(@Param("ip") String ip);

    @SelectProvider(type = WallCounterSql.class, method = "ipAdminWhiteBump")
    Integer count(@Param("ip") String ip, @Param("userAgent") String userAgent, @Param("count") Integer count);

    @UpdateProvider(type = WallDaoSql.class, method = "ipAdminWhiteRefreshToday")
    Integer refresh();

    @SelectProvider(type = WallDaoSql.class, method = "ipAdminWhiteGetIps")
    Set<String> getIps(@Param("forbidden") Integer forbidden);

    @SelectProvider(type = WallDaoSql.class, method = "ipAdminWhiteActiveCount")
    int activeCount();
}
