package cn.org.autumn.modules.sms.ex;

import cn.org.autumn.modules.sms.service.AliyunSmsSignService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;


/**
 * 短信签名控制器
 *
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10-06 17:06:59
 */

@Service
public class AliyunSmsSignExService extends AliyunSmsSignService {


    public int menuOrder(){
        return 11;
    }

    public int parentMenu(){
        return 1;
    }

    @PostConstruct
    public void init(){
        super.init();
    }
}
