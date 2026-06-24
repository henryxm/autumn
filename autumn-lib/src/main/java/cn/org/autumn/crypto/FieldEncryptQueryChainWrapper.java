package cn.org.autumn.crypto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;

import java.util.List;
import java.util.Optional;

/**
 * {@link cn.org.autumn.base.EncryptModuleService#query()} 装饰链：终端读方法自动 {@code afterRead}。
 */
public class FieldEncryptQueryChainWrapper<T> extends QueryChainWrapper<T> {

    private final FieldEncryptChainQuerySupport<T> readSupport;

    public FieldEncryptQueryChainWrapper(BaseMapper<T> baseMapper, FieldEncryptChainQuerySupport<T> readSupport) {
        super(baseMapper);
        this.readSupport = readSupport;
    }

    @Override
    public LambdaQueryChainWrapper<T> lambda() {
        return new FieldEncryptLambdaQueryChainWrapper<>(getBaseMapper(), wrapperChildren.lambda(), readSupport);
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
