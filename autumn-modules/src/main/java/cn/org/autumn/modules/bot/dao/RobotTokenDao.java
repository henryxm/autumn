package cn.org.autumn.modules.bot.dao;

import cn.org.autumn.modules.bot.dao.sql.RobotTokenDaoSql;
import cn.org.autumn.modules.bot.entity.RobotTokenEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Mapper
@Repository
public interface RobotTokenDao extends BaseMapper<RobotTokenEntity> {

    @SelectProvider(type = RobotTokenDaoSql.class, method = "getByUuid")
    RobotTokenEntity getByUuid(@Param("uuid") String uuid);

    @SelectProvider(type = RobotTokenDaoSql.class, method = "getByTokenHash")
    RobotTokenEntity getByTokenHash(@Param("token") String token);

    @SelectProvider(type = RobotTokenDaoSql.class, method = "listByTokenPrefix")
    List<RobotTokenEntity> listByTokenPrefix(@Param("tokenPrefix") String tokenPrefix);

    @SelectProvider(type = RobotTokenDaoSql.class, method = "listActiveByRobot")
    List<RobotTokenEntity> listActiveByRobot(@Param("robot") String robot);

    @SelectProvider(type = RobotTokenDaoSql.class, method = "countByRobot")
    int countByRobot(@Param("robot") String robot);

    @SelectProvider(type = RobotTokenDaoSql.class, method = "countActiveByRobot")
    int countActiveByRobot(@Param("robot") String robot);

    @UpdateProvider(type = RobotTokenDaoSql.class, method = "revokeByRobot")
    int revokeByRobot(@Param("robot") String robot, @Param("updateTime") Date updateTime);

    @SelectProvider(type = RobotTokenDaoSql.class, method = "getOldestRevokedByRobot")
    RobotTokenEntity getOldestRevokedByRobot(@Param("robot") String robot);

    @DeleteProvider(type = RobotTokenDaoSql.class, method = "deleteOldestRevokedByRobot")
    int deleteOldestRevokedByRobot(@Param("robot") String robot);

    @DeleteProvider(type = RobotTokenDaoSql.class, method = "deleteOldestByRobot")
    int deleteOldestByRobot(@Param("robot") String robot);

    @UpdateProvider(type = RobotTokenDaoSql.class, method = "revokeByUuid")
    int revokeByUuid(@Param("uuid") String uuid, @Param("updateTime") Date updateTime);
}
