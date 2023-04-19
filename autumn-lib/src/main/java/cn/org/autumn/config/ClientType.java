package cn.org.autumn.config;

public enum ClientType {
    //系统默认值，在升级或启动时，对应回调地址将动态被修改
    SiteDefault,
    //手动创建的，在系统升级时，不改变，用户需要自行更改
    ManualCreate,
    //用户创建Key时生成的类型，一般不需要更改
    AccessKey,
}