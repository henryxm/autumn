package cn.org.autumn.modules.sms.dao;

import cn.org.autumn.modules.sms.entity.AliyunSmsSendEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 发送验证码
 * 
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */

@Mapper
@Repository
public interface AliyunSmsSendDao extends BaseMapper<AliyunSmsSendEntity> {
	
}
