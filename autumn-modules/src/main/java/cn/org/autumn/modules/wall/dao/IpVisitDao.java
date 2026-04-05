package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.IpVisitEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import cn.org.autumn.modules.wall.dao.sql.WallCounterSql;
import cn.org.autumn.modules.wall.dao.sql.WallDaoSql;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

/**
 * IP访问表
 *
 * @author User
 * @email henryxm@163.com
 * @date 2021-11
 */
@Mapper
@Repository
public interface IpVisitDao extends BaseMapper<IpVisitEntity> {

    @SelectProvider(type = WallDaoSql.class, method = "ipVisitGetByIp")
    IpVisitEntity getByIp(@Param("ip") String ip);

    @SelectProvider(type = WallDaoSql.class, method = "ipVisitHasIp")
    Integer hasIp(@Param("ip") String ip);

    @SelectProvider(type = WallCounterSql.class, method = "ipVisitBump")
    Integer count(@Param("ip") String ip, @Param("userAgent") String userAgent, @Param("host") String host, @Param("uri") String uri, @Param("refer") String refer, @Param("count") Integer count);

    @UpdateProvider(type = WallDaoSql.class, method = "ipVisitRefreshToday")
    Integer refresh();
}
