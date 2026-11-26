package cn.org.autumn.table;

import org.junit.Assert;
import org.junit.Test;

public class SysConfigSchemaGateTest {

    @Test
    public void startsNotReady() {
        SysConfigSchemaGate gate = new SysConfigSchemaGate();
        Assert.assertFalse(gate.isSchemaReady());
        Assert.assertFalse(gate.isConfigBootstrapped());
    }

    @Test
    public void markSchemaThenBootstrap() {
        SysConfigSchemaGate gate = new SysConfigSchemaGate();
        gate.markSchemaReady();
        Assert.assertTrue(gate.isSchemaReady());
        Assert.assertFalse(gate.isConfigBootstrapped());
        gate.markConfigBootstrapped();
        Assert.assertTrue(gate.isConfigBootstrapped());
    }

    @Test
    public void resetForTestsClearsFlags() {
        SysConfigSchemaGate gate = new SysConfigSchemaGate();
        gate.markSchemaReady();
        gate.markConfigBootstrapped();
        gate.resetForTests();
        Assert.assertFalse(gate.isSchemaReady());
        Assert.assertFalse(gate.isConfigBootstrapped());
    }
}
