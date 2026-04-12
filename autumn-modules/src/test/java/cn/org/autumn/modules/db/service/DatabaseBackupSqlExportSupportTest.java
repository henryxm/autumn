package cn.org.autumn.modules.db.service;

import cn.org.autumn.database.DatabaseType;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DatabaseBackupSqlExportSupportTest {

    @Test
    public void derbyIncludesSysPrefixedBusinessTables() throws Exception {
        Method m = DatabaseBackupSqlExportSupport.class.getDeclaredMethod(
                "isUserTable", String.class, String.class, DatabaseType.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "APP", "sys_user", DatabaseType.DERBY));
        assertTrue((Boolean) m.invoke(null, "APP", "SYS_CONFIG", DatabaseType.DERBY));
        assertFalse((Boolean) m.invoke(null, "SYS", "SYSTABLES", DatabaseType.DERBY));
    }

    @Test
    public void db2IncludesSysPrefixedBusinessTables() throws Exception {
        Method m = DatabaseBackupSqlExportSupport.class.getDeclaredMethod(
                "isUserTable", String.class, String.class, DatabaseType.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, "MYAPP", "sys_menu", DatabaseType.DB2));
    }
}
