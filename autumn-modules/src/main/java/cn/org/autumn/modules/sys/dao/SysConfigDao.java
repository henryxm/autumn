package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SysConfigDao extends BaseMapper<SysConfigEntity> {

    /**
     * 根据key，查询value
     */
    @Select("select * from sys_config where param_key = #{paramKey}")
    SysConfigEntity queryByKey(@Param("paramKey") String paramKey);

    @Select("select count(*) from sys_config where param_key = #{paramKey}")
    Integer hasKey(@Param("paramKey") String paramKey);

    /**
     * 根据key，更新value
     */
    @Update("update sys_config set param_value = #{paramValue} where param_key = #{paramKey}")
    int updateValueByKey(@Param("paramKey") String paramKey, @Param("paramValue") String paramValue);

}
