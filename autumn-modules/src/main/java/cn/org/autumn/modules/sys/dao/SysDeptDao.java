package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysDeptEntity;
import cn.org.autumn.modules.sys.entity.SysRoleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysDeptDao extends BaseMapper<SysDeptEntity> {

    @Select("select dept_key from sys_dept where parent_key = #{value} and del_flag = 0")
    List<String> getByParentKey(@Param("value") String parentKey);

    @Select("select * from sys_dept where dept_Key = #{deptKey}")
    SysDeptEntity getByDeptKey(@Param("deptKey") String deptKey);
}
