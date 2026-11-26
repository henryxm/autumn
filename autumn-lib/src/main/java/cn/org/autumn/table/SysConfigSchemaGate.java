package cn.org.autumn.table;

import org.springframework.stereotype.Component;

/**
 * 空库首启门禁：注解建表完成前禁止对 {@code sys_config} 的权威 JDBC 读写；
 * SysConfig 基础种子 put 完成后标记 {@link #configBootstrapped}。
 */
@Component
public class SysConfigSchemaGate {

    private volatile boolean schemaReady;
    private volatile boolean configBootstrapped;

    public boolean isSchemaReady() {
        return schemaReady;
    }

    public boolean isConfigBootstrapped() {
        return configBootstrapped;
    }

    /** 表结构已可访问（注解建表完成，或非注解同步库假定外部 DDL 已就绪）。 */
    public void markSchemaReady() {
        schemaReady = true;
    }

    /** SysConfig 基础种子 put 已完成。 */
    public void markConfigBootstrapped() {
        configBootstrapped = true;
    }

    /** 单测 / 重装重置。 */
    public void resetForTests() {
        schemaReady = false;
        configBootstrapped = false;
    }
}
