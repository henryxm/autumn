package cn.org.autumn.modules.stock.dao;

import cn.org.autumn.modules.stock.entity.StockVisitRecodeEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 股票用户访问记录
 * 
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */

@Mapper
@Repository
public interface StockVisitRecodeDao extends BaseMapper<StockVisitRecodeEntity> {
	
}
