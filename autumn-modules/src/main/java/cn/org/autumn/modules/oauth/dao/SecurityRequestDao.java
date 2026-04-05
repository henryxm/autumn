package cn.org.autumn.modules.oauth.dao;

import cn.org.autumn.modules.oauth.entity.SecurityRequestEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import cn.org.autumn.modules.oauth.dao.sql.OauthInlineSql;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
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

    @SelectProvider(type = OauthInlineSql.class, method = "securityRequestLatest")
    SecurityRequestEntity getLatestEnabled();

    @SelectProvider(type = OauthInlineSql.class, method = "securityRequestByAuth")
    SecurityRequestEntity getByAuth(@Param("auth") String auth);

    @DeleteProvider(type = OauthInlineSql.class, method = "securityRequestDeleteBefore")
    int deleteByCreateBefore(@Param("deadline") Date deadline);
}
