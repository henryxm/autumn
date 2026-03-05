package cn.org.autumn.modules.oauth.dao;

import cn.org.autumn.modules.oauth.entity.SecurityRequestEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * 安全验证
 * 
 * @author User
 * @email henryxm@163.com
 * @date 2026-03
 */
@Mapper
@Repository
public interface SecurityRequestDao extends BaseMapper<SecurityRequestEntity> {

    @Select("select * from oauth_security_request where enabled = 1 order by create desc limit 1")
    SecurityRequestEntity getLatestEnabled();

    @Select("select * from oauth_security_request where enabled = 1 and auth = #{auth} limit 1")
    SecurityRequestEntity getByAuth(@Param("auth") String auth);

    @Delete("delete from oauth_security_request where create < #{deadline}")
    int deleteByCreateBefore(@Param("deadline") Date deadline);
}
