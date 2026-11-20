package cn.org.autumn.table.service;

import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArraySet;

import cn.org.autumn.table.platform.RelationalTableOperations;
import cn.org.autumn.table.data.*;
import cn.org.autumn.table.relational.model.ColumnMeta;
import cn.org.autumn.table.relational.model.SchemaSyncMetadata;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import cn.org.autumn.install.InstallMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.org.autumn.table.annotation.Column;
import cn.org.autumn.table.annotation.LengthCount;
import cn.org.autumn.table.utils.ClassTools;
import cn.org.autumn.table.utils.TableCharsetUtils;

import static cn.org.autumn.table.data.InitType.*;
import cn.org.autumn.table.annotation.IndexOrder;
import cn.org.autumn.database.runtime.query.QueryIndexContributor;
import cn.org.autumn.database.runtime.query.QueryIndexDiscovery;
import cn.org.autumn.database.runtime.query.QueryIndexMerger;
import cn.org.autumn.table.data.index.IndexDialectCapability;
import cn.org.autumn.table.data.index.IndexDiffEngine;
import cn.org.autumn.table.data.index.IndexLimitGuard;
import cn.org.autumn.table.data.index.IndexRebuildTask;
import cn.org.autumn.table.data.index.IndexTableSettings;
import jakarta.annotation.PostConstruct;

@Slf4j
@Transactional
@Service
public class MysqlTableService {

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
    private RelationalTableOperations tableDao;

    @Autowired(required = false)
    private Environment environment;

    /**
     * 要扫描的model所在的pack
     */
    @Getter
    @Value("${autumn.table.pack:" + defaultPackage + "}")
    private String pack;

    /**
     * 自动创建模式：update表示更新，create表示删除原表重新创建，none 表示不执行操作
     */
    @Value("${autumn.table.auto:update}")
    private InitType type;

    /**
     * 是否在 update 模式下根据 @Table 同步表字符集（不匹配则 CONVERT TO）。大表可能耗时锁表，可设为 false 后改用手动迁移。
     */
    @Value("${autumn.table.sync-charset:true}")
    private boolean syncTableCharset;

    /**
     * 索引列序/方向不一致时是否 DROP+ADD 重建。默认 true；false 时退回仅列集合比对（兼容逃生舱）。
     */
    @Value("${autumn.table.index.rebuild-on-order-mismatch:true}")
    private boolean rebuildIndexOnOrderMismatch;

    @Value("${autumn.table.index.max-columns-per-index:16}")
    private int indexMaxColumnsPerIndex;

    @Value("${autumn.table.index.max-per-table:64}")
    private int indexMaxPerTable;

    @Value("${autumn.table.index.max-key-bytes:3072}")
    private int indexMaxKeyBytes;

    @Value("${autumn.table.index.skip-on-limit-violation:false}")
    private boolean indexSkipOnLimitViolation;

    @Value("${autumn.table.index.warn-unique-blank-default:true}")
    private boolean indexWarnUniqueBlankDefault;

    /** 0=跟随方言名长。 */
    @Value("${autumn.table.index.max-name-length:0}")
    private int indexMaxNameLength;

    /** 可选：Spring 管理的 QueryIndexContributor（多数 *DaoSql 非 Bean，仍以包扫描为主）。 */
    @Autowired(required = false)
    private List<QueryIndexContributor> queryIndexContributors;

    /** 单次 createMysqlTable 内的批量元数据快照；无则回退逐表查询。 */
    private final ThreadLocal<SchemaSyncMetadata> schemaSyncMeta = new ThreadLocal<>();

    @PostConstruct
    public void applyIndexTableSettings() {
        System.setProperty(IndexTableSettings.PROP_REBUILD_ON_ORDER_MISMATCH,
                Boolean.toString(rebuildIndexOnOrderMismatch));
        System.setProperty(IndexTableSettings.PROP_MAX_COLUMNS_PER_INDEX,
                Integer.toString(indexMaxColumnsPerIndex));
        System.setProperty(IndexTableSettings.PROP_MAX_PER_TABLE,
                Integer.toString(indexMaxPerTable));
        System.setProperty(IndexTableSettings.PROP_MAX_KEY_BYTES,
                Integer.toString(indexMaxKeyBytes));
        System.setProperty(IndexTableSettings.PROP_SKIP_ON_LIMIT_VIOLATION,
                Boolean.toString(indexSkipOnLimitViolation));
        System.setProperty(IndexTableSettings.PROP_WARN_UNIQUE_BLANK_DEFAULT,
                Boolean.toString(indexWarnUniqueBlankDefault));
        System.setProperty(IndexTableSettings.PROP_MAX_NAME_LENGTH,
                Integer.toString(indexMaxNameLength));
    }

    /** 实体扫描结果缓存（pack 启动后不变；安装 JVM 重启会重建本 Bean）。 */
    private volatile Set<Class<?>> cachedEntityClasses;

    public Set<String> getPacks() {
        return new CopyOnWriteArraySet<>(Arrays.asList(pack.split(",|:|;|-| ")));
    }

