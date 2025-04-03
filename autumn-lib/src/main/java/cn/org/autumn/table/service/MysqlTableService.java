package cn.org.autumn.table.service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArraySet;

import cn.org.autumn.table.annotation.UniqueKey;
import cn.org.autumn.table.annotation.UniqueKeys;
import cn.org.autumn.table.dao.TableDao;
import cn.org.autumn.table.data.*;
import cn.org.autumn.table.mysql.ColumnMeta;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.LengthCount;
import cn.org.autumn.table.utils.ClassTools;

import static cn.org.autumn.table.data.InitType.*;

@Transactional
@Service
public class MysqlTableService {

    private static final Logger log = LoggerFactory.getLogger(MysqlTableService.class);

    private static final List<String> ignoreLengthList = new ArrayList<>();

    private static final String defaultPackage = "cn.org.autumn.modules";

    static {
        ignoreLengthList.add("int");
        ignoreLengthList.add("integer");
        ignoreLengthList.add("bigint");
        ignoreLengthList.add("tinyint");
        ignoreLengthList.add("datetime");
    }

    @Autowired
    private TableDao tableDao;

    /**
     * 要扫描的model所在的pack
     */
    @Value("${autumn.table.pack:" + defaultPackage + "}")
    private String pack;

    /**
     * 自动创建模式：update表示更新，create表示删除原表重新创建，none 表示不执行操作
     */
    @Value("${autumn.table.auto:update}")
    private InitType type;

