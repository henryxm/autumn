package cn.org.autumn.modules;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.lan.dao.LanguageDao;
import cn.org.autumn.modules.lan.entity.LanguageEntity;
import org.springframework.stereotype.Service;

/**
 * 测试用的 LanguageService，继承 ModuleService 以支持缓存功能测试
 */
@Service
public class TestLanguageService extends ModuleService<LanguageDao, LanguageEntity> {
    
    @Override
    public String ico() {
        return "fa-language";
    }
}
