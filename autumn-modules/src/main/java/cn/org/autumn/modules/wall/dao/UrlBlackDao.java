package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.UrlBlackEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    @Select("select * from wall_url_black where url = #{url} limit 1")
    UrlBlackEntity getByUrl(@Param("url") String url);

    @Select("select count(*) from wall_url_black where url = #{url} limit 1")
    Integer hasUrl(@Param("url") String url);

    @Select("update wall_url_black set `count` = ifnull(`count`,0) + #{count}, user_agent = #{userAgent}, today = ifnull(today,0) + #{count}, update_time = now() where url = #{url}")
    Integer count(@Param("url") String url, @Param("userAgent") String userAgent, @Param("count") Integer count);

    @Select("update wall_url_black set today = 0")
    Integer clear();

    @Select("select wh.url from wall_url_black wh where wh.forbidden = #{forbidden}")
    List<String> getUrls(@Param("forbidden") Integer forbidden);
}
