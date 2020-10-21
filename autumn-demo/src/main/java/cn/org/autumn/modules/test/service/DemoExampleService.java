package cn.org.autumn.modules.test.service;

import cn.org.autumn.modules.test.service.gen.DemoExampleServiceGen;
import org.springframework.stereotype.Service;

@Service
public class DemoExampleService extends DemoExampleServiceGen {

    @Override
    public int menuOrder(){
        return super.menuOrder();
    }

    @Override
    public String ico(){
        return super.ico();
    }
}
