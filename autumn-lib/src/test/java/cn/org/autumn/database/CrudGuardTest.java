package cn.org.autumn.database;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.Error;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.mapping.SqlCommandType;
import org.junit.Assert;
import org.junit.Test;

public class CrudGuardTest {

    @Test
    public void databaseWriteOffBlocksSystemScope() {
        CrudGuard guard = new CrudGuard();
        guard.apply(false, true, "只读");
        try {
            guard.check();
            Assert.fail("应拒绝系统级写入");
        } catch (AException e) {
            Assert.assertEquals(834, e.getCode());
        }
    }

    @Test
    public void localWriteOffBlocksUserScopeOnly() {
        CrudGuard guard = new CrudGuard();
        guard.apply(true, false, "维护中");
        guard.check();
        guard.user();
        try {
            guard.check();
            Assert.fail("应拒绝用户级写入");
        } catch (AException e) {
            Assert.assertEquals(834, e.getCode());
        } finally {
            guard.clear();
        }
    }

    @Test
    public void forceBypassesDatabaseWriteOff() {
        CrudGuard guard = new CrudGuard();
        guard.apply(false, false, "只读");
        CrudGuard.force(guard::check);
    }

    @Test
    public void asyncScopePropagationPreservesUserScope() throws Exception {
        CrudGuard guard = new CrudGuard();
        guard.apply(true, false, "维护中");
        guard.user();
        CrudGuard.Snapshot snapshot = CrudGuard.capture();
        final boolean[] blocked = {false};
        Thread t = new Thread(() -> CrudGuard.with(snapshot, () -> {
            try {
                guard.check();
            } catch (AException e) {
                blocked[0] = true;
            }
        }));
        t.start();
        t.join();
        Assert.assertTrue("异步线程应继承 USER 作用域并被拦截", blocked[0]);
        guard.clear();
    }

    @Test
    public void forceReturnsValue() throws Exception {
        CrudGuard guard = new CrudGuard();
        guard.apply(false, true, "只读");
        String value = CrudGuard.force(() -> {
            guard.check();
            return "ok";
        });
        Assert.assertEquals("ok", value);
    }

    @Test
    public void allowMatchesStaticWritable() {
        CrudGuard guard = new CrudGuard();
        guard.apply(false, true, "只读");
        Assert.assertFalse(guard.allow());
        guard.apply(true, false, "维护中");
        Assert.assertTrue(guard.allow());
        guard.user();
        Assert.assertFalse(guard.allow());
        guard.clear();
    }

    @Test
    public void optSkipsWhenDatabaseWriteOff() {
        CrudGuard guard = new CrudGuard();
        guard.apply(false, true, "read-only");
        CrudGuard.bindHolder(guard);
        try {
            final boolean[] ran = {false};
            CrudGuard.opt(() -> ran[0] = true);
            Assert.assertFalse(ran[0]);
        } finally {
            CrudGuard.clearHolder();
        }
    }

    @Test
    public void blockedDetectsWrappedAException() {
        AException root = new AException(Error.DATABASE_READ_ONLY);
        Assert.assertTrue(CrudGuard.blocked(new PersistenceException("x", root)));
    }

    @Test
    public void writeInterceptorTreatsInsertAsWrite() {
        Assert.assertTrue(CrudInterceptor.write(SqlCommandType.INSERT));
        Assert.assertTrue(CrudInterceptor.write(SqlCommandType.UPDATE));
        Assert.assertTrue(CrudInterceptor.write(SqlCommandType.DELETE));
        Assert.assertFalse(CrudInterceptor.write(SqlCommandType.SELECT));
    }
}
