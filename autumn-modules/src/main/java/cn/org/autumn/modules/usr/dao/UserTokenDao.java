package cn.org.autumn.modules.usr.dao;

import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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

    @Delete("delete from usr_user_token where `user_uuid` = #{userUuid}")
    void deleteByUserUuid(@Param("userUuid") String userUuid);
}
