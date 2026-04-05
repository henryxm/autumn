package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.HostEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import cn.org.autumn.modules.wall.dao.sql.WallCounterSql;
import cn.org.autumn.modules.wall.dao.sql.WallDaoSql;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 主机统计
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface HostDao extends BaseMapper<HostEntity> {

    @SelectProvider(type = WallDaoSql.class, method = "hostGetByHost")
    HostEntity getByHost(@Param("host") String host);

    @SelectProvider(type = WallDaoSql.class, method = "hostHasHost")
    Integer hasHost(@Param("host") String host);

    @SelectProvider(type = WallCounterSql.class, method = "hostBump")
    Integer count(@Param("host") String host, @Param("count") Integer count);

    @UpdateProvider(type = WallDaoSql.class, method = "hostRefreshToday")
    Integer refresh();

    @SelectProvider(type = WallDaoSql.class, method = "hostGetHosts")
    List<String> getHosts(@Param("forbidden") Integer forbidden);
}
