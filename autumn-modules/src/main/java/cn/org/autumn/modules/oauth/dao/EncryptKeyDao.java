package cn.org.autumn.modules.oauth.dao;

import cn.org.autumn.modules.oauth.entity.EncryptKeyEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Date;

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

    /**
     * 删除过期的加密密钥记录
     * 删除条件：expire不为空且expire < cleanBeforeTime
     *
     * @param cleanBeforeTime 清理时间点，expire小于此时间的记录将被删除
     * @return 删除的记录数量
     */
    @Delete("DELETE FROM oauth_encrypt_key WHERE expire IS NOT NULL AND expire < #{cleanBeforeTime}")
    int deleteExpiredKeys(@Param("cleanBeforeTime") Date cleanBeforeTime);
}
