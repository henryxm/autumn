package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysMenuDao extends BaseMapper<SysMenuEntity> {
	
	/**
	 * 根据父菜单，查询子菜单
	 * @param parentId 父菜单ID
	 */
	@Deprecated
	@Select("select * from sys_menu where parent_id = #{parentId} order by order_num asc")
	List<SysMenuEntity> queryListParentId(@Param("parentId") Long parentId);

	@Select("select * from sys_menu where parent_key = #{parentKey} order by order_num asc")
	List<SysMenuEntity> getByParentKey(@Param("parentKey") String parentKey);
	
	/**
	 * 获取不包含按钮的菜单列表
	 */
	@Select("select * from sys_menu where type != 2 order by order_num asc")
	List<SysMenuEntity> queryNotButtonList();

	@Select("select * from sys_menu where menu_key = #{menuKey} limit 1")
	SysMenuEntity getByMenuKey(@Param("menuKey") String menuKey);
}
