package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.dao.sql.WallCounterSql;
import cn.org.autumn.modules.wall.dao.sql.WallDaoSql;
import cn.org.autumn.modules.wall.entity.UrlBlackEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

/**
 * 链接黑名单
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface UrlBlackDao extends BaseMapper<UrlBlackEntity> {

    @SelectProvider(type = WallDaoSql.class, method = "urlBlackGetByUrl")
    UrlBlackEntity getByUrl(@Param("url") String url);

    @SelectProvider(type = WallDaoSql.class, method = "urlBlackHasUrl")
    Integer hasUrl(@Param("url") String url);

    @SelectProvider(type = WallCounterSql.class, method = "urlBlackBump")
    Integer count(@Param("url") String url, @Param("userAgent") String userAgent, @Param("count") Integer count);

    @UpdateProvider(type = WallDaoSql.class, method = "urlBlackRefreshToday")
    Integer refresh();

    @SelectProvider(type = WallDaoSql.class, method = "urlBlackGetUrls")
    List<String> getUrls(@Param("forbidden") Integer forbidden);
}
