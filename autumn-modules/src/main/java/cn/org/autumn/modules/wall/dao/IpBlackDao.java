package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import cn.org.autumn.modules.wall.dao.sql.WallCounterSql;
import cn.org.autumn.modules.wall.dao.sql.WallDaoSql;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * IP黑名单
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface IpBlackDao extends BaseMapper<IpBlackEntity> {

    @SelectProvider(type = WallDaoSql.class, method = "ipBlackGetByIp")
    IpBlackEntity getByIp(@Param("ip") String ip);

    @SelectProvider(type = WallDaoSql.class, method = "ipBlackHasIp")
    Integer hasIp(@Param("ip") String ip);

    @SelectProvider(type = WallCounterSql.class, method = "ipBlackBump")
    Integer count(@Param("ip") String ip, @Param("userAgent") String userAgent, @Param("count") Integer count);

    @UpdateProvider(type = WallDaoSql.class, method = "ipBlackRefreshToday")
    Integer refresh();

    @SelectProvider(type = WallDaoSql.class, method = "ipBlackGetIps")
    Set<String> getIps(@Param("available") Integer available);
}
