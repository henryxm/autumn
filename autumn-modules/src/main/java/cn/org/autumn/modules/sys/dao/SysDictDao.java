package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysDictEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysDictDao extends BaseMapper<SysDictEntity> {

    @Select("select * from sys_dict where type = #{type} order by order_num asc")
    List<SysDictEntity> getByType(@Param("type") String type);
}
