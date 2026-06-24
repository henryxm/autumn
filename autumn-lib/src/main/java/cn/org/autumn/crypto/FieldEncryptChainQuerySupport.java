package cn.org.autumn.crypto;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * 链式读终端方法：加密实体对结果 {@code afterRead}，否则零开销 delegate。
 */
public final class FieldEncryptChainQuerySupport<T> {

    private final BooleanSupplier encryptEnabled;
    private final Function<T, T> afterReadEntity;
    private final Function<List<T>, List<T>> afterReadList;

    public FieldEncryptChainQuerySupport(BooleanSupplier encryptEnabled, Function<T, T> afterReadEntity, Function<List<T>, List<T>> afterReadList) {
        this.encryptEnabled = encryptEnabled;
        this.afterReadEntity = afterReadEntity;
        this.afterReadList = afterReadList;
    }

    List<T> afterList(List<T> rows) {
        if (!encryptEnabled.getAsBoolean() || rows == null || rows.isEmpty()) {
            return rows;
        }
        return afterReadList.apply(rows);
    }

    T afterOne(T entity) {
        if (!encryptEnabled.getAsBoolean()) {
            return entity;
        }
        return afterReadEntity.apply(entity);
    }

    Optional<T> afterOneOpt(Optional<T> optional) {
        if (!encryptEnabled.getAsBoolean() || optional == null || !optional.isPresent()) {
            return optional;
        }
        return Optional.of(afterReadEntity.apply(optional.get()));
    }

    <E extends IPage<T>> E afterPage(E page) {
        if (encryptEnabled.getAsBoolean() && page != null && page.getRecords() != null && !page.getRecords().isEmpty()) {
            afterReadList.apply(page.getRecords());
        }
        return page;
    }
}
