package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.IpBlackEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

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
}
