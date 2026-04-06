package cn.org.autumn.modules.usr.dao;

import cn.org.autumn.modules.usr.dao.sql.UserLoginLogDaoSql;
import cn.org.autumn.modules.usr.entity.UserLoginLogEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 登录日志
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@Mapper
@Repository
public interface UserLoginLogDao extends BaseMapper<UserLoginLogEntity> {

    @InsertProvider(type = UserLoginLogDaoSql.class, method = "insertCustom")
    int insertCustom(@Param("e") UserLoginLogEntity e);

    @SelectProvider(type = UserLoginLogDaoSql.class, method = "selectByUuidOrderByCreateDesc")
    List<UserLoginLogEntity> selectByUuidOrderByCreateDesc(@Param("uuid") String uuid);

    @UpdateProvider(type = UserLoginLogDaoSql.class, method = "updateWhiteByIdCustom")
    int updateWhiteByIdCustom(@Param("id") Long id, @Param("whiteVal") boolean whiteVal);

    @DeleteProvider(type = UserLoginLogDaoSql.class, method = "deleteByUuidPattern")
    int deleteByUuidPattern(@Param("pattern") String pattern);

    @SelectProvider(type = UserLoginLogDaoSql.class, method = "countByUuidAndWhite")
    int countByUuidAndWhite(@Param("uuid") String uuid, @Param("whiteVal") boolean whiteVal);
}