    /** Query→Index 启动发现（与 @Indexes 并列，无单独开关；无 Contributor 时几乎空转）。 */
    private void discoverQueryIndexes() {
        QueryIndexDiscovery.discover(getPacks(), queryIndexContributors);
    }

    public Set<Class<?>> getClasses() {
        Set<Class<?>> local = cachedEntityClasses;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (cachedEntityClasses != null) {
                return cachedEntityClasses;
            }
            Set<Class<?>> classes = new LinkedHashSet<>();
            Set<String> packs = getPacks();
            //添加系统代码的默认包名，防止配置错误
            packs.add(defaultPackage);
            for (String pkg : packs) {
                if (!pkg.isEmpty()) {
                    classes.addAll(ClassTools.getClasses(pkg));
                }
            }
            cachedEntityClasses = Collections.unmodifiableSet(classes);
            return cachedEntityClasses;
        }
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

        if (InstallMode.isActive(environment)) {
            log.info("安装向导模式，跳过注解建表。");
            return;
        }

        discoverQueryIndexes();

        long t0 = System.currentTimeMillis();
        SchemaSyncMetadata preload = tableDao.loadSchemaSyncMetadata();
        schemaSyncMeta.set(preload);
        long t1 = System.currentTimeMillis();
        if (preload != null) {
            log.debug("Schema sync preload: tables={} columns={} indexRows={} elapsedMs={}", preload.tableCount(), preload.columnCount(), preload.indexRowCount(), t1 - t0);
        } else {
            log.debug("Schema sync preload: unavailable, per-table meta queries, elapsedMs={}", t1 - t0);
        }

