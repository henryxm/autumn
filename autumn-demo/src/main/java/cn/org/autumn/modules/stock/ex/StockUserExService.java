package cn.org.autumn.modules.stock.ex;

import cn.org.autumn.modules.stock.service.StockUserService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;


/**
 * 股票用户控制器
 *
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-09-25 16:38:09
 */

@Service
public class StockUserExService extends StockUserService {

    public int menuOrder(){
        return 8;
    }

    public int parentMenu(){
        return 1;
    }

    @PostConstruct
    public void init(){
        super.init();
    }

}
