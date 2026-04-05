package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.dao.sql.WallDaoSql;
import cn.org.autumn.modules.wall.entity.ShieldEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * 攻击防御
 *
 * @author User
 * @email henryxm@163.com
 * @date 2024-11
 */
@Mapper
@Repository
public interface ShieldDao extends BaseMapper<ShieldEntity> {

    @SelectProvider(type = WallDaoSql.class, method = "shieldUris")
    Set<String> gets();

    @SelectProvider(type = WallDaoSql.class, method = "shieldDefault")
    ShieldEntity get();

    @SelectProvider(type = WallDaoSql.class, method = "shieldHasDefault")
    int has();
}
