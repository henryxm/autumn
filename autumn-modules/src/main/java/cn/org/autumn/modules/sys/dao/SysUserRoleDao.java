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

import cn.org.autumn.modules.sys.entity.SysUserRoleEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysUserRoleDao extends BaseMapper<SysUserRoleEntity> {

    /**
     * 根据用户ID，获取角色ID列表
     */
    @Select("select role_id from sys_user_role where user_id = #{value}")
    List<Long> queryRoleIdList(@Param("value") Long userId);

    @Select("select role_key from sys_user_role where user_uuid = #{userUuid}")
    List<String> getRoleKeys(@Param("userUuid") String userUuid);

    @Select("select count(*) from sys_user_role where user_uuid = #{userUuid} and role_key = #{roleKey}")
    Integer hasUserRole(@Param("userUuid") String userUuid, @Param("roleKey") String roleKey);

    /**
     * 根据角色ID数组，批量删除
     */
    @Delete("DELETE FROM sys_user_role WHERE role_id IN (#{roleIds})")
    @Lang(SelectInLangDriver.class)
    int deleteBatch(@Param("roleIds") Long[] roleIds);

    @Delete("DELETE FROM sys_user_role WHERE role_key IN (#{roleKeys})")
    int deleteByRoleKeys(@Param("roleIds") String[] roleKeys);
}
