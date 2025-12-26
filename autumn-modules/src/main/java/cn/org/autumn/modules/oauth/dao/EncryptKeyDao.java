package cn.org.autumn.modules.oauth.dao;

import cn.org.autumn.modules.oauth.entity.EncryptKeyEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 加密秘钥
 *
 * @author User
 * @email henryxm@163.com
 * @date 2025-12
 */
@Mapper
@Repository
public interface EncryptKeyDao extends BaseMapper<EncryptKeyEntity> {

    @Select("select * from oauth_encrypt_key where `session` = #{session}")
    EncryptKeyEntity getBySession(@Param("session") String session);
}
