package cn.org.autumn.modules.safe.dao;

import cn.org.autumn.modules.safe.dao.sql.PayUserTrustedDeviceDaoSql;
import cn.org.autumn.modules.safe.entity.PayUserTrustedDeviceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface PayUserTrustedDeviceDao extends BaseMapper<PayUserTrustedDeviceEntity> {

    @SelectProvider(type = PayUserTrustedDeviceDaoSql.class, method = "getByUserAndDevice")
    PayUserTrustedDeviceEntity getByUserAndDevice(@Param("userUuid") String userUuid, @Param("deviceId") String deviceId);

    @SelectProvider(type = PayUserTrustedDeviceDaoSql.class, method = "listByUser")
    List<PayUserTrustedDeviceEntity> listByUser(@Param("userUuid") String userUuid);
}
