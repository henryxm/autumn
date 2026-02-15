package cn.org.autumn.modules.lan.dao;

import cn.org.autumn.modules.lan.entity.LanguageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 国家语言
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

@Mapper
@Repository
public interface LanguageDao extends BaseMapper<LanguageEntity> {

    @Select("select count(*) from sys_language where name = #{name}")
    Integer hasKey(@Param("name") String name);

    @Select("select * from sys_language where name = #{name} and tag = #{tag} limit 1")
    LanguageEntity getByNameTag(@Param("name") String name, @Param("tag") String tag);

    @Select("select * from sys_language where tag is null or tag = ''")
    List<LanguageEntity> load();
}
