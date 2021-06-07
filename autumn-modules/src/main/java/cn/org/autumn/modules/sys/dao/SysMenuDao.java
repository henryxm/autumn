package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysMenuDao extends BaseMapper<SysMenuEntity> {

    @Select("select * from sys_menu where parent_key = #{parentKey} order by order_num asc")
    List<SysMenuEntity> getByParentKey(@Param("parentKey") String parentKey);

    /**
     * 获取不包含按钮的菜单列表
     */
    @Select("select * from sys_menu where type != 2 order by order_num asc")
    List<SysMenuEntity> queryNotButtonList();

    @Select("select * from sys_menu where menu_key = #{menuKey} limit 1")
    SysMenuEntity getByMenuKey(@Param("menuKey") String menuKey);

    @Delete("DELETE FROM sys_menu WHERE menu_key IN (#{menuKeys})")
    @Lang(SelectInLangDriver.class)
    int deleteByMenuKeys(@Param("menuKeys") String[] menuKeys);
}
