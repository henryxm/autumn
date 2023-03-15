package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;

import java.io.Serializable;

@ConfigParam(paramKey = "SYSTEM_UPGRADE", category = SystemUpgrade.config, name = "系统升级", description = "当系统需要升级时开启和关闭相关功能，保护数据单独一致性")
public class SystemUpgrade implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String config = "systemupgrade_config";
    //用户级写入禁止，系统升级过程中，禁止用户新增或更新数据，系统级写入仍然可进行
    @ConfigField(category = InputType.BooleanType, name = "本地数据写入", description = "允许用户上传和更新本地数据，升级时关闭开关，转移数据，升级成功后开启")
    private boolean localWrite = true;
    //系统级写入禁止，升级过程中，禁止任何吸入操作
    @ConfigField(category = InputType.BooleanType, name = "数据库写入更新", description = "修改和更新数据库，系统处于读写状态，关闭不允许写入更新")
    private boolean databaseWrite = true;
    //升级说明
    @ConfigField(category = InputType.StringType, name = "升级过程说明", description = "系统升级的提示说明")
    private String description = "系统升级，功能暂停使用，请稍后重试";

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
