package cn.org.autumn.modules.gen.service;

import cn.org.autumn.modules.gen.entity.GenTypeEntity;
import cn.org.autumn.modules.gen.entity.GenTypeWrapper;
import cn.org.autumn.modules.gen.service.gen.GenTypeServiceGen;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class GenTypeService extends GenTypeServiceGen {

    @Override
    public String ico() {
        return super.ico();
    }

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
                {NULL, "mysql" , "cn.org.autumn" , "cn.org.autumn.modules" , "sys" , "系统管理" , "1" , "Shaohua Xu" , "henryxm@163.com" , "tb" ,
                        "tinyint=Integer,smallint=Integer,mediumint=Integer,int=Integer,integer=Integer,bigint=Long,float=Float," +
                                "double=Double,decimal=BigDecimal,bit=Boolean,char=String,varchar=String,tinytext=String,text=String," +
                                "mediumtext=String,longtext=String,date=Date,datetime=Date,timestamp=Date" },
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
                entity.setModuleId(temp);
            temp = map[7];
            if (NULL != temp)
                entity.setAuthorName(temp);
            temp = map[8];
            if (NULL != temp)
                entity.setEmail(temp);
            temp = map[9];
            if (NULL != temp)
                entity.setTablePrefix(temp);
            temp = map[10];
            if (NULL != temp)
                entity.setMappingString(temp);
            GenTypeEntity et = baseMapper.selectOne(entity);
            if (null == et)
                baseMapper.insert(entity);
        }
    }

    public GenTypeWrapper getGenType(String databaseType) {
        GenTypeEntity entity = new GenTypeEntity();
        entity.setDatabaseType(databaseType);
        entity = baseMapper.selectOne(entity);
        if (null != entity) {
            GenTypeWrapper wrapper = new GenTypeWrapper(entity);
            return wrapper;
        }
        return null;
    }
}
