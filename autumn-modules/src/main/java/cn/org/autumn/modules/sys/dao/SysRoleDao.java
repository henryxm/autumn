package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysRoleDaoSql;
import cn.org.autumn.modules.sys.entity.SysRoleEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SysRoleDao extends BaseMapper<SysRoleEntity> {

    @SelectProvider(type = SysRoleDaoSql.class, method = "getByRoleKey")
    SysRoleEntity getByRoleKey(@Param("roleKey") String roleKey);
}
