package cn.org.autumn.modules.gen.ex;

import cn.org.autumn.modules.gen.dao.GenTypeDao;
import cn.org.autumn.modules.gen.entity.GenTypeEntity;
import cn.org.autumn.modules.gen.entity.GenTypeWrapper;
import cn.org.autumn.modules.gen.service.GenTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class GenTypeExService extends GenTypeService {

    @Autowired
    private GenTypeDao genTypeDao;

    public int menuOrder() {
        return 14;
    }

    public int parentMenu() {
        return 1;
    }

    @PostConstruct
    public void init() {
        super.init();
        if (!tableInit.init)
            return;
        String[][] mapping = new String[][]{
                {NULL, "mysql", "cn.org.autumn", "cn.org.autumn.modules", "sys", "系统管理", "Shaohua Xu", "henryxm@163.com", "tb",
                        "tinyint=Integer,smallint=Integer,mediumint=Integer,int=Integer,integer=Integer,bigint=Long,float=Float," +
                                "double=Double,decimal=BigDecimal,bit=Boolean,char=String,varchar=String,tinytext=String,text=String," +
                                "mediumtext=String,longtext=String,date=Date,datetime=Date,timestamp=Date"},
        };

        for (String[] map : mapping) {
            GenTypeEntity entity = new GenTypeEntity();
            String temp = map[0];
            if (NULL != temp)
                entity.setId(Long.valueOf(temp));
            temp = map[1];
            if (NULL != temp)
                entity.setDatabaseType(temp);
            temp = map[2];
            if (NULL != temp)
                entity.setRootPackage(temp);
            temp = map[3];
            if (NULL != temp)
                entity.setModulePackage(temp);
            temp = map[4];
            if (NULL != temp)
                entity.setModuleName(temp);
            temp = map[5];
            if (NULL != temp)
                entity.setModuleText(temp);
            temp = map[6];
            if (NULL != temp)
                entity.setAuthorName(temp);
            temp = map[7];
            if (NULL != temp)
                entity.setEmail(temp);
            temp = map[8];
            if (NULL != temp)
                entity.setTablePrefix(temp);
            temp = map[9];
            if (NULL != temp)
                entity.setMappingString(temp);
            GenTypeEntity et = genTypeDao.selectOne(entity);
            if (null == et)
                genTypeDao.insert(entity);
        }
    }

    public GenTypeWrapper getGenType(String databaseType) {
        GenTypeEntity entity = new GenTypeEntity();
        entity.setDatabaseType(databaseType);
        entity = genTypeDao.selectOne(entity);
        if (null != entity) {
            GenTypeWrapper wrapper = new GenTypeWrapper(entity);
            return wrapper;
        }
        return null;
    }
}
