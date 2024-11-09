package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    @Select("select * from wall_ip_black where ip = #{ip} limit 1")
    IpBlackEntity getByIp(@Param("ip") String ip);

    @Select("select count(*) from wall_ip_black where ip = #{ip} limit 1")
    Integer hasIp(@Param("ip") String ip);

    @Select("update wall_ip_black set `count` = ifnull(`count`,0) + #{count}, user_agent = #{userAgent}, today = ifnull(today,0) + #{count}, update_time = now() where ip = #{ip}")
    Integer count(@Param("ip") String ip, @Param("userAgent") String userAgent, @Param("count") Integer count);

    @Select("update wall_ip_black set today = 0")
    Integer clear();

    @Select("select wi.ip from wall_ip_black wi where wi.available = #{available}")
    Set<String> getIps(@Param("available") Integer available);
}
