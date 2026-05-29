package cn.org.autumn.modules.safe.dao;

import cn.org.autumn.modules.safe.dao.sql.PayUserTrustedIpDaoSql;
import cn.org.autumn.modules.safe.entity.PayUserTrustedIpEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface PayUserTrustedIpDao extends BaseMapper<PayUserTrustedIpEntity> {

    @SelectProvider(type = PayUserTrustedIpDaoSql.class, method = "getByUserAndIp")
    PayUserTrustedIpEntity getByUserAndIp(@Param("userUuid") String userUuid, @Param("ip") String ip);

    @SelectProvider(type = PayUserTrustedIpDaoSql.class, method = "listByUser")
    List<PayUserTrustedIpEntity> listByUser(@Param("userUuid") String userUuid);
}
