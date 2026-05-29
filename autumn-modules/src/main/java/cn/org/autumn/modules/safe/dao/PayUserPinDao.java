package cn.org.autumn.modules.safe.dao;

import cn.org.autumn.modules.safe.dao.sql.PayUserPinDaoSql;
import cn.org.autumn.modules.safe.entity.PayUserPinEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface PayUserPinDao extends BaseMapper<PayUserPinEntity> {

    @SelectProvider(type = PayUserPinDaoSql.class, method = "getByUserUuid")
    PayUserPinEntity getByUserUuid(@Param("userUuid") String userUuid);
}
