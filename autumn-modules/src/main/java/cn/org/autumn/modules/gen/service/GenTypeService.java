package cn.org.autumn.modules.gen.service;

import cn.org.autumn.modules.gen.entity.GenTypeEntity;
import cn.org.autumn.modules.gen.entity.GenTypeWrapper;
import cn.org.autumn.modules.gen.service.gen.GenTypeServiceGen;
import cn.org.autumn.modules.sys.service.SysMenuService;
import org.springframework.stereotype.Service;

@Service
public class GenTypeService extends GenTypeServiceGen {

    @Override
    public String ico() {
        return "fa-file-text";
    }

    public int menuOrder() {
        return 14;
    }

    public String parentMenu() {
        super.parentMenu();
        return SysMenuService.getSystemManagementMenuKey();
    }

    public void init() {
        String[][] mapping = new String[][]{
                {null, "mysql", "cn.org.autumn", "cn.org.autumn.modules", "sys", "系统管理", SysMenuService.getSystemManagementMenuKey(), "User", "henryxm@163.com", "sys",
                        "tinyint=Integer,smallint=Integer,mediumint=Integer,int=Integer,integer=Integer,bigint=Long,float=Float," +
                                "double=Double,decimal=BigDecimal,bit=Boolean,char=String,varchar=String,tinytext=String,text=String," +
                                "mediumtext=String,longtext=String,date=Date,datetime=Date,timestamp=Date"},
        };

        for (String[] map : mapping) {
            GenTypeEntity entity = new GenTypeEntity();
            String temp = map[0];
            if (null != temp)
                entity.setId(Long.valueOf(temp));
            temp = map[1];
            if (null != temp)
                entity.setDatabaseType(temp);
            temp = map[2];
            if (null != temp)
                entity.setRootPackage(temp);
            temp = map[3];
            if (null != temp)
                entity.setModulePackage(temp);
            temp = map[4];
            if (null != temp)
                entity.setModuleName(temp);
            temp = map[5];
            if (null != temp)
                entity.setModuleText(temp);
            temp = map[6];
            if (null != temp)
                entity.setModuleId(temp);
            temp = map[7];
            if (null != temp)
                entity.setAuthorName(temp);
            temp = map[8];
            if (null != temp)
                entity.setEmail(temp);
            temp = map[9];
            if (null != temp)
                entity.setTablePrefix(temp);
            temp = map[10];
            if (null != temp)
                entity.setMappingString(temp);
            try {
                GenTypeEntity et = baseMapper.selectOne(entity);
                if (null == et)
                    baseMapper.insert(entity);
            } catch (Exception e) {
            }
        }
        super.init();
    }

    public String[][] getMenuItems() {
        String[][] menus = new String[][]{
                {"复制", null, "gen:gentype:copy", "2", "fa " + ico(), order(), button("Copy"), menu(), "sys_string_copy"},
        };
        return menus;
    }

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{
                {"gen_gentype_table_comment", "生成方案", "Generate solution"},
                {"gen_gentype_column_id", "序列号", "Serial number"},
                {"gen_gentype_column_database_type", "数据库类型", "Database type"},
                {"gen_gentype_column_root_package", "程序根包名", "Root package name"},
                {"gen_gentype_column_module_package", "模块根包名", "Module package name"},
                {"gen_gentype_column_module_name", "模块名(用于包名)", "Module name"},
                {"gen_gentype_column_module_text", "模块名称(用于目录)", "Module menu name"},
                {"gen_gentype_column_module_id", "模块ID(用于目录)", "Module ID"},
                {"gen_gentype_column_author_name", "作者名字", "Author name"},
                {"gen_gentype_column_email", "作者邮箱", "Author email"},
                {"gen_gentype_column_table_prefix", "表前缀", "Table prefix"},
                {"gen_gentype_column_mapping_string", "表字段映射", "Table mapping"},
                //
                {"gen_string_package_name", "包名", "Package name"},
                {"gen_string_table_prefix", "表前缀", "Table prefix"},
        };
        return items;
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

    public void copy(Long[] ids) {
        for (Long id : ids) {
            GenTypeEntity genTypeEntity = selectById(id);
            genTypeEntity.setId(null);
            insert(genTypeEntity);
        }
    }
}
