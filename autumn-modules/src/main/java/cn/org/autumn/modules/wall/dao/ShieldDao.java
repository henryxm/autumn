package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.ShieldEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
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

    @Select("select s.uri from wall_shield as s where enable = 1")
    Set<String> gets();

    @Select("select * from wall_shield where uri = '/'")
    ShieldEntity get();

    @Select("select count(*) from wall_shield where uri = '/'")
    int has();
}
