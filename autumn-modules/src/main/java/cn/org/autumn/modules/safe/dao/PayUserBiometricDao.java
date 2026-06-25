package cn.org.autumn.modules.safe.dao;

import cn.org.autumn.modules.safe.dao.sql.PayUserBiometricDaoSql;
import cn.org.autumn.modules.safe.entity.PayUserBiometricEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface PayUserBiometricDao extends BaseMapper<PayUserBiometricEntity> {

    @SelectProvider(type = PayUserBiometricDaoSql.class, method = "getByUserAndDevice")
    PayUserBiometricEntity getByUserAndDevice(@Param("userUuid") String userUuid, @Param("deviceId") String deviceId);

    @SelectProvider(type = PayUserBiometricDaoSql.class, method = "listActiveByUser")
    List<PayUserBiometricEntity> listActiveByUser(@Param("userUuid") String userUuid);

    @SelectProvider(type = PayUserBiometricDaoSql.class, method = "countActiveByUser")
    int countActiveByUser(@Param("userUuid") String userUuid);

    @UpdateProvider(type = PayUserBiometricDaoSql.class, method = "revokeByUserAndDevice")
    int revokeByUserAndDevice(@Param("userUuid") String userUuid, @Param("deviceId") String deviceId, @Param("updateTime") Date updateTime);
}
