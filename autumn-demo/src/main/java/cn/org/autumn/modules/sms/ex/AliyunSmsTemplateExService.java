package cn.org.autumn.modules.sms.ex;

import cn.org.autumn.modules.sms.service.AliyunSmsTemplateService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;


/**
 * 阿里云短信模板控制器
 *
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10-06 17:06:59
 */

@Service
public class AliyunSmsTemplateExService extends AliyunSmsTemplateService {

    public int menuOrder(){
        return 12;
    }

    public int parentMenu(){
        return 1;
    }

    @PostConstruct
    public void init(){
        super.init();
    }
}
