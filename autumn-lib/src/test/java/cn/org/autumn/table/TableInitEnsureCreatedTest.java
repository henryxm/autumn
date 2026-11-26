package cn.org.autumn.table;

import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.table.service.MysqlTableService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TableInitEnsureCreatedTest {

    @Test
    public void ensureCreated_retriesAfterFailure() {
        TableInit tableInit = new TableInit();
        SysConfigSchemaGate gate = new SysConfigSchemaGate();
        MysqlTableService mysql = mock(MysqlTableService.class);
        DatabaseHolder holder = mock(DatabaseHolder.class);
        EnvBean envBean = new EnvBean();
        envBean.setTableInit(true);
        when(holder.getType()).thenReturn(DatabaseType.MYSQL);
        doThrow(new IllegalStateException("boom")).doNothing().when(mysql).create();

        ReflectionTestUtils.setField(tableInit, "schemaGate", gate);
        ReflectionTestUtils.setField(tableInit, "mysqlTableService", mysql);
        ReflectionTestUtils.setField(tableInit, "databaseHolder", holder);
        ReflectionTestUtils.setField(tableInit, "envBean", envBean);
        ReflectionTestUtils.setField(tableInit, "init", true);
        Environment env = new MockEnvironment();
        ReflectionTestUtils.setField(tableInit, "springEnvironment", env);

        try {
            tableInit.ensureCreated();
            Assert.fail("expected failure");
        } catch (IllegalStateException expected) {
            Assert.assertEquals("boom", expected.getMessage());
        }
        Assert.assertFalse(gate.isSchemaReady());

        Assert.assertTrue(tableInit.ensureCreated());
        Assert.assertTrue(gate.isSchemaReady());
        verify(mysql, times(2)).create();
    }

    @Test
    public void ensureCreated_skipsInstallAndIsIdempotent() {
        TableInit tableInit = new TableInit();
        SysConfigSchemaGate gate = new SysConfigSchemaGate();
        MysqlTableService mysql = mock(MysqlTableService.class);
        EnvBean envBean = new EnvBean();
        envBean.setTableInit(true);
        MockEnvironment env = new MockEnvironment();
        env.setProperty("autumn.install.mode", "true");

        ReflectionTestUtils.setField(tableInit, "schemaGate", gate);
        ReflectionTestUtils.setField(tableInit, "mysqlTableService", mysql);
        ReflectionTestUtils.setField(tableInit, "envBean", envBean);
        ReflectionTestUtils.setField(tableInit, "init", true);
        ReflectionTestUtils.setField(tableInit, "springEnvironment", env);

        Assert.assertFalse(tableInit.ensureCreated());
        Assert.assertFalse(tableInit.ensureCreated());
        verify(mysql, times(0)).create();
        Assert.assertFalse(gate.isSchemaReady());
    }
}
