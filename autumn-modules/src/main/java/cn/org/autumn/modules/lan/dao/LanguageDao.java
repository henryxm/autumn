package cn.org.autumn.modules.lan.dao;

import cn.org.autumn.modules.lan.dao.sql.LanguageDaoSql;
import cn.org.autumn.modules.lan.entity.LanguageEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
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

    @SelectProvider(type = LanguageDaoSql.class, method = "hasKey")
    Integer hasKey(@Param("name") String name);

    @SelectProvider(type = LanguageDaoSql.class, method = "getByNameTag")
    LanguageEntity getByNameTag(@Param("name") String name, @Param("tag") String tag);

    @SelectProvider(type = LanguageDaoSql.class, method = "load")
    List<LanguageEntity> load();
}
