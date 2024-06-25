package cn.org.autumn.modules.usr.dao;

import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 用户Token
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface UserTokenDao extends BaseMapper<UserTokenEntity> {

    @Select("select * from usr_user_token where user_uuid = #{uuid} limit 1")
    UserTokenEntity getByUuid(@Param("uuid") String uuid);
	
}
