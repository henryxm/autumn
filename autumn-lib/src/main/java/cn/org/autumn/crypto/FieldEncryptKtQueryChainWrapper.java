package cn.org.autumn.crypto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.kotlin.KtQueryChainWrapper;

import java.util.List;
import java.util.Optional;

/**
 * {@link cn.org.autumn.base.EncryptModuleService#ktQuery()} 装饰链：终端读方法自动 {@code afterRead}。
 */
public class FieldEncryptKtQueryChainWrapper<T> extends KtQueryChainWrapper<T> {

    private final FieldEncryptChainQuerySupport<T> readSupport;

    public FieldEncryptKtQueryChainWrapper(BaseMapper<T> baseMapper, Class<T> entityClass, FieldEncryptChainQuerySupport<T> readSupport) {
        super(baseMapper, entityClass);
        this.readSupport = readSupport;
    }

    @Override
    public List<T> list() {
        return readSupport.afterList(super.list());
    }

    @Override
    public List<T> list(IPage<T> page) {
        return readSupport.afterList(super.list(page));
    }

    @Override
    public T one() {
        return readSupport.afterOne(super.one());
    }

    @Override
    public Optional<T> oneOpt() {
        return readSupport.afterOneOpt(super.oneOpt());
    }

    @Override
    public <E extends IPage<T>> E page(E page) {
        return readSupport.afterPage(super.page(page));
    }
}
