package cn.org.autumn.modules.safe.dao;

import cn.org.autumn.modules.safe.entity.PayCredentialLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface PayCredentialLogDao extends BaseMapper<PayCredentialLogEntity> {
}
