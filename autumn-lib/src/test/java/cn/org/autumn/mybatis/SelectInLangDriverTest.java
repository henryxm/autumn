package cn.org.autumn.mybatis;

import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectInLangDriverTest {

    @Test
    void escapeXmlTextEscapesRawComparators() {
        assertEquals("a &amp; b &lt;= c &gt;= d", SelectInLangDriver.escapeXmlText("a & b <= c >= d"));
        assertEquals("x &lt;&gt; y", SelectInLangDriver.escapeXmlText("x <> y"));
    }

    @Test
    void escapeXmlTextPreservesExistingEntities() {
        // 历史手写 &lt;= 不得变成 &amp;lt;=
        assertEquals("day &lt;= #{to} AND day &gt;= #{from}",
                SelectInLangDriver.escapeXmlText("day &lt;= #{to} AND day &gt;= #{from}"));
        assertEquals("a &amp; b", SelectInLangDriver.escapeXmlText("a &amp; b"));
        assertEquals("n &#60; 1", SelectInLangDriver.escapeXmlText("n &#60; 1"));
    }

    @Test
    void createSqlSourceAcceptsLessEqualWithInClause() {
        SelectInLangDriver driver = new SelectInLangDriver();
        Configuration configuration = new Configuration();
        String sql = "SELECT id FROM t WHERE id IN (#{ids}) AND day <= #{dayTo} AND day >= #{dayFrom}";
        SqlSource source = assertDoesNotThrow(() -> driver.createSqlSource(configuration, sql, Object.class));
        assertNotNull(source);
    }

    @Test
    void createSqlSourceAcceptsPreEscapedLessEqual() {
        SelectInLangDriver driver = new SelectInLangDriver();
        Configuration configuration = new Configuration();
        String sql = "SELECT id FROM t WHERE id IN (#{ids}) AND day &lt;= #{dayTo}";
        SqlSource source = assertDoesNotThrow(() -> driver.createSqlSource(configuration, sql, Object.class));
        assertNotNull(source);
    }

    @Test
    void inPlaceholderSurvivesEscape() {
        String escaped = SelectInLangDriver.escapeXmlText("WHERE x IN (#{uuids}) AND y <= 1");
        assertTrue(escaped.contains("IN (#{uuids})"));
        assertTrue(escaped.contains("&lt;="));
    }

    @Test
    void xmlEntityEndDetectsNamedAndNumeric() {
        assertEquals(4, SelectInLangDriver.xmlEntityEndExclusive("&lt;", 0));
        assertEquals(5, SelectInLangDriver.xmlEntityEndExclusive("&amp;", 0));
        assertEquals(5, SelectInLangDriver.xmlEntityEndExclusive("&#60;", 0));
        assertEquals(-1, SelectInLangDriver.xmlEntityEndExclusive("& alone", 0));
    }
}
