package cn.org.autumn.modules.stock.ex;

import cn.org.autumn.modules.stock.service.StockVisitRecodeService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;


/**
 * 访问记录控制器
 *
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-09-25 16:38:08
 */

@Service
public class StockVisitRecodeExService extends StockVisitRecodeService {

    public int menuOrder(){
        return 9;
    }

    public int parentMenu(){
        return 1;
    }

    @PostConstruct
    public void init(){
        super.init();
    }

}
