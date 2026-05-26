package cn.org.autumn.modules.bot.dao;

import cn.org.autumn.modules.bot.dao.sql.RobotHookDaoSql;
import cn.org.autumn.modules.bot.entity.RobotHookEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface RobotHookDao extends BaseMapper<RobotHookEntity> {

    @SelectProvider(type = RobotHookDaoSql.class, method = "getByUuid")
    RobotHookEntity getByUuid(@Param("uuid") String uuid);

    @SelectProvider(type = RobotHookDaoSql.class, method = "listByRobot")
    List<RobotHookEntity> listByRobot(@Param("robot") String robot);

    @SelectProvider(type = RobotHookDaoSql.class, method = "listActiveByRobot")
    List<RobotHookEntity> listActiveByRobot(@Param("robot") String robot);

    @SelectProvider(type = RobotHookDaoSql.class, method = "countByRobot")
    int countByRobot(@Param("robot") String robot);
}
