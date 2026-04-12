package cn.org.autumn.config;

import cn.org.autumn.database.DatabaseHolder;
import com.baomidou.mybatisplus.plugins.PaginationInterceptor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import java.sql.Connection;

/**
 * 使分页方言随 {@link DatabaseHolder#getType()}（即当前线程数据源 lookup key）变化；
 * MyBatis-Plus 2.x 默认仅在 Bean 创建时 {@code setDialectType} 一次。
 * <p>
 * <b>并发安全</b>：{@link PaginationInterceptor} 为单例 Bean，方言保存在实例字段 {@code dialectType} 中；父类
 * {@code intercept} 在方法前段执行 {@code sqlParser} 等逻辑后<b>才</b>读取该字段。若仅由本类在入口
 * {@code setDialectType} 而无互斥，其它线程可在「已写入」与「父类读取」之间改写字段，导致分页 SQL 方言错乱。
 * 因此对本类中 {@code setDialectType} 与 {@code super.intercept} 使用同一把锁串行化。
 * <p>
 * <b>吞吐</b>：锁覆盖整条父类拦截路径，会全局序列化所有命中该插件的 {@link org.apache.ibatis.executor.statement.StatementHandler}
 * 调用；异构多源且高并发分页时可能成为瓶颈。若压测不可接受，可改为 fork MP 2.x
 * {@code PaginationInterceptor}，在解析 {@code DBType} 处用 {@link ThreadLocal} 传递方言字符串而非共享可变字段
 * （需保留 Baomidou 版权头并随依赖小版本人工合并差异）。
 * <p>
 * <b>插件元数据</b>：MyBatis 注册插件时只解析<b>具体类</b>上的 {@link Intercepts}，不会继承
 * {@link PaginationInterceptor} 上的注解，故须在此重复声明与父类一致的 {@link Signature}。
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class ThreadLocalPaginationInterceptor extends PaginationInterceptor {

    private final DatabaseHolder databaseHolder;

    private final Object dialectGuard = new Object();

    public ThreadLocalPaginationInterceptor(DatabaseHolder databaseHolder) {
        this.databaseHolder = databaseHolder;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String dialect = null;
        if (databaseHolder != null) {
            dialect = databaseHolder.getType().pageHelperDialectName();
        }
        synchronized (dialectGuard) {
            if (dialect != null) {
                setDialectType(dialect);
            }
            return super.intercept(invocation);
        }
    }
}
