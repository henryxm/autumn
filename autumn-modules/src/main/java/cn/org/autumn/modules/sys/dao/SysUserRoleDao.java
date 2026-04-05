package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysUserRoleDaoSql;
import cn.org.autumn.modules.sys.entity.SysUserRoleEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysUserRoleDao extends BaseMapper<SysUserRoleEntity> {

    @SelectProvider(type = SysUserRoleDaoSql.class, method = "getRoleKeys")
    List<String> getRoleKeys(@Param("userUuid") String userUuid);

    @SelectProvider(type = SysUserRoleDaoSql.class, method = "getByUsername")
    List<SysUserRoleEntity> getByUsername(@Param("username") String username);

    @SelectProvider(type = SysUserRoleDaoSql.class, method = "hasUserRole")
    Integer hasUserRole(@Param("userUuid") String userUuid, @Param("roleKey") String roleKey);

    @DeleteProvider(type = SysUserRoleDaoSql.class, method = "deleteByRoleKeys")
    @Lang(SelectInLangDriver.class)
    int deleteByRoleKeys(@Param("roleKeys") String[] roleKeys);
}
