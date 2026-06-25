package cn.org.autumn.utils;

import cn.org.autumn.exception.AException;
import cn.org.autumn.model.Error;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.Assert;
import org.junit.Test;

public class ExceptionUtilsReadOnlyTest {

    @Test
    public void detectsWrappedDatabaseReadOnlyException() {
        AException root = new AException(Error.DATABASE_READ_ONLY);
        PersistenceException wrapped = new PersistenceException("write blocked", root);
        Assert.assertTrue(ExceptionUtils.isDatabaseReadOnlyException(wrapped));
        Assert.assertSame(root, ExceptionUtils.findAException(wrapped));
    }

    @Test
    public void ignoresNonReadOnlyException() {
        Assert.assertFalse(ExceptionUtils.isDatabaseReadOnlyException(new RuntimeException("other")));
    }
}
