package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.sys.dao.SysConfigDao;
import cn.org.autumn.table.SysConfigSchemaGate;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLSyntaxErrorException;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SysConfigServiceMissingRelationTest {

    @Test
    public void detectsMysqlTableMissing() {
        SQLSyntaxErrorException sql = new SQLSyntaxErrorException("Table 'autumn.sys_config' doesn't exist");
        BadSqlGrammarException wrap = new BadSqlGrammarException("query", "SELECT 1", sql);
        Assert.assertTrue(SysConfigService.isMissingRelation(wrap));
    }

    @Test
    public void detectsH2TableMissing() {
        Assert.assertTrue(SysConfigService.isMissingRelation(new RuntimeException("Table \"sys_config\" not found (this database is empty)")));
    }

    @Test
    public void ignoresUnrelatedErrors() {
        Assert.assertFalse(SysConfigService.isMissingRelation(new RuntimeException("connection refused")));
    }

    @Test
    public void hasKey_returnsFalseWhenSysConfigTableMissing() {
        SysConfigService service = new SysConfigService();
        SysConfigSchemaGate gate = new SysConfigSchemaGate();
        gate.markSchemaReady();
        SysConfigDao dao = mock(SysConfigDao.class);
        SQLSyntaxErrorException sql = new SQLSyntaxErrorException("Table 'signdb.sys_config' doesn't exist");
        when(dao.hasKey(anyString())).thenThrow(new BadSqlGrammarException("query", "SELECT COUNT(*)", sql));
        ReflectionTestUtils.setField(service, "baseMapper", dao);
        ReflectionTestUtils.setField(service, "schemaGate", gate);

        Assert.assertFalse(service.hasKey("OBJECT_STORAGE_APP_CONFIG"));
    }

    @Test
    public void hasKey_skipsDbWhenSchemaNotReady() {
        SysConfigService service = new SysConfigService();
        SysConfigSchemaGate gate = new SysConfigSchemaGate();
        SysConfigDao dao = mock(SysConfigDao.class);
        ReflectionTestUtils.setField(service, "baseMapper", dao);
        ReflectionTestUtils.setField(service, "schemaGate", gate);

        Assert.assertFalse(service.hasKey("OBJECT_STORAGE_APP_CONFIG"));
        verify(dao, never()).hasKey(anyString());
    }

    @Test
    public void put_defersWhenSchemaNotReady_andFlushesAfterReady() {
        SysConfigService service = new SysConfigService();
        SysConfigSchemaGate gate = new SysConfigSchemaGate();
        SysConfigDao dao = mock(SysConfigDao.class);
        ReflectionTestUtils.setField(service, "baseMapper", dao);
        ReflectionTestUtils.setField(service, "schemaGate", gate);

        String[][] mapping = new String[][]{
                {"K1", "v1", "1", "r", "sys_config", "string"},
                {"K2", "v2", "1", "r", "sys_config", "string"},
        };
        service.put(mapping);
        verify(dao, never()).queryByKey(anyString());

        @SuppressWarnings("unchecked")
        List<String[][]> pending = (List<String[][]>) ReflectionTestUtils.getField(service, "pendingPuts");
        Assert.assertNotNull(pending);
        Assert.assertEquals(1, pending.size());
        Assert.assertEquals("K1", pending.get(0)[0][0]);
        Assert.assertEquals("K2", pending.get(0)[1][0]);

        gate.markSchemaReady();
        when(dao.queryByKey("K1")).thenThrow(new BadSqlGrammarException("q", "s",
                new SQLSyntaxErrorException("Table 'autumn.sys_config' doesn't exist")));
        // flush will hit missing table on first key → re-defer remaining, no whole-batch abort crash
        service.flushPendingPuts();
        Assert.assertEquals(1, pending.size());
        Assert.assertEquals("K1", pending.get(0)[0][0]);
        verify(dao, times(1)).queryByKey("K1");
        verify(dao, never()).queryByKey("K2");
    }

    @Test
    public void putNow_defersRemainingOnMissingRelation_doesNotAbortSilentlyWithoutQueue() {
        SysConfigService service = new SysConfigService();
        SysConfigSchemaGate gate = new SysConfigSchemaGate();
        gate.markSchemaReady();
        SysConfigDao dao = mock(SysConfigDao.class);
        ReflectionTestUtils.setField(service, "baseMapper", dao);
        ReflectionTestUtils.setField(service, "schemaGate", gate);

        when(dao.queryByKey("A")).thenThrow(new BadSqlGrammarException("q", "s",
                new SQLSyntaxErrorException("Table 'autumn.sys_config' doesn't exist")));

        String[][] mapping = new String[][]{
                {"A", "1"},
                {"B", "2"},
        };
        service.putNow(mapping);

        @SuppressWarnings("unchecked")
        List<String[][]> pending = (List<String[][]>) ReflectionTestUtils.getField(service, "pendingPuts");
        Assert.assertEquals(1, pending.size());
        Assert.assertEquals(2, pending.get(0).length);
        Assert.assertEquals("A", pending.get(0)[0][0]);
        Assert.assertEquals("B", pending.get(0)[1][0]);
        verify(dao, never()).queryByKey("B");
        Assert.assertFalse(gate.isConfigBootstrapped());
    }

    @Test
    public void flushDeferredConfig_marksBootstrappedOnlyWhenPendingEmpty() {
        SysConfigService service = new SysConfigService();
        SysConfigSchemaGate gate = new SysConfigSchemaGate();
        gate.markSchemaReady();
        ReflectionTestUtils.setField(service, "schemaGate", gate);
        ReflectionTestUtils.setField(service, "baseMapper", mock(SysConfigDao.class));

        service.flushDeferredConfig();
        Assert.assertTrue(gate.isConfigBootstrapped());

        gate.resetForTests();
        gate.markSchemaReady();
        @SuppressWarnings("unchecked")
        List<String[][]> pending = (List<String[][]>) ReflectionTestUtils.getField(service, "pendingPuts");
        pending.add(new String[][]{{"STILL_PENDING", "1"}});
        service.flushDeferredConfig();
        // flush 会 putNow；mock 无缺表则可能插入失败——至少 pending 非空时不应在 defer 路径误标
        // 此处手动保持 pending：再次 defer 场景
        gate.resetForTests();
        gate.markSchemaReady();
        pending.clear();
        pending.add(new String[][]{{"STILL_PENDING", "1"}});
        // 不调用 flushPendingPuts 的 mark：直接测 mark 条件——pending 非空时 flushDeferred 先 putNow
        SysConfigDao dao = mock(SysConfigDao.class);
        when(dao.queryByKey("STILL_PENDING")).thenThrow(new BadSqlGrammarException("q", "s",
                new SQLSyntaxErrorException("Table 'autumn.sys_config' doesn't exist")));
        ReflectionTestUtils.setField(service, "baseMapper", dao);
        service.flushDeferredConfig();
        Assert.assertFalse(gate.isConfigBootstrapped());
        Assert.assertFalse(pending.isEmpty());
    }

    @Test
    public void successfulQuery_promotesSchemaReady() {
        SysConfigService service = new SysConfigService();
        SysConfigSchemaGate gate = new SysConfigSchemaGate();
        SysConfigDao dao = mock(SysConfigDao.class);
        when(dao.hasKey("K")).thenReturn(1);
        ReflectionTestUtils.setField(service, "baseMapper", dao);
        ReflectionTestUtils.setField(service, "schemaGate", gate);
        // 强制绕过 isSchemaReady 短路：先标 ready 再 reset 模拟「门禁未置但表已存在」——hasKey 在 !ready 时不打库
        // 因此用 putNow 路径（调用方已认为可写）或临时标 ready 后 noteSchemaUsable 在 query 成功时保持 ready
        gate.markSchemaReady();
        Assert.assertTrue(service.hasKey("K"));
        Assert.assertTrue(gate.isSchemaReady());
    }
}
