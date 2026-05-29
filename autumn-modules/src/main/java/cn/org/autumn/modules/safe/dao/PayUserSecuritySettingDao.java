package cn.org.autumn.modules.safe.dao;

import cn.org.autumn.modules.safe.dao.sql.PayUserSecuritySettingDaoSql;
import cn.org.autumn.modules.safe.entity.PayUserSecuritySettingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface PayUserSecuritySettingDao extends BaseMapper<PayUserSecuritySettingEntity> {

    @SelectProvider(type = PayUserSecuritySettingDaoSql.class, method = "getByUserUuid")
    PayUserSecuritySettingEntity getByUserUuid(@Param("userUuid") String userUuid);
}
