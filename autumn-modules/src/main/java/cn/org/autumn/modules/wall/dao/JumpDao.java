package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.dao.sql.WallDaoSql;
import cn.org.autumn.modules.wall.entity.JumpEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 攻击跳转
 *
 * @author User
 * @email henryxm@163.com
 * @date 2024-11
 */
@Mapper
@Repository
public interface JumpDao extends BaseMapper<JumpEntity> {

    @SelectProvider(type = WallDaoSql.class, method = "jumpEnabled")
    List<JumpEntity> gets();
}
