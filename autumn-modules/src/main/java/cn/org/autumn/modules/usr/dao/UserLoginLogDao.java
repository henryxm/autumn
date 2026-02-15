package cn.org.autumn.modules.usr.dao;

import cn.org.autumn.modules.usr.entity.UserLoginLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 登录日志
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface UserLoginLogDao extends BaseMapper<UserLoginLogEntity> {
	
}
