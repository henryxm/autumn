package cn.org.autumn.modules.test.dao;

import cn.org.autumn.modules.test.entity.DemoExampleEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 测试例子
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */

@Mapper
@Repository
public interface DemoExampleDao extends BaseMapper<DemoExampleEntity> {
	
}
