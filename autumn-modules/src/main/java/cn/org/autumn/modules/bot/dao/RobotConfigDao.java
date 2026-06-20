package cn.org.autumn.modules.bot.dao;

import cn.org.autumn.modules.bot.dao.sql.RobotConfigDaoSql;
import cn.org.autumn.modules.bot.entity.RobotConfigEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface RobotConfigDao extends BaseMapper<RobotConfigEntity> {

    @SelectProvider(type = RobotConfigDaoSql.class, method = "getUser")
    RobotConfigEntity getUser(@Param("user") String user);
}
