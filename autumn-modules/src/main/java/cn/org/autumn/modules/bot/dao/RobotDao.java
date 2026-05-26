package cn.org.autumn.modules.bot.dao;

import cn.org.autumn.modules.bot.dao.sql.RobotDaoSql;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface RobotDao extends BaseMapper<RobotEntity> {

    @SelectProvider(type = RobotDaoSql.class, method = "getByUuid")
    RobotEntity getByUuid(@Param("uuid") String uuid);

    @SelectProvider(type = RobotDaoSql.class, method = "countByUuid")
    int countByUuid(@Param("uuid") String uuid);

    @SelectProvider(type = RobotDaoSql.class, method = "listByOwner")
    List<RobotEntity> listByOwner(@Param("owner") String owner);

    @SelectProvider(type = RobotDaoSql.class, method = "listByOwnerManaged")
    List<RobotEntity> listByOwnerManaged(@Param("owner") String owner);

    @SelectProvider(type = RobotDaoSql.class, method = "countByOwner")
    int countByOwner(@Param("owner") String owner);
}
