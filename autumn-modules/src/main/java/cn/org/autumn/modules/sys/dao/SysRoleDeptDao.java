/**
 * Copyright 2018 Autumn.org.cn http://www.autumn.org.cn
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysRoleDeptEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysRoleDeptDao extends BaseMapper<SysRoleDeptEntity> {

    /**
     * 根据角色ID，获取部门ID列表
     */
    @Select("SELECT dept_id FROM sys_role_dept WHERE role_id IN (#{roleIds})")
    @Lang(SelectInLangDriver.class)
    List<Long> queryDeptIdList(@Param("roleIds") Long[] roleIds);

    /**
     * 根据角色ID数组，批量删除
     */
    @Delete("DELETE FROM sys_role_dept WHERE role_id IN (#{roleIds})")
    @Lang(SelectInLangDriver.class)
    int deleteBatch(@Param("roleIds") Long[] roleIds);

    /**
     * 根据角色key，获取部门Key列表
     */
    @Select("SELECT dept_key FROM sys_role_dept WHERE role_key IN (#{roleKeys})")
    List<String> getDeptKeys(@Param("roleKeys") String[] roleKeys);

    /**
     * 根据角色Key数组，批量删除
     */
    @Delete("DELETE FROM sys_role_dept WHERE role_key IN (#{roleKeys})")
    int deleteByRoleKey(@Param("roleKeys") String[] roleKeys);
}
