package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.HostEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

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

    @Select("select * from wall_host where host = #{host} limit 1")
    HostEntity getByHost(@Param("host") String host);

    @Select("select count(*) from wall_host where host = #{host} limit 1")
    Integer hasHost(@Param("host") String host);

}
