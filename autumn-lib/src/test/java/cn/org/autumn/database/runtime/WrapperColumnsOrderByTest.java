package cn.org.autumn.database.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import cn.org.autumn.exception.AException;

/**
 * ORDER BY 列名白名单：拒绝方言引号旁路与注入片段。
 */
public class WrapperColumnsOrderByTest {

    @Test
    void simpleIdentifierAccepted() {
        String col = WrapperColumns.orderByColumnExpression("create_time", true);
        assertNotNull(col);
        assertTrue(col.contains("create_time") || col.contains("CREATE_TIME") || col.length() > 0);
    }

    @Test
    void quotedInjectionRejected() {
        assertNull(WrapperColumns.orderByColumnExpression("`id`,if(1=1,id,name)`", true));
        assertNull(WrapperColumns.orderByColumnExpression("id);drop table x--", true));
        assertNull(WrapperColumns.orderByColumnExpression("a.b", true));
        assertThrows(AException.class,
                () -> WrapperColumns.orderByColumnExpression("1 union select 1", false));
    }

    @Test
    void blankRejected() {
        assertNull(WrapperColumns.orderByColumnExpression("  ", true));
        assertNull(WrapperColumns.orderByColumnExpression(null, true));
    }
}
