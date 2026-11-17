package cn.org.autumn.modules.sys.service;

import cn.org.autumn.modules.sys.dao.SysConfigDao;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLSyntaxErrorException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
        SysConfigDao dao = mock(SysConfigDao.class);
        SQLSyntaxErrorException sql = new SQLSyntaxErrorException("Table 'signdb.sys_config' doesn't exist");
        when(dao.hasKey(anyString())).thenThrow(new BadSqlGrammarException("query", "SELECT COUNT(*)", sql));
        ReflectionTestUtils.setField(service, "baseMapper", dao);

        Assert.assertFalse(service.hasKey("OBJECT_STORAGE_APP_CONFIG"));
    }
}
