package cn.org.autumn.modules.wall.dao;

import cn.org.autumn.modules.wall.entity.JumpEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

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

    @Select("select * from wall_jump where enable = 1")
    List<JumpEntity> gets();
}
