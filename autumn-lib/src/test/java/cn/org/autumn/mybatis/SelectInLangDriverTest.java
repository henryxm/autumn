package cn.org.autumn.mybatis;

import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void escapeXmlTextPreservesCdataSection() {
        String raw = "and <![CDATA[date_add(create_time, interval + #{overtime} minute) < now()]]>";
        String escaped = SelectInLangDriver.escapeXmlText(raw);
        assertTrue(escaped.contains("<![CDATA["));
        assertTrue(escaped.contains("]]>"));
        assertTrue(escaped.contains("minute) < now()"));
        assertFalse(escaped.contains("&lt;![CDATA["));
        assertFalse(escaped.contains("minute) &lt; now()"));
    }

    @Test
    void escapeXmlTextEscapesOutsideCdataOnly() {
        String raw = "a < b <![CDATA[x < y]]> c > d";
        String escaped = SelectInLangDriver.escapeXmlText(raw);
        assertEquals("a &lt; b <![CDATA[x < y]]> c &gt; d", escaped);
    }

    @Test
    void escapeXmlTextUnclosedCdataFallsBackToCharEscape() {
        String raw = "prefix <![CDATA[no close";
        String escaped = SelectInLangDriver.escapeXmlText(raw);
        assertTrue(escaped.startsWith("prefix &lt;![CDATA["));
        assertFalse(escaped.contains("<![CDATA["));
    }

    @Test
    void escapeXmlTextPreservesMybatisTagsAndEscapesComparatorsBetween() {
        String raw = "<if test=\"x != null\"> AND day <= #{day} </if> id IN (#{ids})";
        String escaped = SelectInLangDriver.escapeXmlText(raw);
        assertTrue(escaped.startsWith("<if test=\"x != null\">"));
        assertTrue(escaped.contains("</if>"));
        assertTrue(escaped.contains("day &lt;= #{day}"));
        assertTrue(escaped.contains("IN (#{ids})"));
        assertFalse(escaped.contains("&lt;if"));
    }

    @Test
    void escapeXmlTextPreservesTagAttributeGreaterThanInQuotes() {
        String raw = "<if test=\"a > b\">ok</if>";
        String escaped = SelectInLangDriver.escapeXmlText(raw);
        assertEquals("<if test=\"a > b\">ok</if>", escaped);
    }

    @Test
    void escapeXmlTextPreservesXmlComment() {
        String raw = "<!-- skip --> day < #{d}";
        assertEquals("<!-- skip --> day &lt; #{d}", SelectInLangDriver.escapeXmlText(raw));
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
    void createSqlSourceAcceptsHistoricalCdataWithInClause() {
        SelectInLangDriver driver = new SelectInLangDriver();
        Configuration configuration = new Configuration();
        String sql = "select * from user_finance_flow where status IN (#{statuses})"
                + " and <![CDATA[date_add(create_time, interval + #{overtime} minute) < now()]]>";
        SqlSource source = assertDoesNotThrow(() -> driver.createSqlSource(configuration, sql, Object.class));
        assertNotNull(source);

        Map<String, Object> param = new HashMap<>();
        param.put("statuses", new String[]{"PayRequested"});
        param.put("overtime", 60);
        BoundSql bound = source.getBoundSql(param);
        String boundSql = bound.getSql();
        assertFalse(boundSql.contains("<![CDATA["), boundSql);
        assertFalse(boundSql.contains("]]>"), boundSql);
        assertTrue(boundSql.contains("date_add(create_time, interval + ? minute) < now()"), boundSql);
        assertTrue(boundSql.toLowerCase().contains(" in "), boundSql);
    }

    @Test
    void createSqlSourceAcceptsIfTagWithBareLessEqualAndIn() {
        SelectInLangDriver driver = new SelectInLangDriver();
        Configuration configuration = new Configuration();
        String sql = "SELECT * FROM t WHERE 1=1"
                + " <if test=\"dayTo != null\"> AND day <= #{dayTo} </if>"
                + " AND id IN (#{ids})";
        SqlSource source = assertDoesNotThrow(() -> driver.createSqlSource(configuration, sql, Object.class));
        assertNotNull(source);
        Map<String, Object> param = new HashMap<>();
        param.put("dayTo", 20260101);
        param.put("ids", new String[]{"a", "b"});
        BoundSql bound = source.getBoundSql(param);
        assertFalse(bound.getSql().contains("&lt;="), bound.getSql());
        assertTrue(bound.getSql().contains("day <= ?") || bound.getSql().contains("day <= #{dayTo}"),
                bound.getSql());
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

    @Test
    void isXmlElementTagStartDetectsOpenAndClose() {
        assertTrue(SelectInLangDriver.isXmlElementTagStart("<if test='x'>", 0));
        assertTrue(SelectInLangDriver.isXmlElementTagStart("</foreach>", 0));
        assertFalse(SelectInLangDriver.isXmlElementTagStart("<![CDATA[x]]>", 0));
        assertFalse(SelectInLangDriver.isXmlElementTagStart("<!-- c -->", 0));
        assertFalse(SelectInLangDriver.isXmlElementTagStart("a < b", 2));
    }
}
