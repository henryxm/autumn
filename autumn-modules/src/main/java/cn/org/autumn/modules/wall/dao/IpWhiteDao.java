package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
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
 * IP白名单
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface IpWhiteDao extends BaseMapper<IpWhiteEntity> {

    @SelectProvider(type = WallDaoSql.class, method = "ipWhiteHasTag")
    int hasTag(@Param("tag") String tag);

    @SelectProvider(type = WallDaoSql.class, method = "ipWhiteGetByIp")
    IpWhiteEntity getByIp(@Param("ip") String ip);

    @SelectProvider(type = WallDaoSql.class, method = "ipWhiteHasIp")
    int hasIp(@Param("ip") String ip);

    @SelectProvider(type = WallCounterSql.class, method = "ipWhiteBump")
    Integer count(@Param("ip") String ip, @Param("userAgent") String userAgent, @Param("count") Integer count);

    @UpdateProvider(type = WallDaoSql.class, method = "ipWhiteRefreshToday")
    Integer refresh();

    @SelectProvider(type = WallDaoSql.class, method = "ipWhiteGetIps")
    Set<String> getIps(@Param("forbidden") Integer forbidden);
}
