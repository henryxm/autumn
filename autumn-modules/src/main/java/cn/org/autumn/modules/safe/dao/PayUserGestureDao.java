package cn.org.autumn.modules.safe.dao;

import cn.org.autumn.modules.safe.dao.sql.PayUserGestureDaoSql;
import cn.org.autumn.modules.safe.entity.PayUserGestureEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface PayUserGestureDao extends BaseMapper<PayUserGestureEntity> {

    @SelectProvider(type = PayUserGestureDaoSql.class, method = "getByUserUuid")
    PayUserGestureEntity getByUserUuid(@Param("userUuid") String userUuid);
}
