package cn.org.autumn.crypto;

import cn.org.autumn.table.data.TableInfo;
import cn.org.autumn.table.service.MysqlTableService;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * 字段存储加密实体清单与批量处理（管理 API 内部使用）。
 */
@Slf4j
@Service
public class FieldEncryptMigrationService {

    @Autowired
    private FieldEncryptService fieldEncryptService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    MysqlTableService mysqlTableService;

    public List<Map<String, Object>> listEncryptEntities() {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<Class<?>> classes = mysqlTableService.getClasses();
        if (classes == null) {
            return out;
        }
        for (Class<?> clazz : classes) {
            fieldEncryptService.registerEntity(clazz);
            if (!fieldEncryptService.hasEncryptedFields(clazz)) {
                continue;
            }
            TableInfo ti = new TableInfo(clazz);
            if (!ti.isValid()) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("className", clazz.getSimpleName());
            m.put("classFullName", clazz.getName());
            m.put("tableName", ti.getName());
            m.put("comment", ti.getComment() != null ? ti.getComment() : "");
            List<Map<String, Object>> fields = new ArrayList<>();
            for (FieldMeta meta : fieldEncryptService.getFields(clazz)) {
                Map<String, Object> fm = new LinkedHashMap<>();
                fm.put("fieldName", meta.getFieldName());
                fm.put("searchable", meta.isSearchable());
                fm.put("hashField", meta.getHashFieldName());
                fm.put("vector", meta.getVector());
                fields.add(fm);
            }
            m.put("encryptFields", fields);
            out.add(m);
        }
        out.sort((a, b) -> String.valueOf(a.get("tableName")).compareToIgnoreCase(String.valueOf(b.get("tableName"))));
        return out;
    }

    public MigrationResult migrate(String entityClassName, boolean dryRun, int batchSize) throws ClassNotFoundException {
        return runBatch(entityClassName, dryRun, batchSize, true);
    }

    public MigrationResult restore(String entityClassName, boolean dryRun, int batchSize) throws ClassNotFoundException {
        return runBatch(entityClassName, dryRun, batchSize, false);
    }

    public MigrationResult migrateOne(String entityClassName, String id, boolean dryRun) throws ClassNotFoundException {
        return runOne(entityClassName, id, dryRun, true);
    }

    public MigrationResult restoreOne(String entityClassName, String id, boolean dryRun) throws ClassNotFoundException {
        return runOne(entityClassName, id, dryRun, false);
    }

    private MigrationResult runBatch(String entityClassName, boolean dryRun, int batchSize, boolean encrypt) throws ClassNotFoundException {
        Class<?> entityClass = prepareEntityClass(entityClassName);
        assertWriteOrReadReady(encrypt);
        if (batchSize <= 0) {
            batchSize = 200;
        }
        BaseMapper mapper = resolveMapper(entityClass);
        MigrationResult result = new MigrationResult(entityClassName, dryRun, encrypt ? "migrate" : "restore");
        int offset = 0;
        final int limit = batchSize;
        while (true) {
            final int off = offset;
            List rows = FieldEncryptContext.runSkip(() -> mapper.selectList(new EntityWrapper<>().last("LIMIT " + limit + " OFFSET " + off)));
            if (rows == null || rows.isEmpty()) {
                break;
            }
            for (Object row : rows) {
                result.incrementScanned();
                if (!matchesAction(row, encrypt)) {
                    continue;
                }
                result.incrementPending();
                if (!dryRun) {
                    persistRow(mapper, row, encrypt);
                    result.incrementProcessed();
                }
            }
            if (rows.size() < limit) {
                break;
            }
            offset += limit;
        }
        return result;
    }

