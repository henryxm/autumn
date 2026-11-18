package cn.org.autumn.table.data.index;

import cn.org.autumn.table.data.IndexInfo;
import cn.org.autumn.table.data.TableInfo;

/**
 * 索引升级任务：定义变更时 DROP+ADD；仅改名时优先 RENAME（失败则 DROP+ADD）。
 */
public final class IndexRebuildTask {

    public enum Reason {
        /** 列集合相同，仅顺序或 ASC/DESC 不一致（历史 HashMap 错序升级） */
        ORDER,
        /** 同名但类型/方法/列集合等定义不一致 */
        DEFINITION,
        /** 定义指纹相同，仅物理索引名不同 */
        NAME
    }

    private final TableInfo table;
    private final IndexInfo dbIndex;
    private final IndexInfo entityIndex;
    private final Reason reason;

    public IndexRebuildTask(TableInfo table, IndexInfo dbIndex, IndexInfo entityIndex, Reason reason) {
        this.table = table;
        this.dbIndex = dbIndex;
        this.entityIndex = entityIndex;
        this.reason = reason == null ? Reason.DEFINITION : reason;
    }

    public TableInfo getTable() {
        return table;
    }

    public String getTableName() {
        return table == null ? "" : table.getName();
    }

    public IndexInfo getDbIndex() {
        return dbIndex;
    }

    public IndexInfo getEntityIndex() {
        return entityIndex;
    }

    public Reason getReason() {
        return reason;
    }

    public String getIndexName() {
        if (entityIndex != null && entityIndex.getName() != null) {
            return entityIndex.getName();
        }
        return dbIndex == null ? "" : dbIndex.getName();
    }
}
