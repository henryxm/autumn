package cn.org.autumn.modules.test.service;

import cn.org.autumn.base.ModuleService;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.test.dao.DemoExampleDao;
import cn.org.autumn.modules.test.entity.DemoExampleEntity;

@Service
public class DemoExampleService extends ModuleService<DemoExampleDao, DemoExampleEntity> {

}
