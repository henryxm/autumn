package cn.org.autumn.modules.sys.dao;

import cn.org.autumn.modules.sys.dao.sql.SysShiroSessionDaoSql;
import cn.org.autumn.modules.sys.entity.SysShiroSessionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SysShiroSessionDao extends BaseMapper<SysShiroSessionEntity> {

    @SelectProvider(type = SysShiroSessionDaoSql.class, method = "getBySessionId")
    SysShiroSessionEntity getBySessionId(@Param("sessionId") String sessionId);

    /** 按业务唯一键 session_id 更新；0 行表示尚不存在，调用方再 insert。 */
    @UpdateProvider(type = SysShiroSessionDaoSql.class, method = "updateBySessionId")
    int updateBySessionId(SysShiroSessionEntity entity);

    @DeleteProvider(type = SysShiroSessionDaoSql.class, method = "deleteBySessionId")
    int deleteBySessionId(@Param("sessionId") String sessionId);

    @DeleteProvider(type = SysShiroSessionDaoSql.class, method = "deleteExpiredBefore")
    int deleteExpiredBefore(@Param("now") Date now);

    @DeleteProvider(type = SysShiroSessionDaoSql.class, method = "deleteByUser")
    int deleteByUser(@Param("user") String user);

    @SelectProvider(type = SysShiroSessionDaoSql.class, method = "listValidSessionIdsByUser")
    List<String> listValidSessionIdsByUser(@Param("user") String user, @Param("now") Date now,
            @Param("limit") long limit, @Param("offset") long offset);

    @SelectProvider(type = SysShiroSessionDaoSql.class, method = "listValidSessionIds")
    List<String> listValidSessionIds(@Param("now") Date now, @Param("limit") long limit, @Param("offset") long offset);
}
