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

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysUserDao extends BaseMapper<SysUserEntity> {

    /**
     * 查询用户的所有权限
     *
     * @param userId 用户ID
     */
    @Select("select m.perms from sys_user_role ur " +
            "LEFT JOIN sys_role_menu rm on ur.role_id = rm.role_id " +
            "LEFT JOIN sys_menu m on rm.menu_id = m.menu_id " +
            "where ur.user_id = #{userId}")
    List<String> queryAllPerms(@Param("userId") Long userId);

    /**
     * 查询用户的所有菜单ID
     */
    @Select("select distinct rm.menu_id from sys_user_role ur " +
            "LEFT JOIN sys_role_menu rm on ur.role_id = rm.role_id " +
            "where ur.user_id = #{userId}")
    List<Long> queryAllMenuId(@Param("userId") Long userId);

    @Select("select * from sys_user u where u.username = #{username}")
    SysUserEntity getByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE email=#{email}")
    SysUserEntity getByEmail(@Param("email")String email);

    @Select("SELECT * FROM sys_user WHERE mobile=#{mobile}")
    SysUserEntity getByPhone(@Param("mobile")String mobile);

    @Select("SELECT * FROM sys_user WHERE uuid=#{uuid}")
    SysUserEntity getByUuid(@Param("uuid")String uuid);

}
