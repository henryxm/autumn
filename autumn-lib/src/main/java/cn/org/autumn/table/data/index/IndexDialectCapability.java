package cn.org.autumn.table.data.index;

import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.data.IndexInfo;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 方言索引能力矩阵：不支持的特性 WARN 并跳过/降级发射。
 * <p>
 * {@link #forEmit} 返回副本并剥离不支持项，<strong>不修改</strong>实体侧 IndexInfo，
 * 避免 diff 阶段因原地清空 where/include 等导致状态漂移。
 */
@Slf4j
public final class IndexDialectCapability {

    public enum Family {
        MYSQL,
        POSTGRES,
        SQLSERVER,
        ORACLE,
        SQLITE,
        H2,
        ANSI
    }

    private IndexDialectCapability() {
    }

    /**
     * 索引标识符最大长度：MySQL/SQL Server/H2/SQLite/ANSI=64，PostgreSQL=63，Oracle 旧标识符=30。
     * 可用 {@code autumn.table.index.max-name-length} 覆盖。
     */
    public static int maxIndexNameLength(Family family) {
        int override = IndexTableSettings.maxNameLengthOverride();
        if (override > 0) {
            return override;
        }
        if (family == null) {
            return IndexInfo.DEFAULT_MAX_INDEX_NAME_LENGTH;
        }
        switch (family) {
            case POSTGRES:
                return 63;
            case ORACLE:
                return 30;
            case MYSQL:
            case SQLSERVER:
            case SQLITE:
            case H2:
            case ANSI:
            default:
                return IndexInfo.DEFAULT_MAX_INDEX_NAME_LENGTH;
        }
    }

    /** 默认按 MySQL 族长度（可用配置覆盖）。 */
    public static int maxIndexNameLength() {
        return maxIndexNameLength(Family.MYSQL);
    }

    /**
     * @return true 表示可以发射（调用方应使用 {@link #forEmit} 获取降级后的副本）
     */
    public static boolean prepareForEmit(Family family, IndexInfo index) {
        return canEmit(family, index);
    }

    /**
     * 静默判断是否可发射（不打方言降级 WARN，避免 ADD 前预检与 SQL 生成双重刷屏）。
     * 硬拒绝：null / {@code skipEmit} / 方言不支持的 FULLTEXT·SPATIAL / 列数超限。
     */
    public static boolean canEmit(Family family, IndexInfo index) {
        if (index == null || index.isSkipEmit()) {
            return false;
        }
        IndexInfo probe = IndexInfo.copyOf(index);
        probe.resolve();
        if (IndexTypeEnum.FULLTEXT.name().equals(probe.getIndexType()) && family != Family.MYSQL) {
            return false;
        }
        if (IndexTypeEnum.SPATIAL.name().equals(probe.getIndexType()) && family != Family.MYSQL) {
            return false;
        }
        int maxCols = IndexTableSettings.maxColumnsPerIndex();
        return maxCols <= 0 || IndexLimitGuard.countKeyParts(probe.getColumnSpecs()) <= maxCols;
    }

    /**
     * 生成用于 DDL 的降级副本；不支持的类型或预检 skipEmit 返回 null（已 WARN）。
     * 原 {@code index} 不被修改。
     */
    public static IndexInfo forEmit(Family family, IndexInfo index) {
        if (index == null) {
            return null;
        }
        if (index.isSkipEmit()) {
            // 预检阶段已 WARN，此处仅 debug 避免重复刷屏
            log.debug("Index skipped by limit guard: name={}", index.getName());
            return null;
        }
        // 仅副本 resolve：实体侧 UNIQUE 等不得被 Non_unique 空值降级，否则 ADD 丢唯一性并每启重建
        IndexInfo emit = IndexInfo.copyOf(index);
        emit.resolve();
        if (IndexTypeEnum.FULLTEXT.name().equals(emit.getIndexType())) {
            if (family != Family.MYSQL) {
                log.warn("Index FULLTEXT skipped for dialect {}: name={}", family, index.getName());
                return null;
            }
        }
        if (IndexTypeEnum.SPATIAL.name().equals(emit.getIndexType())) {
            if (family != Family.MYSQL) {
                log.warn("Index SPATIAL skipped for dialect {}: name={}", family, index.getName());
                return null;
            }
        }
        if (StringUtils.isNotBlank(emit.getWherePredicate()) && family == Family.MYSQL) {
            log.warn("Index WHERE ignored on MySQL: name={}", emit.getName());
            emit.setWherePredicate("");
        }
        if (emit.getIncludeColumns() != null && !emit.getIncludeColumns().isEmpty()) {
            if (family == Family.MYSQL || family == Family.SQLITE || family == Family.ORACLE || family == Family.H2) {
                log.warn("Index INCLUDE ignored for dialect {}: name={}", family, emit.getName());
                emit.setIncludeColumns(null);
            }
        }
        if (emit.isInvisible() && family != Family.MYSQL) {
            log.warn("Index INVISIBLE ignored for dialect {}: name={}", family, emit.getName());
            emit.setInvisible(false);
        }
        if (StringUtils.isNotBlank(emit.getParser()) && family != Family.MYSQL) {
            log.warn("Index parser ignored for dialect {}: name={}", family, emit.getName());
            emit.setParser("");
        }
        int maxCols = IndexTableSettings.maxColumnsPerIndex();
        if (maxCols > 0 && IndexLimitGuard.countKeyParts(emit.getColumnSpecs()) > maxCols) {
            log.warn("Index column count exceeds limit for emit: name={} columns={} max={}",
                    emit.getName(), IndexLimitGuard.countKeyParts(emit.getColumnSpecs()), maxCols);
            return null;
        }
        return emit;
    }
}
