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

    public String[][] getLanguageItemArray() {
        String[][] items = new String[][]{
                {"test_demoexample_table_comment", "测试例子", "Test example"},
                {"test_demoexample_column_example", "例子字段", "Example column"},
        };
        return items;
    }
}
