package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysLogDaoSql;
import cn.org.autumn.modules.sys.entity.SysLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SysLogDao extends BaseMapper<SysLogEntity> {

    @UpdateProvider(type = SysLogDaoSql.class, method = "clear")
    void clear();
}
