package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysMenuDaoSql;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.mybatis.SelectInLangDriver;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysMenuDao extends BaseMapper<SysMenuEntity> {

    @SelectProvider(type = SysMenuDaoSql.class, method = "getByParentKey")
    List<SysMenuEntity> getByParentKey(@Param("parentKey") String parentKey);

    /**
     * 获取不包含按钮的菜单列表
     */
    @SelectProvider(type = SysMenuDaoSql.class, method = "queryNotButtonList")
    List<SysMenuEntity> queryNotButtonList();

    @SelectProvider(type = SysMenuDaoSql.class, method = "getByMenuKey")
    SysMenuEntity getByMenuKey(@Param("menuKey") String menuKey);

    @DeleteProvider(type = SysMenuDaoSql.class, method = "deleteByMenuKeys")
    @Lang(SelectInLangDriver.class)
    int deleteByMenuKeys(@Param("menuKeys") String[] menuKeys);
}
