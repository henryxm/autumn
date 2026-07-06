package cn.org.autumn.modules.opl.dao;

import cn.org.autumn.modules.opl.dao.sql.OplDaoSql;
import cn.org.autumn.modules.opl.entity.OpenUnionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface OpenUnionDao extends BaseMapper<OpenUnionEntity> {

    @SelectProvider(type = OplDaoSql.class, method = "openUnionByAccountAndUser")
    OpenUnionEntity getByAccountAndUser(@Param("account") String account, @Param("user") String user);

    @SelectProvider(type = OplDaoSql.class, method = "openUnionByUnionId")
    OpenUnionEntity getByUnionId(@Param("unionId") String unionId);
}
