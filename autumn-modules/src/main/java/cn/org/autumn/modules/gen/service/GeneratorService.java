package cn.org.autumn.modules.gen.service;

import cn.org.autumn.modules.gen.entity.GenTypeEntity;
import cn.org.autumn.modules.gen.entity.GenTypeWrapper;
import cn.org.autumn.modules.gen.ex.GenTypeExService;
import cn.org.autumn.modules.gen.utils.GenUtils;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.table.TableInit;
import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.dao.TableDao;
import cn.org.autumn.table.data.ColumnInfo;
import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.mysql.ColumnMeta;
import cn.org.autumn.table.mysql.TableMeta;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.plugins.pagination.PageHelper;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器
 */
@Service
public class GeneratorService {

    @Autowired
    private SysMenuService sysMenuService;

    @Autowired
    private TableDao generatorDao;

    @Autowired
    private GenTypeExService genTypeExService;

    @Autowired
    private TableInit tableInit;

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
        PageHelper.startPage(query.getPage().getSize(), query.getLimit());
        String tableName = "";
        int offset = 0;
        int limit = 10;
        if (query.containsKey("limit"))
            limit = (int) query.get("limit");
        if (query.containsKey("offset"))
            offset = (int) query.get("offset");
        if (query.containsKey("tableName"))
            tableName = (String) query.get("tableName");

        List<TableMeta> list = generatorDao.getTableMetas(tableName, offset, limit);

        return list;
    }

    public List<TableMeta> queryTable(String tableName) {
        return generatorDao.getTableMetas(tableName);
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
        List<ColumnInfo> list = new ArrayList<>();
        for (ColumnMeta columnMeta : columns) {
            ColumnInfo columnInfo = toColumnInfo(columnMeta, wrapper);
            if (columnInfo.isKey() && null == tableInfo.getPk())
                tableInfo.setPk(columnInfo);
            list.add(columnInfo);
            if("BigDecimal".equalsIgnoreCase(columnInfo.getAttrType())){
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
        GenTypeEntity entity = genTypeExService.selectById(genId);
        wrapper = new GenTypeWrapper(entity);
        for (String tableName : tableNames) {
            TableInfo tableInfo = build(tableName, wrapper);
            //生成代码
            GenUtils.generatorCode(tableInfo, wrapper, zip);
        }
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
    public int parentMenu() {
        return 1;
    }

    private String order() {
        return String.valueOf(menuOrder());
    }

    private String parent() {
        return String.valueOf(parentMenu());
    }

    @PostConstruct
    public void init() {
        if (!tableInit.init)
            return;
        Long id = 0L;
        String[] _m = new String[]
                {null, parent(), "代码生成", "modules/gen/generator.html", "gen:generator:list,gen:generator:code", "1", "fa fa-file-code-o", order()};
        SysMenuEntity sysMenu = sysMenuService.from(_m);
        SysMenuEntity entity = sysMenuService.get(sysMenu);
        if (null == entity) {
            int ret = sysMenuService.put(sysMenu);
            if (1 == ret)
                id = sysMenu.getMenuId();
        } else
            id = entity.getMenuId();
        String[][] menus = new String[][]{
                {null, id + "", "查看", null, "gen:generator:list,gen:generator:info", "2", null, order()},
                {null, id + "", "生成", null, "gen:generator:code", "2", null, order()},
        };
        for (String[] menu : menus) {
            sysMenu = sysMenuService.from(menu);
            entity = sysMenuService.get(sysMenu);
            if (null == entity) {
                sysMenuService.put(sysMenu);
            }
        }
    }
}
