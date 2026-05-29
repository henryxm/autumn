package cn.org.autumn.modules.bot.dao;

import cn.org.autumn.modules.bot.dao.sql.RobotDaoSql;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.Date;
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

    @SelectProvider(type = RobotDaoSql.class, method = "countByOwnerForQuota")
    int countByOwnerForQuota(@Param("owner") String owner);

    @SelectProvider(type = RobotDaoSql.class, method = "countSoftDeletedByOwner")
    int countSoftDeletedByOwner(@Param("owner") String owner);

    @SelectProvider(type = RobotDaoSql.class, method = "listUuidsSoftDeletedByOwner")
    List<String> listUuidsSoftDeletedByOwner(@Param("owner") String owner);

    @SelectProvider(type = RobotDaoSql.class, method = "listUuidsDeletedBefore")
    List<String> listUuidsDeletedBefore(@Param("beforeTime") Date beforeTime);

    @SelectProvider(type = RobotDaoSql.class, method = "countByHashInUse")
    int countByHashInUse(@Param("hash") String hash);
}
