package cn.org.autumn.modules.gen.service;

import cn.org.autumn.modules.gen.entity.GenTypeEntity;
import cn.org.autumn.modules.gen.entity.GenTypeWrapper;
import cn.org.autumn.modules.gen.utils.GenUtils;
import cn.org.autumn.modules.lan.service.LanguageService;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.site.InitFactory;
import cn.org.autumn.table.dao.TableDao;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.data.UniqueKeyInfo;
import cn.org.autumn.table.mysql.ColumnMeta;
import cn.org.autumn.table.mysql.TableMeta;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器
 */
@Service
public class GeneratorService implements InitFactory.Init {

    @Autowired
    private SysMenuService sysMenuService;

    @Autowired
    private TableDao generatorDao;

    @Autowired
    private GenTypeService genTypeService;

    @Autowired
    protected LanguageService languageService;

    private GenTypeWrapper wrapper;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<TableMeta> page = new Page<>();
        Query query = new Query(params);
        page.setRecords(queryList(query));
        page.setTotal(generatorDao.getTableCount());
        page.setCurrent(query.getCurrPage());
        page.setSize(query.getLimit());
        return new PageUtils(page);
    }

    public List<TableMeta> queryList(Query query) {
        String tableName = "";
        int offset = 0;
        int limit = 10;
        if (query.containsKey("limit"))
            limit = (int) query.get("limit");
        if (query.containsKey("offset"))
            offset = (int) query.get("offset");
        if (query.containsKey("tableName"))
            tableName = (String) query.get("tableName");
        return generatorDao.getTableMetasPage(tableName, offset, limit);
    }

    public List<TableMeta> queryTable(String tableName) {
        return generatorDao.getTableMetas(tableName);
    }

    public List<UniqueKeyInfo> showKeys(String tableName) {
        return generatorDao.getTableKeys(tableName);
    }

    public List<IndexInfo> showIndex(String tableName) {
        return generatorDao.getTableIndex(tableName);
    }

    public List<ColumnMeta> queryColumns(String tableName) {
        return generatorDao.getColumnMetas(tableName);
    }

    public TableInfo toTableInfo(TableMeta table) {
        TableInfo tableInfo = new TableInfo(table);
        tableInfo.setPrefix(wrapper.getEntity().getTablePrefix());
        return tableInfo;
    }

    public ColumnInfo toColumnInfo(ColumnMeta meta, GenTypeWrapper wrapper) {
        ColumnInfo columnInfo = new ColumnInfo(meta);
        String attrType = wrapper.getMapping().get(meta.getDataType());
        columnInfo.setAttrType(attrType);
        return columnInfo;
    }

    public TableInfo build(String tableName, GenTypeWrapper wrapper) {
        List<TableMeta> table = queryTable(tableName);
        List<ColumnMeta> columns = queryColumns(tableName);
        TableInfo tableInfo = toTableInfo(table.get(0));
        tableInfo.setIndexInfos(showIndex(tableName));
        List<ColumnInfo> list = new ArrayList<>();
        for (ColumnMeta columnMeta : columns) {
            ColumnInfo columnInfo = toColumnInfo(columnMeta, wrapper);
            if (columnInfo.isKey() && null == tableInfo.getPk())
                tableInfo.setPk(columnInfo);
            list.add(columnInfo);
            if ("BigDecimal".equalsIgnoreCase(columnInfo.getAttrType())) {
                tableInfo.setHasBigDecimal(true);
            }
        }
        tableInfo.setColumns(list);
        if (null == tableInfo.getPk())
            tableInfo.setPk(list.get(0));
        return tableInfo;
    }

    public byte[] generatorCode(String[] tableNames, String genId) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        GenTypeEntity entity = genTypeService.getById(genId);
        String menuKey = entity.getModuleId();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(menuKey);
        wrapper = new GenTypeWrapper(entity, sysMenuEntity);
        List<Map<String, Object>> tables = new ArrayList<>();
        for (String tableName : tableNames) {
            TableInfo tableInfo = build(tableName, wrapper);
            tableInfo.setModule(null != entity.getModuleName() ? entity.getModuleName() : "");
            tableInfo.setPrefix(null != entity.getTablePrefix() ? entity.getTablePrefix() : "");
            //生成代码
            GenUtils.generatorCode(tableInfo, wrapper, zip, GenUtils.getTemplates(), tables);
        }
        GenUtils.generatorCode(null, wrapper, zip, GenUtils.getSiteTemplates(), tables);
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

    /**
     * need implement it in the subclass.
     *
     * @return
     */
    public int menuOrder() {
        return 13;
    }

    /**
     * need implement it in the subclass.
     *
     * @return
     */

    public String parentMenu() {
        return SysMenuService.getSystemManagementMenuKey();
    }

    private String order() {
        return String.valueOf(menuOrder());
    }

    protected String ico() {
        return "fa-codepen";
    }

    public void init() {
        String keyMenu = SysMenuService.getMenuKey("Gen", "Generator");
        String[][] menus = new String[][]{
                {"代码生成", "modules/gen/generator", "gen:generator:list,gen:generator:code", "1", "fa " + ico(), order(), keyMenu, parentMenu(), "sys_string_code_generator"},
                {"查看", null, "gen:generator:list,gen:generator:info", "2", null, order(), SysMenuService.getMenuKey("Gen", "GeneratorInfo"), keyMenu, "sys_string_lookup"},
                {"生成", null, "gen:generator:code", "2", null, order(), SysMenuService.getMenuKey("Gen", "GeneratorCode"), keyMenu, "sys_string_generate"},
                {"重置表", null, "gen:generator:reset", "2", null, order(), SysMenuService.getMenuKey("Gen", "ResetTable"), keyMenu, "sys_string_reset_table"},
        };
        sysMenuService.put(menus);
        initVelocity();
    }

    public void initVelocity() {
        Properties prop = new Properties();
        prop.setProperty(Velocity.RESOURCE_LOADERS, "class");
        prop.setProperty("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        prop.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
        Velocity.init(prop);
    }
}
