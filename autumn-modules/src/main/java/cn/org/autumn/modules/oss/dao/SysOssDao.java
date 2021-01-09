package cn.org.autumn.modules.oss.dao;


import cn.org.autumn.modules.oss.entity.SysOssEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 文件上传
 */
@Mapper
@Repository
public interface SysOssDao extends BaseMapper<SysOssEntity> {
	
}
