package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.entity.PasswordChangeLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface PasswordChangeLogDao extends BaseMapper<PasswordChangeLogEntity> {
}
