package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysDeptEntity;
import cn.org.autumn.modules.sys.entity.SysRoleEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysDeptDao extends BaseMapper<SysDeptEntity> {

    /**
     * 查询子部门ID列表
     *
     * @param parentId 上级部门ID
     */
    @Select("select dept_id from sys_dept where parent_id = #{value} and del_flag = 0")
    List<Long> queryDetpIdList(@Param("value") Long parentId);

    @Select("select * from sys_dept where dept_Key = #{deptKey}")
    SysDeptEntity getByDeptKey(@Param("deptKey") String deptKey);
}
