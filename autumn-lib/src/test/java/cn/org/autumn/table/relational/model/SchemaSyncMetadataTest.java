package cn.org.autumn.table.relational.model;

import cn.org.autumn.table.data.IndexInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SchemaSyncMetadataTest {

    @Test
    public void groupsByTableIgnoreCaseAndSupportsRemove() {
        ColumnMeta c1 = new ColumnMeta();
        c1.setTableName("Sys_User");
        c1.setColumnName("id");
        ColumnMeta c2 = new ColumnMeta();
        c2.setTableName("sys_user");
        c2.setColumnName("uuid");

        IndexInfo idx = new IndexInfo();
        idx.setTable("SYS_USER");
        idx.setKeyName("uuid");
        idx.setColumnName("uuid");

        TableCharsetMeta cs = new TableCharsetMeta();
        cs.setTableName("sys_user");
        cs.setCharacterSetName("utf8mb4");

        SchemaSyncMetadata meta = SchemaSyncMetadata.from(
                Arrays.asList(c1, c2),
                Collections.singletonList(idx),
                Collections.singletonList(cs));

        Assert.assertTrue(meta.hasTable("SYS_USER"));
        Assert.assertEquals(2, meta.getColumns("sys_user").size());
        Assert.assertEquals(1, meta.getIndexRows("Sys_User").size());
        Assert.assertEquals("utf8mb4", meta.getCharacterSetName("sys_user"));

        meta.removeTable("sys_user");
        Assert.assertFalse(meta.hasTable("sys_user"));
        Assert.assertTrue(meta.getColumns("sys_user").isEmpty());
    }
}
