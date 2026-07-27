package cn.org.autumn.modules.sys.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLSyntaxErrorException;

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
}