        try {
            // 获取Mysql的类型，以及类型需要设置几个长度
            Map<String, Object> mySqlTypeAndLengthMap = mySqlTypeAndLengthMap();

            // 用于存需要创建的表名+结构
            Map<TableInfo, List<Object>> newTableMap = new HashMap<>();

            // 用于存需要更新字段类型等的表名+结构
            Map<TableInfo, List<Object>> modifyTableMap = new HashMap<>();

            // 用于存需要增加字段的表名+结构
            Map<TableInfo, List<Object>> addTableMap = new HashMap<>();

            // 用于存需要删除字段的表名+结构
            Map<TableInfo, List<Object>> removeTableMap = new HashMap<>();

            // 用于存需要删除主键的表名+结构
            Map<TableInfo, List<Object>> dropKeyTableMap = new HashMap<>();

            // 用于存需要删除唯一约束的表名+结构
            Map<TableInfo, List<Object>> dropUniqueTableMap = new HashMap<>();

            // 用于存需要删除唯一约束的表名+结构
            Map<TableInfo, List<Object>> addIndexTableMap = new HashMap<>();
            Map<TableInfo, List<Object>> removeIndexTableMap = new HashMap<>();
            // 同名索引重建（列序/定义）：逐条 DROP+ADD，与纯新增/删除分离
            List<IndexRebuildTask> rebuildIndexTasks = new ArrayList<>();
            // 待 CONVERT TO 的表：须在删索引之后执行，避免 utf8→utf8mb4 时超长索引（3072）导致失败
            List<TableInfo> charsetSyncTables = new ArrayList<TableInfo>();
            // 构建出全部表的增删改的map
            allTableMapConstruct(type, mySqlTypeAndLengthMap, classes, newTableMap, modifyTableMap, addTableMap, removeTableMap, dropKeyTableMap, dropUniqueTableMap, addIndexTableMap, removeIndexTableMap, rebuildIndexTasks, charsetSyncTables);
            long t2 = System.currentTimeMillis();

            // 根据传入的map，分别去创建或修改表结构（先删多余索引，再改列，再 CONVERT）
            createOrModifyTableConstruct(newTableMap, modifyTableMap, addTableMap, removeTableMap, dropKeyTableMap, dropUniqueTableMap, addIndexTableMap, removeIndexTableMap, rebuildIndexTasks, charsetSyncTables);
            long t3 = System.currentTimeMillis();
            log.debug("Schema sync done: entities={} newTables={} rebuildIndexes={} diffMs={} ddlMs={} totalMs={}", classes.size(), newTableMap.size(), rebuildIndexTasks.size(), t2 - t1, t3 - t2, t3 - t0);
        } finally {
            schemaSyncMeta.remove();
        }
    }

    private boolean tableExists(String tableName) {
        SchemaSyncMetadata meta = schemaSyncMeta.get();
        if (meta != null) {
            return meta.hasTable(tableName);
        }
        return tableDao.hasTable(tableName);
    }

    private List<ColumnMeta> columnMetas(String tableName) {
        SchemaSyncMetadata meta = schemaSyncMeta.get();
        if (meta != null) {
            return meta.getColumns(tableName);
        }
        return tableDao.getColumnMetas(tableName);
    }

    private List<IndexInfo> indexRows(String tableName) {
        SchemaSyncMetadata meta = schemaSyncMeta.get();
        if (meta != null) {
            return meta.getIndexRows(tableName);
        }
        return tableDao.getTableIndex(tableName);
    }

    private String tableCharacterSetName(String tableName) {
        SchemaSyncMetadata meta = schemaSyncMeta.get();
        if (meta != null) {
            return meta.getCharacterSetName(tableName);
        }
        return tableDao.getTableCharacterSetName(tableName);
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
                                      Map<TableInfo, List<Object>> removeIndexTableMap,
                                      List<IndexRebuildTask> rebuildIndexTasks,
                                      List<TableInfo> charsetSyncTables) {
        for (Class<?> clazz : classes) {
            try {
                TableInfo tableInfo = new TableInfo(clazz);

                if (!tableInfo.isValid())
                    continue;

                // Query→Index：Registry 无同表项时 applyTo 立即返回，不影响纯 @Indexes 表
                QueryIndexMerger.applyTo(tableInfo, clazz);

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
                    SchemaSyncMetadata meta = schemaSyncMeta.get();
                    if (meta != null) {
                        meta.removeTable(tableInfo.getName());
                    }
                }

                boolean exist = tableExists(tableInfo.getName());

                // 不存在时
                if (!exist) {
                    newTableMap.put(tableInfo, newFieldList);
                } else {
                    if (syncTableCharset) {
                        // 延后到删索引之后再 CONVERT，见 createOrModifyTableConstruct
                        charsetSyncTables.add(tableInfo);
                    }
                    List<ColumnMeta> tableColumnList = columnMetas(tableInfo.getName());
                    warnOnLargeInformationSchemaLengths(tableInfo.getName(), tableColumnList);
                    List<String> columnNames = ClassTools.getPropertyValueList(tableColumnList,
                            ColumnMeta.COLUMN_NAME_KEY);
                    // 验证对比从model中解析的fieldList与从数据库查出来的columnList
                    // 1. 找出增加的字段
                    // 2. 找出删除的字段
                    // 3. 找出更新的字段
                    buildAddAndRemoveAndModifyFields(mySqlTypeAndLengthMap, modifyTableMap, addTableMap, removeTableMap,
                            dropKeyTableMap, dropUniqueTableMap, addIndexTableMap, removeIndexTableMap, rebuildIndexTasks,
                            tableInfo, newFieldList, removeFieldList, addFieldList,
                            modifyFieldList, dropKeyFieldList, dropUniqueFieldList, addIndexList, removeIndexList, tableColumnList, columnNames);
                }
            } catch (Throwable e) {
                // 单实体元数据/对照失败不得中断空库全量建表（否则 sys_config 等核心表也不会创建）
                log.warn("建表跳过: [{}]: {}", clazz.getName(), e.getMessage());
            }
        }
    }

    /**
     * 若库表字符集与 @Table.charset 不一致，则执行 {@code ALTER TABLE ... CONVERT TO}。
     * <p>须在实体多余索引已删除之后调用，否则 utf8→utf8mb4 时超长索引会报 max key length 3072。
     * <p>名称经校验后拼接，非法则跳过；utf8 与 utf8mb3 视为一致不升级。
     */
    private void syncTableCharsetIfNeeded(TableInfo tableInfo) {
        if (tableInfo == null || StringUtils.isBlank(tableInfo.getName())) {
            return;
        }
        String raw = tableInfo.getCharset();
        if (StringUtils.isBlank(raw)) {
            return;
        }
        String desired = raw.trim();
        if (!TableCharsetUtils.isSafeSqlCharsetOrCollationName(desired)) {
            log.warn("实体表 [{}] @Table.charset 非法，已跳过字符集同步: {}", tableInfo.getName(), raw);
            return;
        }
        String coll = tableInfo.getCollation();
        String collationArg = null;
        if (StringUtils.isNotBlank(coll)) {
            String c = coll.trim();
            if (!TableCharsetUtils.isSafeSqlCharsetOrCollationName(c)) {
                log.warn("实体表 [{}] @Table.collation 非法，已跳过字符集同步: {}", tableInfo.getName(), coll);
                return;
            }
            collationArg = c;
        }
        try {
            String dbCharset = tableCharacterSetName(tableInfo.getName());
            if (StringUtils.isBlank(dbCharset)) {
                log.debug("无法解析表 [{}] 的字符集，跳过字符集同步", tableInfo.getName());
                return;
            }
            if (TableCharsetUtils.sameCharset(desired, dbCharset)) {
                return;
            }
            log.info("表 [{}] 字符集与实体不一致（库: {}，实体: {}），执行 CONVERT TO", tableInfo.getName(), dbCharset, desired);
            tableDao.convertTableCharset(tableInfo.getName(), desired, collationArg);
        } catch (Throwable e) {
            log.warn("表 [{}] 字符集同步失败: {}", tableInfo.getName(), e.getMessage());
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
                                                  List<IndexRebuildTask> rebuildIndexTasks,
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
        buildModifyIndex(tableInfo, addIndexTableMap, addIndexList, removeIndexTableMap, removeIndexList, rebuildIndexTasks);
    }

    /** 合并 SHOW INDEX 多行列，resolve 类型/方法后供 diff。 */
    private Collection<IndexInfo> filter(List<IndexInfo> indexInfoList) {
        Map<String, IndexInfo> byName = new LinkedHashMap<>();
        for (IndexInfo row : indexInfoList) {
            String keyName = StringUtils.isNotBlank(row.getKeyName()) ? row.getKeyName() : row.getName();
            if (StringUtils.isBlank(keyName) || "PRIMARY".equalsIgnoreCase(keyName)) {
                continue;
            }
            IndexOrder order =
                    IndexOrder.fromCollation(row.getCollation());
            int prefix = row.getSubPartInt();
            String col = row.getColumnName();
            if (byName.containsKey(keyName)) {
                IndexInfo merged = byName.get(keyName);
                merged.appendColumn(col, prefix, order);
                if (IndexInfo.isUniqueNonUniqueFlag(row.getNonUnique())) {
                    merged.setNonUnique("0");
                    merged.resolve();
                }
            } else {
                IndexInfo fresh = new IndexInfo();
                fresh.setKeyName(keyName);
                fresh.setName(keyName);
                fresh.setIndexType(row.getIndexType());
                fresh.setIndexMethod(row.getIndexMethod());
                fresh.setNonUnique(row.getNonUnique());
                fresh.setComment(row.getComment());
                fresh.setTable(row.getTable());
                fresh.appendColumn(col, prefix, order);
                fresh.resolve();
                byName.put(keyName, fresh);
            }
        }
        return byName.values();
    }

    private void buildModifyIndex(TableInfo table,
                                  Map<TableInfo, List<Object>> addIndexTableMap,
                                  List<Object> addIndexList,
                                  Map<TableInfo, List<Object>> removeIndexTableMap,
                                  List<Object> removeIndexList,
                                  List<IndexRebuildTask> rebuildIndexTasks) {
        List<IndexInfo> indexInfos = table.getIndexInfosCombine();
        Collection<IndexInfo> dbIndexes = filter(indexRows(table.getName()));
        List<IndexInfo> remainingDb = new ArrayList<>(dbIndexes);
        Set<IndexInfo> pairedDb = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IndexInfo entityIdx : indexInfos) {
            if (entityIdx != null && entityIdx.isSkipEmit()) {
                // 收集阶段已 WARN，同步阶段不再重复刷屏
                log.debug("Index sync skipped by limit guard: table={} index={}",
                        table.getName(), entityIdx.getName());
                continue;
            }
            IndexDiffEngine.PairOutcome outcome =
                    IndexDiffEngine.pair(table.getName(), entityIdx, remainingDb);
            if (outcome.kind == IndexDiffEngine.PairOutcome.Kind.AMBIGUOUS
                    || outcome.kind == IndexDiffEngine.PairOutcome.Kind.UNPAIRED) {
                addIndexList.add(entityIdx);
                continue;
            }
            IndexInfo dbIdx = outcome.dbIndex;
            IndexDiffEngine.removePaired(remainingDb, dbIdx);
            pairedDb.add(dbIdx);
            IndexRebuildTask.Reason reason =
                    IndexDiffEngine.classifyAfterPair(
                            table.getName(), entityIdx, dbIdx);
            if (reason == null) {
                continue;
            }
            // NAME → 优先 RENAME；ORDER/DEFINITION → 成对 DROP+ADD
            if (rebuildIndexTasks != null) {
                rebuildIndexTasks.add(new IndexRebuildTask(
                        table, dbIdx, entityIdx, reason));
            } else {
                removeIndexList.add(dbIdx);
                addIndexList.add(entityIdx);
            }
        }
        addIndexTableMap.put(table, addIndexList);

        for (IndexInfo dbIdx : dbIndexes) {
            if (pairedDb.contains(dbIdx)) {
                continue;
            }
            if (IndexDiffEngine.nameMatchesAny(table.getName(), dbIdx, indexInfos)) {
                continue;
            }
            removeIndexList.add(dbIdx);
        }
        removeIndexTableMap.put(table, removeIndexList);
    }

    private boolean ignoreLength(String typeAndLength) {
        return ignoreLengthList.contains(typeAndLength.toLowerCase());
    }

    /**
     * 字符串相关列：字符集或与表一致的排序规则是否与库中不一致（含列继承表 collation 的情形）。
     */
    private boolean needsCharsetCollationModify(ColumnInfo col, ColumnMeta sys, TableInfo tableInfo) {
        if (!col.supportsSqlCharsetClause()) {
            return false;
        }
        String dbCs = sys.getCharacterSetName();
        String dbCo = sys.getCollationName();
        boolean hasExCs = StringUtils.isNotBlank(col.getExplicitCharset());
        boolean hasExCo = StringUtils.isNotBlank(col.getExplicitCollation());
        String desCs = hasExCs ? col.getExplicitCharset() : tableInfo.getCharset();
        if (!TableCharsetUtils.sameCharset(desCs, dbCs)) {
            return true;
        }
        if (hasExCo) {
            return !TableCharsetUtils.sameCollation(col.getExplicitCollation(), dbCo);
        }
        if (hasExCs) {
            return false;
        }
        if (StringUtils.isNotBlank(tableInfo.getCollation())) {
            return !TableCharsetUtils.sameCollation(tableInfo.getCollation(), dbCo);
        }
        return false;
    }

    /**
     * 监控 information_schema 中的超大长度值（如 LONGTEXT 的 4294967295），
     * 便于排查 JDBC/驱动在旧版本整数映射上的溢出风险。
     */
    private void warnOnLargeInformationSchemaLengths(String tableName, List<ColumnMeta> tableColumnList) {
        if (tableColumnList == null || tableColumnList.isEmpty()) {
            return;
        }
        long maxInt = Integer.MAX_VALUE;
        for (ColumnMeta c : tableColumnList) {
            if (c == null) {
                continue;
            }
            Long maxLen = c.getCharacterMaximumLength();
            Long octLen = c.getCharacterOctetLength();
            Long numPrecision = c.getNumericPrecision();
            if ((maxLen != null && maxLen > maxInt)
                    || (octLen != null && octLen > maxInt)
                    || (numPrecision != null && numPrecision > maxInt)) {
                if (log.isDebugEnabled())
                    log.debug("表 [{}] 字段 [{}] 元数据存在超大长度/精度值: characterMaximumLength={}, characterOctetLength={}, numericPrecision={}, dataType={}, columnType={}; "
                                    + "已使用 Long 映射兼容，避免 Integer 溢出。",
                            tableName,
                            c.getColumnName(),
                            maxLen,
                            octLen,
                            numPrecision,
                            c.getDataType(),
                            c.getColumnType());
            }
        }
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
            // 数据库中有该字段时（JDBC/Oracle 等可能返回大写列名，与实体 @Column 蛇形小写对齐）
            ColumnInfo createTableParam = columnInfoByNameIgnoreCase(fieldMap, sysColumn.getColumnName());
            if (createTableParam != null) {
                // 检查是否要删除已有主键和是否要删除已有唯一约束的代码必须放在其他检查的最前面
                // 原本是主键，现在不是了，那么要去做删除主键的操作
                if ("PRI".equals(sysColumn.getColumnKey())) {
                    createTableParam.setExistingPrimaryKey(true);
                    if (!createTableParam.isKey()) {
                        dropKeyFieldList.add(createTableParam);
                    }
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

                if (needsCharsetCollationModify(createTableParam, sysColumn, table)) {
                    modifyFieldList.add(createTableParam);
                    continue;
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

                // 枚举字段数据库默认值自愈：非法值(含空字符串)强制改为NULL
                if (isInvalidEnumDefault(createTableParam, sysColumn.getColumnDefault())) {
                    createTableParam.setDefaultValue("NULL");
                    modifyFieldList.add(createTableParam);
                    continue;
                }

                // 6.验证默认值
                if (sysColumn.getColumnDefault() == null || sysColumn.getColumnDefault().equals("")) {
                    // 数据库默认值是null，model中注解设置的默认值不为NULL时，那么需要更新该字段
                    if (!Objects.equals(sysColumn.getColumnDefault(), createTableParam.getDefaultValue())) {
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

        if (!modifyFieldList.isEmpty()) {
            modifyTableMap.put(table, modifyFieldList);
        }

        if (!dropKeyFieldList.isEmpty()) {
            dropKeyTableMap.put(table, dropKeyFieldList);
        }

        if (!dropUniqueFieldList.isEmpty()) {
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
        if (!removeFieldList.isEmpty()) {
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
        if (!addFieldList.isEmpty()) {
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

    private ColumnInfo columnInfoByNameIgnoreCase(Map<String, ColumnInfo> fieldMap, String columnName) {
        if (columnName == null || fieldMap == null) {
            return null;
        }
        ColumnInfo direct = fieldMap.get(columnName);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, ColumnInfo> e : fieldMap.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(columnName)) {
                return e.getValue();
            }
        }
        return null;
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
        newFieldList.addAll(collectColumnInfos(clas, mySqlTypeAndLengthMap));
    }

    /**
     * 收集实体类的 @Column 字段信息，用于建表或列表展示
     */
    private List<ColumnInfo> collectColumnInfos(Class<?> clazz, Map<String, Object> mySqlTypeAndLengthMap) {
        List<ColumnInfo> list = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        fields = recursionParents(clazz, fields);
        for (Field field : fields) {
            if (!field.isAnnotationPresent(Column.class)) continue;
            Column column = field.getAnnotation(Column.class);
            ColumnInfo columnInfo = new ColumnInfo(field);
            int length = 0;
            try {
                Object o = mySqlTypeAndLengthMap.get(columnInfo.getType());
                if (o != null) length = (Integer) o;
            } catch (Exception e) {
                log.error("未知的Mysql数据类型字段:{}", column.type());
            }
            columnInfo.setTypeLength(length);
            normalizeEnumDefaultValue(clazz, field, columnInfo);
            ColumnDefaultRules.sanitizeMysqlColumnDefault(columnInfo, clazz.getName());
            list.add(columnInfo);
        }
        return list;
    }

    /**
     * 枚举字段的默认值强制校验：非法值统一重置为 NULL。
     */
    private void normalizeEnumDefaultValue(Class<?> clazz, Field field, ColumnInfo columnInfo) {
        if (!field.getType().isEnum()) {
            return;
        }

        String defaultValue = columnInfo.getDefaultValue();
        if (defaultValue == null) {
            columnInfo.setDefaultValue("NULL");
            return;
        }

        String normalized = stripQuote(defaultValue.trim());
        if (normalized.isEmpty() || "NULL".equalsIgnoreCase(normalized)) {
            columnInfo.setDefaultValue("NULL");
            return;
        }

        Object[] enumConstants = field.getType().getEnumConstants();
        boolean matched = false;
        if (enumConstants != null) {
            for (Object enumConstant : enumConstants) {
                if (enumConstant instanceof Enum<?> e) {
                    if (e.name().equals(normalized)) {
                        matched = true;
                        break;
                    }
                }
            }
        }

        if (!matched) {
            log.warn("枚举字段默认值非法，已强制重置为NULL: {}.{} defaultValue={}", clazz.getName(), field.getName(), defaultValue);
            columnInfo.setDefaultValue("NULL");
            return;
        }

        // 合法值统一去掉外层引号，避免后续SQL拼接重复引号
        columnInfo.setDefaultValue(normalized);
    }

    private String stripQuote(String value) {
        if (value.length() >= 2) {
            boolean single = value.startsWith("'") && value.endsWith("'");
            boolean dbl = value.startsWith("\"") && value.endsWith("\"");
            if (single || dbl) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private boolean isInvalidEnumDefault(ColumnInfo columnInfo, String dbDefaultValue) {
        if (columnInfo == null || !columnInfo.isEnumType()) {
            return false;
        }
        if (dbDefaultValue == null) {
            return false;
        }
        String normalized = stripQuote(dbDefaultValue.trim());
        if (normalized.isEmpty()) {
            return true;
        }
        if ("NULL".equalsIgnoreCase(normalized)) {
            return false;
        }
        Set<String> enumConstants = columnInfo.getEnumConstants();
        return enumConstants != null && !enumConstants.isEmpty() && !enumConstants.contains(normalized);
    }

    /**
     * 列出所有带 @Table 注解的实体及其基本属性、列信息，用于 reinit 页面展示
     */
    public List<Map<String, Object>> listTableEntities() {
        Set<Class<?>> classes = getClasses();
        Map<String, Object> typeMap = mySqlTypeAndLengthMap();
        List<Map<String, Object>> out = new ArrayList<>();
        discoverQueryIndexes();
        for (Class<?> clazz : classes) {
            TableInfo ti = new TableInfo(clazz);
            if (!ti.isValid()) continue;
            QueryIndexMerger.applyTo(ti, clazz);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("className", clazz.getSimpleName());
            m.put("classFullName", clazz.getName());
            m.put("tableName", ti.getName());
            m.put("comment", ti.getComment() != null ? ti.getComment() : "");
            m.put("engine", ti.getEngine() != null ? ti.getEngine() : "");
            m.put("charset", ti.getCharset() != null ? ti.getCharset() : "");
            m.put("collation", ti.getCollation() != null ? ti.getCollation() : "");
            m.put("prefix", ti.getPrefix() != null ? ti.getPrefix() : "");
            List<Map<String, Object>> cols = new ArrayList<>();
            for (ColumnInfo c : collectColumnInfos(clazz, typeMap)) {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("name", c.getName());
                cm.put("type", c.getType());
                cm.put("length", c.getLength());
                cm.put("decimalLength", c.getDecimalLength());
                cm.put("isKey", c.isKey());
                cm.put("isNull", c.isNull());
                cm.put("isAutoIncrement", c.isAutoIncrement());
                cm.put("isUnique", c.isUnique());
                cm.put("defaultValue", c.getDefaultValue() != null ? c.getDefaultValue() : "");
                cm.put("comment", c.getComment() != null ? c.getComment() : "");
                cm.put("explicitCharset", c.getExplicitCharset());
                cm.put("explicitCollation", c.getExplicitCollation());
                cols.add(cm);
            }
            m.put("columns", cols);
            out.add(m);
        }
        out.sort(Comparator.comparing(m -> String.valueOf(m.get("tableName")).toLowerCase()));
        return out;
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
                                              Map<TableInfo, List<Object>> removeIndexTableMap,
                                              List<IndexRebuildTask> rebuildIndexTasks,
                                              List<TableInfo> charsetSyncTables) {
        // 1. 创建表
        createTableByMap(newTableMap);
        // 2. 删除要变更主键的表的原来的字段的主键
        dropFieldsKeyByMap(dropKeyTableMap);
        // 3. 删除要变更唯一约束的表的原来的字段的唯一约束
        dropFieldsUniqueByMap(dropUniqueTableMap);
        // 4. 先删多余索引（须在列 charset MODIFY / 表 CONVERT 之前，避免超长键）
        removeIndexByMap(removeIndexTableMap);
        // 5. 修改字段类型等（含列级字符集；varchar→TEXT / 缩短须在加列之前，否则 InnoDB 行宽已满时 ADD 会被吞掉）
        modifyFieldsByMap(modifyTableMap);
        // 6. 添加新的字段
        addFieldsByMap(addTableMap);
        // 7. 删除实体中已不存在的字段（保持在 ADD 之后：改名时若先 DROP 再 ADD 失败会丢旧列数据）
        removeFieldsByMap(removeTableMap);
        // 8. 同名索引重建（列序/定义）：逐条 DROP+ADD，失败不阻断启动
        rebuildIndexes(rebuildIndexTasks);
        // 9. 新增索引
        addIndexByMap(addIndexTableMap);
        // 10. 表级字符集与实体对齐（CONVERT TO）
        if (charsetSyncTables != null) {
            for (TableInfo tableInfo : charsetSyncTables) {
                syncTableCharsetIfNeeded(tableInfo);
            }
        }
    }

    /**
     * 执行索引升级：NAME 优先 RENAME，否则（或失败时）DROP+ADD；ORDER/DEFINITION 成对 DROP+ADD。
     * 无任务则静默返回。成功时最多一条 INFO 汇总；明细为 debug；失败仍 WARN。
     */
    private void rebuildIndexes(List<IndexRebuildTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        int orderCount = 0;
        int nameCount = 0;
        for (IndexRebuildTask t : tasks) {
            if (t.getReason() == IndexRebuildTask.Reason.ORDER) {
                orderCount++;
            } else if (t.getReason() == IndexRebuildTask.Reason.NAME) {
                nameCount++;
            }
        }
        log.debug("Index rebuild upgrade begin: count={} orderFixes={} nameRenames={}", tasks.size(), orderCount, nameCount);
        int ok = 0;
        int fail = 0;
        for (IndexRebuildTask task : tasks) {
            String tableName = task.getTableName();
            String indexName = task.getIndexName();
            String fromCols = IndexDiffEngine.formatColumns(task.getDbIndex());
            String toCols = IndexDiffEngine.formatColumns(task.getEntityIndex());
            log.debug("Index rebuild start: reason={} table={} index={} fromName={} toName={} from=[{}] to=[{}]",
                    task.getReason(), tableName, indexName,
                    task.getDbIndex() == null ? "-" : task.getDbIndex().getName(),
                    task.getEntityIndex() == null ? "-" : task.getEntityIndex().getName(),
                    fromCols, toCols);
            long t0 = System.currentTimeMillis();
            // 无法发射时禁止 DROP，避免库侧索引被删后 ADD 失败导致「有定义无索引」
            if (!IndexDialectCapability.canEmit(
                    IndexDialectCapability.Family.MYSQL,
                    task.getEntityIndex())) {
                log.warn("Index rebuild skipped (cannot emit; keep DB index): table={} index={}",
                        tableName, indexName);
                fail++;
                continue;
            }
            if (task.getReason() == IndexRebuildTask.Reason.NAME) {
                if (tryRenameIndex(task)) {
                    ok++;
                    log.debug("Index rename ok: table={} from={} to={} elapsedMs={}", tableName,
                            task.getDbIndex().getName(), task.getEntityIndex().getName(),
                            System.currentTimeMillis() - t0);
                    continue;
                }
                log.debug("Index rename unavailable or failed, fallback DROP+ADD: table={} index={}", tableName, indexName);
            }
            Map<TableInfo, Object> dropMap = new HashMap<>();
            dropMap.put(task.getTable(), task.getDbIndex());
            try {
                tableDao.dropIndex(dropMap);
            } catch (Throwable e) {
                fail++;
                log.warn("Index rebuild failed: table={} index={} phase=DROP error={}", tableName, indexName, ddlError(e));
                continue;
            }
            Map<TableInfo, Object> addMap = new HashMap<>();
            addMap.put(task.getTable(), task.getEntityIndex());
            try {
                tableDao.addIndex(addMap);
                ok++;
                log.debug("Index rebuild ok: table={} index={} elapsedMs={}", tableName, indexName,
                        System.currentTimeMillis() - t0);
            } catch (Throwable e) {
                fail++;
                log.warn("Index rebuild failed: table={} index={} phase=ADD error={} (index dropped; retry next startup)",
                        tableName, indexName, ddlError(e));
            }
        }
        if (ok > 0 || fail > 0) {
            log.info("Index rebuild upgrade: ok={} failed={} orderFixes={} nameRenames={}", ok, fail, orderCount, nameCount);
        }
    }

    /** MySQL {@code RENAME INDEX}；其它方言或失败返回 false，由调用方 DROP+ADD。 */
    private boolean tryRenameIndex(IndexRebuildTask task) {
        if (task == null || task.getTable() == null || task.getDbIndex() == null || task.getEntityIndex() == null) {
            return false;
        }
        String from = task.getDbIndex().getName();
        String to = task.getEntityIndex().getName();
        if (StringUtils.isBlank(from) || StringUtils.isBlank(to) || from.equals(to)) {
            return false;
        }
        try {
            Map<TableInfo, Object> map = new HashMap<>();
            map.put(task.getTable(), task);
            tableDao.renameIndex(map);
            return true;
        } catch (Throwable e) {
            log.debug("Index rename skipped: table={} from={} to={} error={}",
                    task.getTableName(), from, to, ddlError(e));
            return false;
        }
    }

    /**
     * 根据map结构修改表中的字段类型等
     *
     * @param modifyTableMap 用于存需要更新字段类型等的表名+结构
     */
    private void modifyFieldsByMap(Map<TableInfo, List<Object>> modifyTableMap) {
        // 做修改字段操作
        if (!modifyTableMap.isEmpty()) {
            for (Entry<TableInfo, List<Object>> entry : modifyTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<>();
                    map.put(entry.getKey(), obj);
                    try {
                        tableDao.modifyColumn(map);
                    } catch (Throwable e) {
                        log.warn("Modify Columns:{}", ddlError(e));
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
        if (!removeTableMap.isEmpty()) {
            for (Entry<TableInfo, List<Object>> entry : removeTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<>();
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
        if (!addTableMap.isEmpty()) {
            for (Entry<TableInfo, List<Object>> entry : addTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<>();
                    map.put(entry.getKey(), obj);
                    try {
                        tableDao.addColumns(map);
                    } catch (Throwable e) {
                        String col = obj instanceof ColumnInfo ? ((ColumnInfo) obj).getName() : String.valueOf(obj);
                        log.warn("Add Columns:{} {}", col, ddlError(e));
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
        if (!dropKeyTableMap.isEmpty()) {
            for (Entry<TableInfo, List<Object>> entry : dropKeyTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<>();
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
        if (!dropUniqueTableMap.isEmpty()) {
            for (Entry<TableInfo, List<Object>> entry : dropUniqueTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<>();
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
        if (!addIndexTableMap.isEmpty()) {
            for (Entry<TableInfo, List<Object>> entry : addIndexTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<>();
                    map.put(entry.getKey(), obj);
                    String idxName = obj instanceof IndexInfo ? ((IndexInfo) obj).getName() : String.valueOf(obj);
                    try {
                        if (obj instanceof IndexInfo
                                && !IndexDialectCapability.canEmit(
                                IndexDialectCapability.Family.MYSQL,
                                (IndexInfo) obj)) {
                            log.debug("Index add skipped (cannot emit): table={} index={}",
                                    entry.getKey().getName(), idxName);
                            continue;
                        }
                        tableDao.addIndex(map);
                        log.debug("Index add ok: table={} index={}", entry.getKey().getName(), idxName);
                    } catch (Throwable e) {
                        String err = ddlError(e);
                        String hint = IndexLimitGuard.classifyAddFailure(err);
                        if (StringUtils.isNotBlank(hint)) {
                            log.warn("Index add failed: table={} index={} reason={} error={}",
                                    entry.getKey().getName(), idxName, hint, err);
                        } else {
                            log.warn("Index add failed: table={} index={} error={}",
                                    entry.getKey().getName(), idxName, err);
                        }
                    }
                }
            }
        }
    }

    private void removeIndexByMap(Map<TableInfo, List<Object>> removeIndexTableMap) {
        if (!removeIndexTableMap.isEmpty()) {
            for (Entry<TableInfo, List<Object>> entry : removeIndexTableMap.entrySet()) {
                for (Object obj : entry.getValue()) {
                    Map<TableInfo, Object> map = new HashMap<>();
                    map.put(entry.getKey(), obj);
                    String idxName = obj instanceof IndexInfo ? ((IndexInfo) obj).getName() : String.valueOf(obj);
                    try {
                        tableDao.dropIndex(map);
                        log.debug("Index drop ok: table={} index={}", entry.getKey().getName(), idxName);
                    } catch (Throwable e) {
                        log.warn("Index drop failed: table={} index={} error={}", entry.getKey().getName(), idxName, ddlError(e));
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
        if (!newTableMap.isEmpty()) {
            for (Entry<TableInfo, List<Object>> entry : newTableMap.entrySet()) {
                Map<TableInfo, List<Object>> map = new HashMap<>();
                map.put(entry.getKey(), entry.getValue());
                try {
                    tableDao.createTable(map);
                } catch (Throwable e) {
                    log.warn("注解建表失败 [{}]: {}", entry.getKey().getName(), e.toString(), e);
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
        Map<String, Object> map = new HashMap<>();
        for (Field field : fields) {
            LengthCount lengthCount = field.getAnnotation(LengthCount.class);
            if (null != lengthCount)
                map.put(field.getName().toLowerCase(), lengthCount.LengthCount());
        }
        return map;
    }

    private static String ddlError(Throwable e) {
        if (e == null) {
            return "";
        }
        String m = e.getMessage();
        if (m == null) {
            return e.getClass().getSimpleName();
        }
        return m.replace('\r', ' ').replace('\n', ' ').replaceAll(" +", " ").trim();
    }

    public void dropTable(String tableName) {
        try {
            tableDao.dropTable(tableName);
        } catch (Throwable e) {
            log.debug("Drop Table:{}", e.getMessage());
        }
    }
}
