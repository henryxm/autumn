package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.IpWhiteEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

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

    @Select("select * from wall_ip_white where ip = #{ip} limit 1")
    IpWhiteEntity getByIp(@Param("ip") String ip);

    @Select("select count(*) from wall_ip_white where ip = #{ip} limit 1")
    Integer hasIp(@Param("ip") String ip);

    @Select("update wall_ip_white set `count` = ifnull(`count`,0) + #{count}, today = ifnull(today,0) + #{count}, update_time = now() where ip = #{ip}")
    Integer count(@Param("ip") String ip, @Param("count") Integer count);

    @Select("update wall_ip_white set today = 0")
    Integer clear();
}
