package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysRoleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SysRoleDao extends BaseMapper<SysRoleEntity> {

    @Select("select * from sys_role where role_key = #{roleKey}")
    SysRoleEntity getByRoleKey(@Param("roleKey") String roleKey);
}