    public Set<String> getPacks() {
        return new CopyOnWriteArraySet<>(Arrays.asList(pack.split(",|:|;|-| ")));
    }

    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new CopyOnWriteArraySet<>();
        Set<String> packs = getPacks();
        //添加系统代码的默认包名，防止配置错误
        packs.add(defaultPackage);
        for (String pkg : packs) {
            if (!pkg.isEmpty())
                classes.addAll(ClassTools.getClasses(pkg));
        }
        return classes;
    }

    public void create() {
        // 从包package中获取所有的Class
        Set<Class<?>> classes = getClasses();
        createMysqlTable(classes, type);
    }

    public void create(Class<?> clazz, InitType type) {
        Set<Class<?>> classes = new CopyOnWriteArraySet<>();
        classes.add(clazz);
        createMysqlTable(classes, type);
    }

    public void create(Set<Class<?>> classes, InitType type) {
        createMysqlTable(classes, type);
    }

    /**
     * 读取配置文件的三种状态（创建表、更新表、不做任何事情）
     */
    private void createMysqlTable(Set<Class<?>> classes, InitType type) {

        // 不做任何事情
        if (none.equals(type)) {
            return;
        }

        // 获取Mysql的类型，以及类型需要设置几个长度
        Map<String, Object> mySqlTypeAndLengthMap = mySqlTypeAndLengthMap();

        // 用于存需要创建的表名+结构
        Map<TableInfo, List<Object>> newTableMap = new HashMap<TableInfo, List<Object>>();

        // 用于存需要更新字段类型等的表名+结构
        Map<TableInfo, List<Object>> modifyTableMap = new HashMap<TableInfo, List<Object>>();

        // 用于存需要增加字段的表名+结构
        Map<TableInfo, List<Object>> addTableMap = new HashMap<TableInfo, List<Object>>();

        // 用于存需要删除字段的表名+结构
        Map<TableInfo, List<Object>> removeTableMap = new HashMap<TableInfo, List<Object>>();

        // 用于存需要删除主键的表名+结构
        Map<TableInfo, List<Object>> dropKeyTableMap = new HashMap<TableInfo, List<Object>>();

        // 用于存需要删除唯一约束的表名+结构
        Map<TableInfo, List<Object>> dropUniqueTableMap = new HashMap<TableInfo, List<Object>>();

        // 用于存需要删除唯一约束的表名+结构
        Map<TableInfo, List<Object>> addIndexTableMap = new HashMap<TableInfo, List<Object>>();
        Map<TableInfo, List<Object>> removeIndexTableMap = new HashMap<TableInfo, List<Object>>();
        // 构建出全部表的增删改的map
        allTableMapConstruct(type, mySqlTypeAndLengthMap, classes, newTableMap, modifyTableMap, addTableMap, removeTableMap,
                dropKeyTableMap, dropUniqueTableMap, addIndexTableMap, removeIndexTableMap);

        // 根据传入的map，分别去创建或修改表结构
        createOrModifyTableConstruct(newTableMap, modifyTableMap, addTableMap, removeTableMap, dropKeyTableMap,
                dropUniqueTableMap, addIndexTableMap, removeIndexTableMap);
    }

    /**
     * 构建出全部表的增删改的map
     *
     * @param mySqlTypeAndLengthMap 获取Mysql的类型，以及类型需要设置几个长度
     * @param classes               从包package中获取所有的Class
     * @param newTableMap           用于存需要创建的表名+结构
     * @param modifyTableMap        用于存需要更新字段类型等的表名+结构
     * @param addTableMap           用于存需要增加字段的表名+结构
     * @param removeTableMap        用于存需要删除字段的表名+结构
     * @param dropKeyTableMap       用于存需要删除主键的表名+结构
     * @param dropUniqueTableMap    用于存需要删除唯一约束的表名+结构
     */
    private void allTableMapConstruct(InitType type,
                                      Map<String, Object> mySqlTypeAndLengthMap,
                                      Set<Class<?>> classes,
                                      Map<TableInfo, List<Object>> newTableMap,
                                      Map<TableInfo, List<Object>> modifyTableMap,
                                      Map<TableInfo, List<Object>> addTableMap,
                                      Map<TableInfo, List<Object>> removeTableMap,
                                      Map<TableInfo, List<Object>> dropKeyTableMap,
                                      Map<TableInfo, List<Object>> dropUniqueTableMap,
                                      Map<TableInfo, List<Object>> addIndexTableMap,
                                      Map<TableInfo, List<Object>> removeIndexTableMap) {
        for (Class<?> clazz : classes) {

            TableInfo tableInfo = new TableInfo(clazz);

            if (!tableInfo.isValid())
                continue;

            // 用于存新增表的字段
            List<Object> newFieldList = new ArrayList<Object>();
            // 用于存删除的字段
            List<Object> removeFieldList = new ArrayList<Object>();
            // 用于存新增的字段
            List<Object> addFieldList = new ArrayList<Object>();
            // 用于存修改的字段
            List<Object> modifyFieldList = new ArrayList<Object>();
            // 用于存删除主键的字段
            List<Object> dropKeyFieldList = new ArrayList<Object>();
            // 用于存删除唯一约束的字段
            List<Object> dropUniqueFieldList = new ArrayList<Object>();
            // 保存需要增加索引的信息
            List<Object> addIndexList = new ArrayList<Object>();
            // 保存需要删除索引的信息
            List<Object> removeIndexList = new ArrayList<Object>();

            // 迭代出所有model的所有fields存到newFieldList中
            tableFieldsConstruct(mySqlTypeAndLengthMap, clazz, newFieldList);

            // 如果配置文件配置的是create，表示将所有的表删掉重新创建
            if (create.equals(type)) {
                tableDao.dropTable(tableInfo.getName());
            }

            Boolean exist = tableDao.hasTable(tableInfo.getName());

            // 不存在时
            if (!exist) {
                newTableMap.put(tableInfo, newFieldList);
            } else {
                List<ColumnMeta> tableColumnList = tableDao.getColumnMetas(tableInfo.getName());

//                List<TableMeta> tableMetas = tableDao.getTableMetas("sys");

                List<String> columnNames = ClassTools.getPropertyValueList(tableColumnList,
                        ColumnMeta.COLUMN_NAME_KEY);

                // 验证对比从model中解析的fieldList与从数据库查出来的columnList
                // 1. 找出增加的字段
                // 2. 找出删除的字段
                // 3. 找出更新的字段
                buildAddAndRemoveAndModifyFields(mySqlTypeAndLengthMap, modifyTableMap, addTableMap, removeTableMap,
                        dropKeyTableMap, dropUniqueTableMap, addIndexTableMap, removeIndexTableMap, tableInfo, newFieldList, removeFieldList, addFieldList,
                        modifyFieldList, dropKeyFieldList, dropUniqueFieldList, addIndexList, removeIndexList, tableColumnList, columnNames);

            }
        }
    }

    /**
     * 构建增加的删除的修改的字段
     *
     * @param mySqlTypeAndLengthMap 获取Mysql的类型，以及类型需要设置几个长度
     * @param modifyTableMap        用于存需要更新字段类型等的表名+结构
     * @param addTableMap           用于存需要增加字段的表名+结构
     * @param removeTableMap        用于存需要删除字段的表名+结构
     * @param dropKeyTableMap       用于存需要删除主键的表名+结构
     * @param dropUniqueTableMap    用于存需要删除唯一约束的表名+结构
     * @param tableInfo             表
     * @param newFieldList          用于存新增表的字段
     * @param removeFieldList       用于存删除的字段
     * @param addFieldList          用于存新增的字段
     * @param modifyFieldList       用于存修改的字段
     * @param dropKeyFieldList      用于存删除主键的字段
     * @param dropUniqueFieldList   用于存删除唯一约束的字段
     * @param tableColumnList       已存在时理论上做修改的操作，这里查出该表的结构
     * @param columnNames           从sysColumns中取出我们需要比较的列的List
     */
    private void buildAddAndRemoveAndModifyFields(Map<String, Object> mySqlTypeAndLengthMap,
                                                  Map<TableInfo, List<Object>> modifyTableMap,
                                                  Map<TableInfo, List<Object>> addTableMap,
                                                  Map<TableInfo, List<Object>> removeTableMap,
                                                  Map<TableInfo, List<Object>> dropKeyTableMap,
                                                  Map<TableInfo, List<Object>> dropUniqueTableMap,
                                                  Map<TableInfo, List<Object>> addIndexTableMap,
                                                  Map<TableInfo, List<Object>> removeIndexTableMap,
                                                  TableInfo tableInfo,
                                                  List<Object> newFieldList,
                                                  List<Object> removeFieldList,
                                                  List<Object> addFieldList,
                                                  List<Object> modifyFieldList,
                                                  List<Object> dropKeyFieldList,
                                                  List<Object> dropUniqueFieldList,
                                                  List<Object> addIndexList,
                                                  List<Object> removeIndexList,
                                                  List<ColumnMeta> tableColumnList,
                                                  List<String> columnNames) {
        // 1. 找出增加的字段
        // 根据数据库中表的结构和model中表的结构对比找出新增的字段
        buildNewFields(addTableMap, tableInfo, newFieldList, addFieldList, columnNames);

        // 将fieldList转成Map类型，字段名作为主键
        Map<String, ColumnInfo> fieldMap = new HashMap<String, ColumnInfo>();
        for (Object obj : newFieldList) {
            ColumnInfo createTableParam = (ColumnInfo) obj;
            fieldMap.put(createTableParam.getName(), createTableParam);
        }

        // 2. 找出删除的字段
        buildRemoveFields(removeTableMap, tableInfo, removeFieldList, columnNames, fieldMap);

        // 3. 找出更新的字段
        buildModifyFields(mySqlTypeAndLengthMap, modifyTableMap, dropKeyTableMap, dropUniqueTableMap, addIndexTableMap, tableInfo,
                modifyFieldList, dropKeyFieldList, dropUniqueFieldList, addIndexList, tableColumnList, fieldMap);
        buildModifyIndex(tableInfo, addIndexTableMap, addIndexList, removeIndexTableMap, removeIndexList);
    }

    private Collection<IndexInfo> filter(List<IndexInfo> indexInfoList) {
        Map<String, IndexInfo> stringIndexInfoHashMap = new HashMap<>();
        for (IndexInfo indexInfo : indexInfoList) {
            indexInfo.resolve();
            String keyName = indexInfo.getKeyName();
            if ("PRIMARY".equalsIgnoreCase(keyName))
                continue;
            if (stringIndexInfoHashMap.containsKey(keyName)) {
                IndexInfo i = stringIndexInfoHashMap.get(keyName);
                if (i.getFields() == null)
                    i.setFields(new HashMap<>());
                i.getFields().put(indexInfo.getColumnName(), 0);
            } else {
                if (indexInfo.getFields() == null)
                    indexInfo.setFields(new HashMap<>());
                indexInfo.getFields().put(indexInfo.getColumnName(), 0);
                stringIndexInfoHashMap.put(keyName, indexInfo);
            }
        }
        return stringIndexInfoHashMap.values();
    }

    private void buildModifyIndex(TableInfo table,
                                  Map<TableInfo, List<Object>> addIndexTableMap,
                                  List<Object> addIndexList,
                                  Map<TableInfo, List<Object>> removeIndexTableMap,
                                  List<Object> removeIndexList) {
        List<IndexInfo> indexInfos = table.getIndexInfosCombine();
        List<IndexInfo> indexInfoList = tableDao.getTableIndex(table.getName());
        Collection<IndexInfo> nn = filter(indexInfoList);
        for (IndexInfo indexInfo : indexInfos) {
            boolean has = false;
            for (IndexInfo indexInfo1 : nn) {
                has = indexInfo.equals(indexInfo1);
                if (has)
                    break;
            }
            if (!has) {
                addIndexList.add(indexInfo);
            }
        }
        addIndexTableMap.put(table, addIndexList);

        for (IndexInfo indexInfo : nn) {
            boolean has = false;
            for (IndexInfo indexInfo1 : indexInfos) {
                has = indexInfo.equals(indexInfo1);
                if (has)
                    break;
            }
            if (!has) {
                removeIndexList.add(indexInfo);
            }
        }
        removeIndexTableMap.put(table, removeIndexList);
    }

    private boolean ignoreLength(String typeAndLength) {
        return ignoreLengthList.contains(typeAndLength.toLowerCase());
    }

    /**
     * 根据数据库中表的结构和model中表的结构对比找出修改类型默认值等属性的字段
     *
     * @param mySqlTypeAndLengthMap 获取Mysql的类型，以及类型需要设置几个长度
     * @param modifyTableMap        用于存需要更新字段类型等的表名+结构
     * @param dropKeyTableMap       用于存需要删除主键的表名+结构
     * @param dropUniqueTableMap    用于存需要删除唯一约束的表名+结构
     * @param table                 表
     * @param modifyFieldList       用于存修改的字段
     * @param dropKeyFieldList      用于存删除主键的字段
     * @param dropUniqueFieldList   用于存删除唯一约束的字段
     * @param tableColumnList       已存在时理论上做修改的操作，这里查出该表的结构
     * @param fieldMap              从sysColumns中取出我们需要比较的列的List
     */
    private void buildModifyFields(Map<String, Object> mySqlTypeAndLengthMap,
                                   Map<TableInfo, List<Object>> modifyTableMap,
                                   Map<TableInfo, List<Object>> dropKeyTableMap,
                                   Map<TableInfo, List<Object>> dropUniqueTableMap,
                                   Map<TableInfo, List<Object>> addIndexTableMap,
                                   TableInfo table,
                                   List<Object> modifyFieldList,
                                   List<Object> dropKeyFieldList,
                                   List<Object> dropUniqueFieldList,
                                   List<Object> addIndexFieldList,
                                   List<ColumnMeta> tableColumnList,
                                   Map<String, ColumnInfo> fieldMap) {
        for (ColumnMeta sysColumn : tableColumnList) {
            // 数据库中有该字段时
            ColumnInfo createTableParam = fieldMap.get(sysColumn.getColumnName());
            if (createTableParam != null) {
                // 检查是否要删除已有主键和是否要删除已有唯一约束的代码必须放在其他检查的最前面
                // 原本是主键，现在不是了，那么要去做删除主键的操作
                if ("PRI".equals(sysColumn.getColumnKey()) && !createTableParam.isKey()) {
                    dropKeyFieldList.add(createTableParam);
                }

                // 原本是唯一，现在不是了，那么要去做删除唯一的操作
                if ("UNI".equals(sysColumn.getColumnKey()) && !createTableParam.hasUniqueKey()) {
//                    dropUniqueFieldList.add(createTableParam);
                }

                // 验证是否有更新
                // 1.验证类型
                if (!sysColumn.getDataType().toLowerCase().equals(createTableParam.getType().toLowerCase())) {
                    modifyFieldList.add(createTableParam);
                    continue;
                }
                // 2.验证长度
                // 3.验证小数点位数integer
                int length = (Integer) mySqlTypeAndLengthMap.get(createTableParam.getType().toLowerCase());
                String typeAndLength = createTableParam.getType().toLowerCase();
                boolean ignoreLenth = ignoreLength(typeAndLength);
                if (!ignoreLenth) {
                    if (length == 1) {
                        // 拼接出类型加长度，比如varchar(1)
                        typeAndLength = typeAndLength + "(" + createTableParam.getLength() + ")";
                    } else if (length == 2) {
                        typeAndLength = typeAndLength + "(" + createTableParam.getLength() + ","
                                + createTableParam.getDecimalLength() + ")";
                    }
                    // 判断类型+长度是否相同
                    if (!sysColumn.getColumnType().toLowerCase().equals(typeAndLength)) {
                        modifyFieldList.add(createTableParam);
                        continue;
                    }
                } else {
                    if (!sysColumn.getColumnType().toLowerCase().contains(typeAndLength)) {
                        modifyFieldList.add(createTableParam);
                        continue;
                    }
                }

                // 4.验证主键
                if (!"PRI".equals(sysColumn.getColumnKey()) && createTableParam.isKey()) {
                    // 原本不是主键，现在变成了主键，那么要去做更新
                    modifyFieldList.add(createTableParam);
                    continue;
                }

                // 5.验证自增
                if ("auto_increment".equals(sysColumn.getExtra()) && !createTableParam.isAutoIncrement()) {
                    modifyFieldList.add(createTableParam);
                    continue;
                }

                // 6.验证默认值
                if (sysColumn.getColumnDefault() == null || sysColumn.getColumnDefault().equals("")) {
                    // 数据库默认值是null，model中注解设置的默认值不为NULL时，那么需要更新该字段
                    if(!Objects.equals(sysColumn.getColumnDefault(),createTableParam.getDefaultValue())) {
                        if (!"NULL".equals(createTableParam.getDefaultValue())) {
                            modifyFieldList.add(createTableParam);
                            continue;
                        }
                    }
                } else if (!sysColumn.getColumnDefault().equals(createTableParam.getDefaultValue())) {
                    if (createTableParam.getType().equals(DataType.FLOAT) || createTableParam.getType().equals(DataType.DECIMAL) || createTableParam.getType().equals(DataType.DOUBLE)) {
                        try {
                            double d = Double.parseDouble(sysColumn.getColumnDefault());
                            double c = Double.parseDouble(createTableParam.getDefaultValue());
                            if (d != c) {
                                modifyFieldList.add(createTableParam);
                            }
                        } catch (Exception ignored) {
                        }
                    } else
                        // 两者不相等时，需要更新该字段
                        modifyFieldList.add(createTableParam);
                    continue;
                }

                // 7.验证是否可以为null(主键不参与是否为null的更新)
                if (!sysColumn.getNullable() && !createTableParam.isKey()) {
                    if (createTableParam.isNull()) {
                        // 一个是可以一个是不可用，所以需要更新该字段
                        modifyFieldList.add(createTableParam);
                        continue;
                    }
                } else if (sysColumn.getNullable() && !createTableParam.isKey()) {
                    if (!createTableParam.isNull()) {
                        // 一个是可以一个是不可用，所以需要更新该字段
                        modifyFieldList.add(createTableParam);
                        continue;
                    }
                }

                // 8.验证是否唯一
                if (!"UNI".equals(sysColumn.getColumnKey()) && createTableParam.hasUniqueKey()) {
                    // 原本不是唯一，现在变成了唯一，那么要去做更新
//                    modifyFieldList.add(createTableParam);
                    continue;
                }

            }
        }

        if (modifyFieldList.size() > 0) {
            modifyTableMap.put(table, modifyFieldList);
        }

        if (dropKeyFieldList.size() > 0) {
            dropKeyTableMap.put(table, dropKeyFieldList);
        }

        if (dropUniqueFieldList.size() > 0) {
            dropUniqueTableMap.put(table, dropUniqueFieldList);
        }

    }

    /**
     * 根据数据库中表的结构和model中表的结构对比找出删除的字段
     *
     * @param removeTableMap  用于存需要删除字段的表名+结构
     * @param table           表
     * @param removeFieldList 用于存删除的字段
     * @param columnNames     数据库中的结构
     * @param fieldMap        model中的字段，字段名为key
     */
    private void buildRemoveFields(Map<TableInfo, List<Object>> removeTableMap,
                                   TableInfo table, List<Object> removeFieldList,
                                   List<String> columnNames,
                                   Map<String, ColumnInfo> fieldMap) {
        for (String fieldNm : columnNames) {
            // 判断该字段在新的model结构中是否存在

            if (!containIgnoreCase(fieldMap.keySet(), fieldNm)) {
                removeFieldList.add(fieldNm);
            }
        }
        if (removeFieldList.size() > 0) {
            removeTableMap.put(table, removeFieldList);
        }
    }


    /**
     * 根据数据库中表的结构和model中表的结构对比找出新增的字段
     *
     * @param addTableMap  用于存需要增加字段的表名+结构
     * @param table        表
     * @param newFieldList model中的结构
     * @param addFieldList 用于存新增的字段
     * @param columnNames  数据库中的结构
     */
    private void buildNewFields(Map<TableInfo, List<Object>> addTableMap,
                                TableInfo table, List<Object> newFieldList,
                                List<Object> addFieldList,
                                List<String> columnNames) {
        for (Object obj : newFieldList) {
            ColumnInfo createTableParam = (ColumnInfo) obj;
            // 循环新的model中的字段，判断是否在数据库中已经存在

            if (!containIgnoreCase(columnNames, createTableParam.getName())) {
                // 不存在，表示要在数据库中增加该字段
                addFieldList.add(obj);
            }
        }
        if (addFieldList.size() > 0) {
            addTableMap.put(table, addFieldList);
        }
    }

    private boolean containIgnoreCase(List<String> columnNames, String column) {
        for (String s : columnNames) {
            if (s.equalsIgnoreCase(column))
                return true;
        }
        return false;
    }

    private boolean containIgnoreCase(Set<String> columnNames, String column) {
        for (String s : columnNames) {
            if (s.equalsIgnoreCase(column))
                return true;
        }
        return false;
    }

    /**
     * 迭代出所有model的所有fields存到newFieldList中
     *
     * @param mySqlTypeAndLengthMap mysql数据类型和对应几个长度的map
     * @param clas                  准备做为创建表依据的class
     * @param newFieldList          用于存新增表的字段
     */
    private void tableFieldsConstruct(Map<String, Object> mySqlTypeAndLengthMap,
                                      Class<?> clas,
                                      List<Object> newFieldList) {
        Field[] fields = clas.getDeclaredFields();

        // 判断是否有父类，如果有拉取父类的field，这里只支持多层继承
        fields = recursionParents(clas, fields);

        UniqueKeys uniqueKeys = clas.getAnnotation(UniqueKeys.class);
        UniqueKey uniqueKey = clas.getAnnotation(UniqueKey.class);

        for (Field field : fields) {
            // 判断方法中是否有指定注解类型的注解
            boolean hasAnnotation = field.isAnnotationPresent(Column.class);
            if (hasAnnotation) {
                // 根据注解类型返回方法的指定类型注解
                Column column = field.getAnnotation(Column.class);
                ColumnInfo columnInfo = new ColumnInfo(field, uniqueKeys, uniqueKey);
                int length = 0;
                try {
                    length = (Integer) mySqlTypeAndLengthMap.get(columnInfo.getType());
                } catch (Exception e) {
                    log.error("未知的Mysql数据类型字段:" + column.type());
                }
                columnInfo.setTypeLength(length);
                newFieldList.add(columnInfo);
            }
        }
    }

    /**
     * 递归扫描父类的fields
     *
     * @param clas   类
     * @param fields 属性
     */
    @SuppressWarnings("rawtypes")
    private Field[] recursionParents(Class<?> clas, Field[] fields) {
        if (clas.getSuperclass() != null) {
            Class clsSup = clas.getSuperclass();
            fields = (Field[]) ArrayUtils.addAll(fields, clsSup.getDeclaredFields());
            fields = recursionParents(clsSup, fields);
        }
        return fields;
    }

    /**
     * 根据传入的map创建或修改表结构
     *
     * @param newTableMap        用于存需要创建的表名+结构
     * @param modifyTableMap     用于存需要更新字段类型等的表名+结构
     * @param addTableMap        用于存需要增加字段的表名+结构
     * @param removeTableMap     用于存需要删除字段的表名+结构
     * @param dropKeyTableMap    用于存需要删除主键的表名+结构
     * @param dropUniqueTableMap 用于存需要删除唯一约束的表名+结构
     */
    private void createOrModifyTableConstruct(Map<TableInfo, List<Object>> newTableMap,
                                              Map<TableInfo, List<Object>> modifyTableMap,
                                              Map<TableInfo, List<Object>> addTableMap,
                                              Map<TableInfo, List<Object>> removeTableMap,
                                              Map<TableInfo, List<Object>> dropKeyTableMap,
                                              Map<TableInfo, List<Object>> dropUniqueTableMap,
                                              Map<TableInfo, List<Object>> addIndexTableMap,
                                              Map<TableInfo, List<Object>> removeIndexTableMap) {
        // 1. 创建表
        createTableByMap(newTableMap);
        // 2. 删除要变更主键的表的原来的字段的主键
        dropFieldsKeyByMap(dropKeyTableMap);
        // 3. 删除要变更唯一约束的表的原来的字段的唯一约束
        dropFieldsUniqueByMap(dropUniqueTableMap);
        // 4. 添加新的字段
        addFieldsByMap(addTableMap);
        // 5. 删除字段
        removeFieldsByMap(removeTableMap);
        // 6. 修改字段类型等
        modifyFieldsByMap(modifyTableMap);
        removeIndexByMap(removeIndexTableMap);
        addIndexByMap(addIndexTableMap);
    }

    /**
     * 根据map结构修改表中的字段类型等
     *
     * @param modifyTableMap 用于存需要更新字段类型等的表名+结构
     */
    private void modifyFieldsByMap(Map<TableInfo, List<Object>> modifyTableMap) {
        // 做修改字段操作
        if (modifyTableMap.size() > 0) {
            for (Entry<TableInfo, List<Object>> entry : modifyTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<TableInfo, Object>();
                    map.put(entry.getKey(), obj);
                    try {
                        tableDao.modifyColumn(map);
                    } catch (Throwable e) {
                        log.debug("Modify Columns:{}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 根据map结构删除表中的字段
     *
     * @param removeTableMap 用于存需要删除字段的表名+结构
     */
    private void removeFieldsByMap(Map<TableInfo, List<Object>> removeTableMap) {
        // 做删除字段操作
        if (removeTableMap.size() > 0) {
            for (Entry<TableInfo, List<Object>> entry : removeTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<TableInfo, Object>();
                    map.put(entry.getKey(), obj);
                    try {
                        tableDao.dropColumn(map);
                    } catch (Throwable e) {
                        log.debug("Drop Columns:{}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 根据map结构对表中添加新的字段
     *
     * @param addTableMap 用于存需要增加字段的表名+结构
     */
    private void addFieldsByMap(Map<TableInfo, List<Object>> addTableMap) {
        if (addTableMap.size() > 0) {
            for (Entry<TableInfo, List<Object>> entry : addTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<TableInfo, Object>();
                    map.put(entry.getKey(), obj);
                    try {
                        tableDao.addColumns(map);
                    } catch (Throwable e) {
                        log.debug("Add Columns:{}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 根据map结构删除要变更表中字段的主键
     *
     * @param dropKeyTableMap 用于存需要删除主键的表名+结构
     */
    private void dropFieldsKeyByMap(Map<TableInfo, List<Object>> dropKeyTableMap) {
        // 先去做删除主键的操作，这步操作必须在增加和修改字段之前！
        if (dropKeyTableMap.size() > 0) {
            for (Entry<TableInfo, List<Object>> entry : dropKeyTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<TableInfo, Object>();
                    map.put(entry.getKey(), obj);
                    try {
                        tableDao.dropPrimaryKey(map);
                    } catch (Throwable e) {
                        log.debug("Drop Primary Key:{}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 根据map结构删除要变更表中字段的唯一约束
     *
     * @param dropUniqueTableMap 用于存需要删除唯一约束的表名+结构
     */
    private void dropFieldsUniqueByMap(Map<TableInfo, List<Object>> dropUniqueTableMap) {
        // 先去做删除唯一约束的操作，这步操作必须在增加和修改字段之前！
        if (dropUniqueTableMap.size() > 0) {
            for (Entry<TableInfo, List<Object>> entry : dropUniqueTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<TableInfo, Object>();
                    map.put(entry.getKey(), obj);
                    try {
                        tableDao.dropIndex(map);
                    } catch (Throwable e) {
                        log.debug("Drop Index:{}", e.getMessage());
                    }
                    try {
                        tableDao.modifyColumn(map);
                    } catch (Throwable e) {
                        log.debug("Modify Column:{}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 根据map结构增加索引
     *
     * @param addIndexTableMap 用于存需要删除唯一约束的表名+结构
     */
    private void addIndexByMap(Map<TableInfo, List<Object>> addIndexTableMap) {
        // 先去做删除唯一约束的操作，这步操作必须在增加和修改字段之前！
        if (addIndexTableMap.size() > 0) {
            for (Entry<TableInfo, List<Object>> entry : addIndexTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<TableInfo, Object>();
                    map.put(entry.getKey(), obj);
                    try {
                        tableDao.addIndex(map);
                    } catch (Throwable e) {
                        log.debug("Add Index:{}", e.getMessage());
                    }
                }
            }
        }
    }

    private void removeIndexByMap(Map<TableInfo, List<Object>> addIndexTableMap) {
        // 先去做删除唯一约束的操作，这步操作必须在增加和修改字段之前！
        if (addIndexTableMap.size() > 0) {
            for (Entry<TableInfo, List<Object>> entry : addIndexTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<TableInfo, Object>();
                    map.put(entry.getKey(), obj);
                    try {
                        tableDao.dropIndex(map);
                    } catch (Throwable e) {
                        log.debug("Drop Index:{}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 根据map结构创建表
     *
     * @param newTableMap 用于存需要创建的表名+结构
     */
    private void createTableByMap(Map<TableInfo, List<Object>> newTableMap) {
        if (newTableMap.size() > 0) {
            for (Entry<TableInfo, List<Object>> entry : newTableMap.entrySet()) {
                Map<TableInfo, List<Object>> map = new HashMap<TableInfo, List<Object>>();
                map.put(entry.getKey(), entry.getValue());
                try {
                    tableDao.createTable(map);
                } catch (Throwable e) {
                    log.debug("Create Table:{}", e.getMessage());
                }
            }
        }
    }

    /**
     * 获取Mysql的类型，以及类型需要设置几个长度，这里构建成map的样式
     * 构建Map(字段名(小写),需要设置几个长度(0表示不需要设置，1表示需要设置一个，2表示需要设置两个))
     */
    public Map<String, Object> mySqlTypeAndLengthMap() {
        Field[] fields = DataType.class.getDeclaredFields();
        Map<String, Object> map = new HashMap<String, Object>();
        for (Field field : fields) {
            LengthCount lengthCount = field.getAnnotation(LengthCount.class);
            if (null != lengthCount)
                map.put(field.getName().toLowerCase(), lengthCount.LengthCount());
        }
        return map;
    }

    public void dropTable(String tableName) {
        try {
            tableDao.dropTable(tableName);
        } catch (Throwable e) {
            log.debug("Drop Table:{}", e.getMessage());
        }
    }
}
