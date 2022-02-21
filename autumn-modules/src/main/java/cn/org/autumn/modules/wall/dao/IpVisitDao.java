package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import cn.org.autumn.modules.wall.entity.IpVisitEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    @Select("select * from wall_ip_visit where ip = #{ip} limit 1")
    IpVisitEntity getByIp(@Param("ip") String ip);

    @Select("select count(*) from wall_ip_visit where ip = #{ip}")
    Integer hasIp(@Param("ip") String ip);

    @Select("update wall_ip_visit set `count` = ifnull(`count`,0) + #{count}, `user_agent` = #{userAgent}, `host` = #{host}, `uri` = #{uri}, `refer` = #{refer}, today = ifnull(today,0) + #{count}, update_time = now() where ip = #{ip}")
    Integer count(@Param("ip") String ip, @Param("userAgent") String userAgent, @Param("host") String host, @Param("uri") String uri, @Param("refer") String refer, @Param("count") Integer count);

    @Select("update wall_ip_visit set today = 0")
    Integer clear();
}
