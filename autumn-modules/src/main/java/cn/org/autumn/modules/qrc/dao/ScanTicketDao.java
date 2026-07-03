package cn.org.autumn.modules.qrc.dao;

import cn.org.autumn.modules.qrc.dao.sql.ScanTicketDaoSql;
import cn.org.autumn.modules.qrc.entity.ScanTicketEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

@Repository
public interface ScanTicketDao extends BaseMapper<ScanTicketEntity> {

    @SelectProvider(type = ScanTicketDaoSql.class, method = "getByUuid")
    ScanTicketEntity getByUuid(@Param("uuid") String uuid);

    @UpdateProvider(type = ScanTicketDaoSql.class, method = "deleteExpiredBefore")
    int deleteExpiredBefore(@Param("beforeTime") java.util.Date beforeTime);
}
