package cn.org.autumn.modules.db.service;

import org.junit.Assert;
import org.junit.Test;

public class DatabaseBackupExecutionGuardTest {

    @Test
    public void matchesHostPattern_exact() {
        Assert.assertTrue(DatabaseBackupExecutionGuard.matchesHostPattern("app-1", "app-1"));
        Assert.assertTrue(DatabaseBackupExecutionGuard.matchesHostPattern("App-1", "app-1"));
        Assert.assertFalse(DatabaseBackupExecutionGuard.matchesHostPattern("app-2", "app-1"));
    }

    @Test
    public void matchesHostPattern_prefixSuffixContains() {
        Assert.assertTrue(DatabaseBackupExecutionGuard.matchesHostPattern("web-prod-1", "web-*"));
        Assert.assertTrue(DatabaseBackupExecutionGuard.matchesHostPattern("k8s-worker-b", "*-b"));
        Assert.assertTrue(DatabaseBackupExecutionGuard.matchesHostPattern("foo-bar-baz", "*bar*"));
        Assert.assertFalse(DatabaseBackupExecutionGuard.matchesHostPattern("other", "web-*"));
    }
}
