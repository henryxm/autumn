package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysCategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 系统配置类型表
 *
 * @author User
 * @email henryxm@163.com
 * @date 2022-12
 */
@Mapper
@Repository
public interface SysCategoryDao extends BaseMapper<SysCategoryEntity> {

    @Select("select count(*) from sys_category where category = #{category}")
    int has(@Param("category") String category);

    @Select("select * from sys_category where category = #{category}")
    SysCategoryEntity getByCategory(@Param("category") String category);
}