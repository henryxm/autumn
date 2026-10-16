package cn.org.autumn.modules.bot.dao;

import cn.org.autumn.modules.bot.dao.sql.GuestDaoSql;
import cn.org.autumn.modules.bot.entity.GuestEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Mapper
@Repository
public interface GuestDao extends BaseMapper<GuestEntity> {

    @SelectProvider(type = GuestDaoSql.class, method = "getByUuid")
    GuestEntity getByUuid(@Param("uuid") String uuid);

    @SelectProvider(type = GuestDaoSql.class, method = "countByUuid")
    int countByUuid(@Param("uuid") String uuid);

    @SelectProvider(type = GuestDaoSql.class, method = "getByGuestKey")
    GuestEntity getByGuestKey(@Param("guestKey") String guestKey);

    @DeleteProvider(type = GuestDaoSql.class, method = "deleteExpired")
    int deleteExpired(@Param("now") Date now);
}
