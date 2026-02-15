package cn.org.autumn.modules.db.dao;

import cn.org.autumn.modules.db.entity.DatabaseBackupStrategyEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 备份策略
 * 
 * @author User
 * @email henryxm@163.com
 * @date 2026-02
 */
@Mapper
@Repository
public interface DatabaseBackupStrategyDao extends BaseMapper<DatabaseBackupStrategyEntity> {
	
}
