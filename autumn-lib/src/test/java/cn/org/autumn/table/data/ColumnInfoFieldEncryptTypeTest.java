package cn.org.autumn.table.data;

import cn.org.autumn.annotation.FieldEncrypt;
import cn.org.autumn.table.annotation.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @FieldEncrypt 不得把显式 TEXT 压成 varchar(1024)（SysConfig.paramValue 等）。
 */
class ColumnInfoFieldEncryptTypeTest {

    static class Sample {
        @FieldEncrypt
        @Column(type = DataType.TEXT, comment = "value")
        String paramValue;

        @FieldEncrypt
        @Column(comment = "token")
        String token;
    }

    @Test
    void fieldEncrypt_keepsExplicitText() throws Exception {
        Field f = Sample.class.getDeclaredField("paramValue");
        ColumnInfo info = new ColumnInfo(f);
        assertEquals(DataType.TEXT, info.getType());
    }

    @Test
    void fieldEncrypt_defaultStringBecomesVarchar1024() throws Exception {
        Field f = Sample.class.getDeclaredField("token");
        ColumnInfo info = new ColumnInfo(f);
        assertEquals(DataType.VARCHAR, info.getType());
        assertEquals(1024, info.getLength());
    }

    @Test
    void isLobTextType_recognizesTextFamily() {
        assertTrue(ColumnInfo.isLobTextType("text"));
        assertTrue(ColumnInfo.isLobTextType("MEDIUMTEXT"));
        assertTrue(ColumnInfo.isLobTextType("longtext"));
    }
}
