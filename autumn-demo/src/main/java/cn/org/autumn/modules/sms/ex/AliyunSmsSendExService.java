package cn.org.autumn.modules.sms.ex;

import cn.org.autumn.modules.sms.service.AliyunSmsSendService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;


/**
 * 发送验证码控制器
 *
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10-06 17:06:59
 */

@Service
public class AliyunSmsSendExService extends AliyunSmsSendService {

    public int menuOrder(){
        return 10;
    }

    public int parentMenu(){
        return 1;
    }

    @PostConstruct
    public void init(){
        super.init();
    }

}
