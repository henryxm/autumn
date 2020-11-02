package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.UrlBlackEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 链接黑名单
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface UrlBlackDao extends BaseMapper<UrlBlackEntity> {
	
}
