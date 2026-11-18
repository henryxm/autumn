package cn.org.autumn.table.data.index;

import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.data.IndexInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 方言索引能力矩阵：不支持的特性 WARN 并跳过/降级发射。
 * <p>
 * {@link #forEmit} 返回副本并剥离不支持项，<strong>不修改</strong>实体侧 IndexInfo，
 * 避免 diff 阶段因原地清空 where/include 等导致状态漂移。
 */
public final class IndexDialectCapability {

    private static final Logger log = LoggerFactory.getLogger(IndexDialectCapability.class);

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
     * @return true 表示可以发射（调用方应使用 {@link #forEmit} 获取降级后的副本）
     */
    public static boolean prepareForEmit(Family family, IndexInfo index) {
        return forEmit(family, index) != null;
    }

    /**
     * 生成用于 DDL 的降级副本；不支持的类型返回 null（已 WARN）。
     * 原 {@code index} 不被修改。
     */
    public static IndexInfo forEmit(Family family, IndexInfo index) {
        if (index == null) {
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
        return emit;
    }
}
