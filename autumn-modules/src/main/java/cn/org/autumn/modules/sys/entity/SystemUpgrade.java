package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;

import java.io.Serializable;

@ConfigParam(paramKey = "SYSTEM_UPGRADE", category = SystemUpgrade.config, name = "系统升级", description = "系统升级")
public class SystemUpgrade implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String config = "systemupgrade_config";
    //用户级写入禁止，系统升级过程中，禁止用户新增或更新数据，系统级写入仍然可进行
    @ConfigField(category = InputType.BooleanType, name = "禁止本地数据操作", description = "禁止用户上传和更新本地数据")
    boolean localWrite = true;
    //系统级写入禁止，升级过程中，禁止任何吸入操作
    @ConfigField(category = InputType.BooleanType, name = "禁止数据库操作", description = "禁止禁止修改和更新数据库，系统处于只读状态")
    boolean databaseWrite = true;
    //升级说明
    @ConfigField(category = InputType.StringType, name = "升级过程说明", description = "系统升级的提示说明")
    String description = "";

    public boolean isLocalWrite() {
        return localWrite;
    }

    public void setLocalWrite(boolean localWrite) {
        this.localWrite = localWrite;
    }

    public boolean isDatabaseWrite() {
        return databaseWrite;
    }

    public void setDatabaseWrite(boolean databaseWrite) {
        this.databaseWrite = databaseWrite;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