    private MigrationResult runOne(String entityClassName, String id, boolean dryRun, boolean encrypt) throws ClassNotFoundException {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("id 不能为空");
        }
        Class<?> entityClass = prepareEntityClass(entityClassName);
        assertWriteOrReadReady(encrypt);
        BaseMapper mapper = resolveMapper(entityClass);
        Serializable pk = parseId(id);
        Object row = FieldEncryptContext.runSkip(() -> mapper.selectById(pk));
        MigrationResult result = new MigrationResult(entityClassName, dryRun, encrypt ? "migrate" : "restore");
        result.incrementScanned();
        if (row == null) {
            result.setMessage("未找到记录 id=" + id);
            return result;
        }
        if (!matchesAction(row, encrypt)) {
            result.setMessage(encrypt ? "该记录无需加密迁移（字段已为密文或为空）" : "该记录无需还原（字段非密文）");
            return result;
        }
        result.incrementPending();
        if (!dryRun) {
            persistRow(mapper, row, encrypt);
            result.incrementProcessed();
            result.setMessage("操作成功");
        } else {
            result.setMessage("预检通过，可执行");
        }
        return result;
    }

    private void persistRow(BaseMapper mapper, Object row, boolean encrypt) {
        if (encrypt) {
            FieldEncryptContext.runSkip(() -> {
                fieldEncryptService.applyEncryptBeforePersist(row);
                mapper.updateById(row);
            });
            fieldEncryptService.applyAfterRead(row);
            return;
        }
        fieldEncryptService.applyRestoreBeforeWrite(row);
        FieldEncryptContext.runSkip(() -> mapper.updateById(row));
    }

    private boolean matchesAction(Object entity, boolean encrypt) {
        for (FieldMeta meta : fieldEncryptService.getFields(entity.getClass())) {
            try {
                Object raw = meta.getField().get(entity);
                if (!(raw instanceof String)) {
                    continue;
                }
                String val = (String) raw;
                if (encrypt && fieldEncryptService.needsMigration(val)) {
                    return true;
                }
                if (!encrypt && fieldEncryptService.needsRestore(val)) {
                    return true;
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return false;
    }

    private Class<?> prepareEntityClass(String entityClassName) throws ClassNotFoundException {
        if (StringUtils.isBlank(entityClassName)) {
            throw new IllegalArgumentException("entity 不能为空");
        }
        Class<?> entityClass = Class.forName(entityClassName.trim());
        fieldEncryptService.registerEntity(entityClass);
        if (!fieldEncryptService.hasEncryptedFields(entityClass)) {
            throw new IllegalArgumentException("实体未声明 @FieldEncrypt 字段:" + entityClassName);
        }
        return entityClass;
    }

    private void assertWriteOrReadReady(boolean encrypt) {
        if (encrypt) {
            if (!fieldEncryptService.isKeyConfigured()) {
                throw new IllegalStateException("加密写入须配置有效的 autumn.crypto.field.key");
            }
            if (!fieldEncryptService.isHashKeyConfigured()) {
                throw new IllegalStateException("加密写入须配置有效的 autumn.crypto.field.hash-key");
            }
            return;
        }
        if (!fieldEncryptService.isReadDecryptEnabled()) {
            throw new IllegalStateException("还原明文须配置有效的 autumn.crypto.field.key");
        }
    }

    @SuppressWarnings("rawtypes")
    private BaseMapper resolveMapper(Class<?> entityClass) {
        String simple = entityClass.getSimpleName();
        String daoSimple = simple.endsWith("Entity") ? simple.substring(0, simple.length() - 6) + "Dao" : simple + "Dao";
        String daoClassName = toDaoPackage(entityClass.getPackage().getName()) + "." + daoSimple;
        try {
            Class<?> daoClass = Class.forName(daoClassName);
            return (BaseMapper) applicationContext.getBean(daoClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("未找到 Dao:" + daoClassName, e);
        }
    }

    /**
     * {@code *.entity} / {@code *.entity.*} → {@code *.dao} / {@code *.dao.*}
     */
    private static String toDaoPackage(String entityPackage) {
        if (entityPackage.endsWith(".entity")) {
            return entityPackage.substring(0, entityPackage.length() - ".entity".length()) + ".dao";
        }
        return entityPackage.replace(".entity.", ".dao.");
    }

    private static Serializable parseId(String id) {
        String trimmed = id.trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return trimmed;
        }
    }

    @Getter
    public static class MigrationResult {
        private final String entity;
        private final boolean dryRun;
        private final String action;
        private int scanned;
        private int pending;
        private int processed;
        private String message;

        public MigrationResult(String entity, boolean dryRun, String action) {
            this.entity = entity;
            this.dryRun = dryRun;
            this.action = action;
        }

        void incrementScanned() {
            scanned++;
        }

        void incrementPending() {
            pending++;
        }

        void incrementProcessed() {
            processed++;
        }

        void setMessage(String message) {
            this.message = message;
        }

        /**
         * 兼容旧字段名
         */
        public int getMigrated() {
            return processed;
        }
    }
}
