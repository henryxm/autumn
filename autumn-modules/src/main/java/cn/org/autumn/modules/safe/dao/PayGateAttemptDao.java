package cn.org.autumn.modules.safe.dao;

import cn.org.autumn.modules.safe.dao.sql.PayGateAttemptDaoSql;
import cn.org.autumn.modules.safe.entity.PayGateAttemptEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Mapper
@Repository
public interface PayGateAttemptDao extends BaseMapper<PayGateAttemptEntity> {

    @SelectProvider(type = PayGateAttemptDaoSql.class, method = "countSameAmountSince")
    int countSameAmountSince(@Param("userUuid") String userUuid, @Param("amountCent") long amountCent, @Param("since") Date since);

    @DeleteProvider(type = PayGateAttemptDaoSql.class, method = "deleteOlderThan")
    int deleteOlderThan(@Param("before") Date before);

    @SelectProvider(type = PayGateAttemptDaoSql.class, method = "countByOrderIdSince")
    int countByOrderIdSince(@Param("userUuid") String userUuid, @Param("orderId") String orderId, @Param("since") Date since);

    @SelectProvider(type = PayGateAttemptDaoSql.class, method = "countPasswordlessSince")
    int countPasswordlessSince(@Param("userUuid") String userUuid, @Param("since") Date since);

    @SelectProvider(type = PayGateAttemptDaoSql.class, method = "sumPasswordlessAmountSince")
    long sumPasswordlessAmountSince(@Param("userUuid") String userUuid, @Param("since") Date since);
}
