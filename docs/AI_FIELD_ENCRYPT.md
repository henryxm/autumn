# Autumn 字段存储加密（按需加载）

> 用途：实体 `@FieldEncrypt` 字段落库加密、读库解密；与传输层 `docs/AI_CRYPTO.md` **完全独立**（不同密钥、不同算法用途、不同 Service）。  
> **应用层硬约束摘要**：见 **`docs/AI_STANDARDS.md` §10.5**（`EncryptModuleService`、`searchable` 必有 hash 列、禁 lambdaUpdate 直写）。

---

## 0. 先读：最容易混淆的概念

### 0.1 三个层次不要混

| 层次 | 控制什么 | 典型配置/代码 | 会不会改表结构 |
|------|----------|---------------|----------------|
| **实体注解** | 哪些字段要加密、是否走盲索引 | `@FieldEncrypt`、`searchable`、`mobileHash` 字段 | 仅当你**在实体类里增删 `@Column` 字段**时，由表结构同步决定 |
| **写入加密开关** | 新写入是否加密、是否维护 hash | `enabled`、后台运行时开关 | **否** |
| **Service 继承** | 哪条业务链路上下加密 | `ModuleService` vs `EncryptModuleService` | **否** |

后台切换「写入加密」**不会**自动增加/删除 `mobile_hash` 列；改 `searchable=true/false` 也**不会**自动 DDL。

### 0.2 `vector`（IV）vs `hashField`（盲索引列）——各干各的

| | **vector（IV）** | **searchable + hashField** |
|---|------------------|----------------------------|
| **作用** | AES-GCM 加密时的初始化向量 | 等值查询用的 **HMAC 盲索引** |
| **存在哪** | 打包在 `ENC$v1$...` 密文字符串的前 12 字节里 | **单独一列**（如 `mobile_hash`） |
| **解密要不要** | 从密文里拆出 IV，**不看注解** | **不参与解密** |
| **同明文多次写入** | 默认随机 IV → 密文不同 | hash 相同（确定性） |
| **能否代替对方** | 不能；固定 IV 也不应替代 hash 列 | 不能；hash 不可逆，不能解密 |

**随机 vector 如何解密？** 加密时 IV 与密文拼在一起 Base64 落库；读库时从该字符串还原 IV，再用 `key` 解密。不是「解密时再猜随机数」。

```
写入: 明文 → [随机IV + GCM密文] → "ENC$v1$Base64(...)"  → mobile 列
      明文 → HMAC(明文, hash-key) → mobile_hash 列（仅 searchable 且实体声明了 hash 字段）

读取: mobile 列 → 拆 IV → 解密 → 明文
      列表按手机号查 → WHERE mobile_hash = HMAC(查询值)（写入开关开时）
```

### 0.3 `searchable` 与 `hashField` 不会「自动建列」

```java
@FieldEncrypt(searchable = true)  // 写入时维护 hash；列表条件改走 hash 列
private String mobile;

@Column(length = 64, comment = "手机号哈希:盲索引")  // 必须手写；框架不会自动生成
private String mobileHash;
```

- `hashField = "mobileHash"`：仅指定 **Java 字段名**（默认 `{fieldName}Hash`），**不是**自动 DDL。
- 若 `searchable=true` 但实体缺少对应字段：启动 WARN，**不写 hash、不能盲索引查**。

### 0.4 写入开关行为

| 状态 | 加密列 `mobile` | 盲索引列 `mobile_hash` | 列表按手机号查 |
|------|-----------------|--------------------------|----------------|
| 写入加密 **开** + `searchable=true` | 写密文 | 写 HMAC | `getCondition` → hash 列（`EncryptModuleService#tryHashQueryCondition`） |
| 写入加密 **关**（运行时） | 写明文 | 不再维护 hash | 回退查 **明文列** `mobile` |
| 注解 `searchable=false` | 按 key 加解密 | 不参与 | 查明文列 |

### 0.5 加解密路径（唯一）

| 调用方式 | 写 | 读 |
|----------|----|----|
| **`EncryptModuleService`** 的 `insert*` / `update*` / `select*` / `getById` / `list` / `page` 等 | 自动 `onWrite` + `restoreAfterWrite` | 自动 `onRead` |
| **读链** `lambdaQuery()` / `lambdaQuery(entity)` / `query()` / `ktQuery()` 的终端方法 `list` / `one` / `oneOpt` / `page` | — | 自动 `onRead`（`count` / `exists` 不解密） |
| **`baseMapper` / Dao 手写 SQL** | 须手动 `encrypt.onWrite`（或改走 Service 写方法） | 返回实体须 **`afterRead(...)`**；返回 `Map`/标量须 **`afterReadMap` / `afterReadMaps` / `afterReadScalar(s)`** |
| **`ModuleService` 子类** | 无加解密 | 无自动解密 |

**读链边界（刻意不覆盖）：**

- **`lambdaUpdate()` 写链**：`set(column, 明文)` 不会自动加密；带实体的 `update(entity)` 不会走 `onWrite`。加密字段请用 `save*` / `updateById` 等 Service 写方法。
- **链式 `eq` 查 searchable 字段**：不会自动改 hash 列（与 `baseMapper.selectList(wrapper)` 手写明文条件同类问题）；列表页等值条件走 `BaseService#getCondition` → `tryHashQueryCondition`。
- **`query().lambda()`**：已返回带解密的 lambda 链。
- **`ChainWrappers.*` 直接 new / Kotlin 顶层扩展**：绕过 Service，须改走读链或 `list(Wrapper)` + `afterRead`。

列表/分页 **searchable** 等值条件：`BaseService#getCondition` → `EncryptModuleService#tryHashQueryCondition`（改 hash 列）。

约束单测：`FieldEncryptConventionTest`（存在 `@FieldEncrypt` 实体时，其 Service 须 extends `EncryptModuleService`；业务仓库可加 JVM 参数 `-Dautumn.fieldEncrypt.convention.packages=cn.example.modules` 扩展扫描包）。

迁移读库内密文原文：`FieldEncryptContext.runSkip(...)` 包裹 Mapper 查询（`FieldEncryptMigrationService`）。

---

## 1. 配置

```yaml
autumn:
  crypto:
    field:
      enabled: true
      key: ${AUTUMN_FIELD_ENCRYPT_KEY:}   # Base64，32 字节 AES-256
      hash-key: ${AUTUMN_FIELD_HASH_KEY:} # 盲索引 HMAC，默认同 key；建议分开
      prefix: "ENC$v1$"
```

docker-compose / 环境变量（由 `FieldEncryptConfigSource` 统一解析；**显式短前缀优先**）：

| 优先级 | 写法 | 示例 |
|--------|------|------|
| 1 | 短前缀（与 bighub 一致） | `crypto.field.enabled=true`、`crypto.field.key=...`、`crypto.field.hash-key=...` |
| 2 | Spring 松散绑定 | `AUTUMN_CRYPTO_FIELD_ENABLED` / `AUTUMN_CRYPTO_FIELD_KEY` / `AUTUMN_CRYPTO_FIELD_HASH_KEY` |
| 3 | 文档历史名 | `AUTUMN_FIELD_ENCRYPT_ENABLED`、`AUTUMN_FIELD_ENCRYPT_KEY`、`AUTUMN_FIELD_HASH_KEY` |

- `enabled=true` 且密钥无效 → 启动失败。
- `enabled=false` → 默认不写加密；配置了 `key` 仍可解密带前缀密文。
- 密钥通过 yml/环境变量注入；后台不可修改运行中密钥。

### 1.1 开关与密钥

| 项 | 含义 |
|----|------|
| `enabled` | 启动默认：是否对新写入加密 |
| 运行时写入开关 | 后台覆盖 `enabled`；单机 `sys_config`，集群 Redis |
| `key` | AES 加解密 |
| `hash-key` | 仅 `searchable` 盲索引；与 `vector` 无关 |

### 1.2 单机与集群（Redis）

| 模式 | 写入开关 | 密钥 |
|------|----------|------|
| 单机 | `sys_config.field_encrypt_runtime_write_enabled` | yml/env |
| 集群 | Redis `{ns}:autumn:field-encrypt:write-enabled` | Redis；启动用 env 对齐密钥 |

**启动对齐（写开关保留一键开启）**

| 情况 | 行为 |
|------|------|
| 存储无写开关 | 用 env/yml `enabled` 种子 |
| env `enabled=true` | **强制**打开存储中的写开关（升级推高） |
| env `enabled=false` 且存储已为 `true` | **不覆盖**（保留 fieldencrypt 一键开启，滚动重启不关写） |
| 合法 key/hash-key | 仍从 env 对齐到 Redis / 本机 |

关写请用后台「写入加密」关闭或「恢复默认」，不要指望把 yml 改回 `enabled=false` 来关掉已一键开启的集群开关。

### 1.3 多节点滚动启用（业务零改动）

业务继续 `SysConfigService.getValue` / `getConfigObject` / `updateValueByKey` / `save` / `update`，无需改业务仓代码。

1. 全节点部署新 Autumn，配置**同一** `crypto.field.key` / `hash-key`，保持 **`crypto.field.enabled=false`**（默认不写加密）。
2. 确认各节点 `readDecryptEnabled=true`、`writeEncryptEnabled=false`（`fieldencrypt.html` 状态）。
3. `fieldencrypt.html`：对各 `@FieldEncrypt` 实体预检 migrate → 确认执行；SysConfig 也可用 `configsecure.html`。
4. 打开运行时「写入加密」（集群 Pub/Sub 同步其它节点）。
5. 抽检：`getValue` 仍为明文；DB `param_value` 为 `ENC$v1$…`。
6. 滚动重启后写开关仍为 true（§1.2）。迁移非 dryRun 后框架会 `SysConfigService.reloadAfterFieldEncryptMigration()` 清 Redis/本机 map 并广播失效。

---

## 2. 实体与 Service

### 2.1 仅需保密、不需按密文字段等值查

```java
@Column(length = 128)
@FieldEncrypt   // 默认 vector 随机；不声明 searchable
private String token;
```

### 2.2 需要列表/条件按明文等值查

```java
@Column(comment = "手机号", length = 512)
@FieldEncrypt(searchable = true)
private String mobile;

@Column(comment = "手机号哈希:盲索引", length = 64)
private String mobileHash;
```

- 仅 **`String`** 字段；`searchable` 须 **手写** hash 列（见 §0.3）。
- **不要**用固定 `vector` 代替 `searchable`（见 §0.2）。

### 2.3 `vector` 选用

| | 默认（空 vector） | 固定 `vector="Base64..."` |
|---|-------------------|---------------------------|
| 开发 | **推荐** | 可用来产生可重复密文 |
| 同明文密文 | 每次不同 | 每次相同 |
| 等值查询 | 配合 `searchable` + hash 列 | **不能**替代 hash 列 |

### 2.4 Service 层（EncryptModuleService）

```java
public class XxxService extends EncryptModuleService<XxxDao, XxxEntity> {
    // 写：insert* / save* / update* → onWrite → 落库 → restoreAfterWrite（业务侧保持明文）
    // 读：select* / getById / list / page → onRead
    // 读链：lambdaQuery() / lambdaQuery(entity) / query() / ktQuery()
    //       → list / one / oneOpt / page 终端方法同样 onRead
}

// 读链示例（Java）
XxxEntity row = lambdaQuery().eq(XxxEntity::getUuid, uuid).one();

// baseMapper / Dao 手写 SQL（须手动解密）
SomeEntity row = baseMapper.getByUuid(uuid);
afterRead(row);
```

**读链未覆盖（见 §0.5）**：`lambdaUpdate` / `update()` 写链、`Wrapper.eq` 查 searchable 明文、`baseMapper` 直查。

### 2.5 SysConfig `paramValue`（全字段落库加密）

| 项 | 约定 |
|----|------|
| 实体 | `SysConfigEntity.paramValue`：`@FieldEncrypt` + `TEXT`（容纳 `ENC$v1$` 密文；`ColumnInfo` 对显式 TEXT **不会**压成 `varchar(1024)`） |
| Service | `SysConfigService` **必须** `extends EncryptModuleService`；`getMenuItems` / `getMenuItemsInternal` 置空（菜单由 `SysMenuService` 维护） |
| 旁路 | **对外读须明文**：`EncryptModuleService.afterRead` + SysConfig `ensurePlainParamValue` 对残留 `ENC$` 再 `decryptValue`；`getValue`/`getByKey`/`getNameSpace`/`getById`/`list`/`page`/`getObject` 全覆盖；`getObject` 解析失败回落默认对象；无密钥时密文无法解密（须配 key）；`FieldEncryptRuntimeService` `@Order(-1500)` 早于 SysConfig init；`updateValueByKey`/`save`/`update` 写路径受写开关控制；Redis/map **缓存明文** |

### 2.5.1 对外读路径是否穷尽？

| 结论 | 说明 |
|------|------|
| **框架 Service CRUD + SysConfig 字符串 API：是（有密钥时）** | 凡继承 `EncryptModuleService` 的 `getById/list/page/select*/读链`，以及 `SysConfigService.str/getValue/json/getObject/getByKey`，读出后统一解密；业务仓零改代码即可拿明文 |
| **未覆盖 / 须人工** | ① `baseMapper`/Dao 直查且未调 `afterRead`；② `ChainWrappers` 绕过 Service；③ 密钥未配置或错误（`decryptValue` 原样返回 `ENC$`）；④ 运维 `runSkip` / `isParamValueEncryptedAtRest`（故意读密文） |
| **当前仓库 `@FieldEncrypt` 实体** | 仅 `SysConfigEntity.paramValue`、`ConnectAppEntity.appSecret`（均已 `EncryptModuleService` + 约定测试） |
| 运维页 | `configsecure.html` + `/sys/configsecure/*`（超管 + IP 白名单）；明文列表/编辑，密文落库；存量迁移复用 `FieldEncryptMigrationService`（实体全名 `cn.org.autumn.modules.sys.entity.SysConfigEntity`）；非 dryRun 后 `reloadAfterFieldEncryptMigration` |
| 通用字段加密页 | `fieldencrypt.html`；迁移 SysConfig 实体时同样刷新配置集群缓存 |

---

## 3. 关键类

| 类 | 职责 |
|----|------|
| `FieldEncrypt` | 字段注解（见 §0） |
| `FieldEncryptService` | `onWrite` / `onRead`、盲索引、`useHashForQuery` |
| `FieldEncrypt*ChainWrapper` | 读链装饰（`cn.org.autumn.crypto`）；由 `EncryptModuleService` 组装 |
| `EncryptModuleService` | 带 `@FieldEncrypt` 实体的 Module Service 基类 |
| `FieldEncryptRuntimeService` | 运行时开关与密钥对齐；实现 InitFactory.Init/After（`@Order(-1500)`） |
| `FieldEncryptAdminController` | 管理 API：`/sys/crypto/field/*` |

---

## 3.1 开发态（IDEA Debug）密钥从哪来？

**没有「开发模式免密钥自动加解密」。** 读解密只认内存里是否已有有效 AES key（`isKeyConfigured()`），与 `Config.isDev()` 无关。

| 阶段 | 密钥来源 | 典型现象 |
|------|----------|----------|
| `FieldEncryptRuntimeService` `@PostConstruct`（`ShiroConfig @DependsOn`） | yml/env；若无则 **早期** 用 `autumn.node.namespace`/`default` 读 Redis `{ns}:autumn:field-encrypt:key` | 有 Redis 历史密钥时可在 Shiro Bean 创建前解密 SysConfig |
| `InitFactory` → `FieldEncryptRuntimeService.init`（`@Order(-1500)`） | 完整对齐 Redis/sys_config 写开关 + 密钥 | 启动完成后管理页「密钥就绪=是」 |
| 管理页 `generateKey` / 一键开写 | 写入 Redis | 即使 yml 未配 key，下次启动仍可从 Redis 加载 |

IDEA「没配 yml key 却能加解密」= Redis/env 已有历史密钥。**推荐**：`application-dev.yml` 固定同一套 `crypto.field.key`/`hash-key`，且 **`enabled=false`**（仅解密；连共享 Redis 时勿 `enabled=true` 以免推高写开关）。

## 3.2 启动/业务读到密文：日志含义与处理

| 日志关键词 | 含义 | 业务侧怎么做 |
|------------|------|----------------|
| `值为密文但字段加密密钥未就绪` | 库中已是 `ENC$`，但 yml/env 无 key，且早期 Redis 也未读到；`getValue`/`getNameSpace` **不返回、不缓存**密文（回落 env/默认） | ① 在 yml/env 配置**加密时使用的同一** key（开发：`enabled=false` 仅配 key，勿对共享 Redis 强制 `enabled=true`）；② 检查 Redis 密码/连通与 `{ns}:autumn:field-encrypt:key`（已有 Redis key **不会**被不同 env key 覆盖）；③ 临时：configsecure 对该项「解密」落明文后重启 |
| InitFactory 反射 NPE / `not an instance of declaring class` | `obj.getClass().getMethod` 打到 CGLIB 代理壳 | `Factory.invoke` 只用接口 Method；读 ENC$ 的 Shiro Bean `@DependsOn(fieldEncryptRuntimeService)`；LBP 用 static `@Bean` |
| `密文解密失败` / `解密后仍为密文` | 有 key 但与密文不匹配或损坏 | fieldencrypt 页用密文测解密；换对 key；或 restore 明文后用正确 key 再 migrate |
| `JSON 解析失败` 且 preview=`ENC$…` | 旧版/未升级时 Gson 误解析密文 | 升级到含 §3.1 早期加载的框架；并按上表配 key |
| 启动后功能用了默认会话/安全配置 | 上述回落导致 Bean 用了 `new SecuritySessionConfig()` | 配好 key 后**重启**，使 Shiro 等重新读库明文 |

**业务仓纪律（避免 ENC$ 进业务逻辑）**

1. 只通过 `SysConfigService` 标准读 API / 继承 `EncryptModuleService` 的 Service 读加密字段；禁止 `baseMapper` 直出未 `afterRead`。
2. **`sys_config` 取值强制标准 API**（字段加密自动加解密）：
   - **推荐短名**（与长名等价，前向兼容）：`str` / `bool` / `num` / `json` / `jsonOrNull` / `has`。
   - **长名仍可用**：`getValue` / `getBoolean` / `getInt` / `getConfigObject` / `getConfigObjectValidate` / `hasKey`；写用 `updateValueByKey` / `save` / `update`。
   - **空配置要 null**：用 `jsonOrNull(key, Type.class)`（勿 `getValue` 后再 `getConfigObject`，避免双读与默认对象）。
   - **禁止**以 `getByKey(...).getParamValue()`、手写 `Gson.fromJson(entity.getParamValue(), …)`、Dao/Redis 直读 `param_value` 作为业务取值（解密失败时可能残留 `ENC$`，且无 `getValue` 的 null 兜底）。
   - `getByKey` 仅用于元数据（id / type / remark / options）；需要**值**时仍调 `str`/`getValue` / `json`/`getConfigObject*`。
   - 运维例外：`isParamValueEncryptedAtRest` / `FieldEncryptContext.runSkip` 仅用于看库内密文，业务逻辑勿模仿。
3. **读路径缓存语义**（热路径）：
   - 本机 map 命中且 `paramValue` **非** `ENC$` 前缀 → **跳过** `afterRead`，直接返回明文；Redis/DB 回源仍解密后再写入 Redis/map。
   - `getObject` / `json` 对本机按 `paramKey + Class` 缓存解析结果；`invalidateLocalCache` / `updateValueByKey` / 集群失效 / `onFiveMinute` 同步清理。
   - **勿原地 mutate** JSON 返回对象；改配置须 `updateValueByKey`（或需新实例时自行 `getValue`+`fromJson`）。
4. 启动 `@PostConstruct` / `@Bean` 若必须读 JSON SysConfig：确保节点 yml 带 key，或依赖框架早期 Redis 加载；不要假设「无 key 也能解密」。
5. 多节点：**同一** `crypto.field.key`；先迁密文再开写开关（§1.3）。
6. 遇到回落默认：先看 warn 表处理，再核对业务是否依赖了非默认的 `SECURITY_*` / 其它 JSON 配置。

---

## 4. 管理后台（开发）

页面 **`fieldencrypt.html`**，API 前缀 **`/sys/crypto/field`**（**系统管理员 + IP 白名单**；Cookie 会话 POST 须带 **`X-CSRF-Token`**）：

| 能力 | 说明 |
|------|------|
| `GET /status` | 当前开关、`writeReady`、密钥来源、集群模式；`?reload=true` 才从 Redis 重载 |
| `GET /list` | 已扫描到的 `@FieldEncrypt` 实体清单 |
| `POST /writeEnabled` / `resetWriteEnabled` | 运行时写入开关；写操作记 SecurityAlert 审计 |
| `POST /reloadCluster` | 从 Redis 刷新本机内存 |
| `POST /test` / `decrypt` | 内存加解密（运维调试；审计不落明文） |
| `POST /generateKey` / `generateVector` | 仅非生产；生成开发用材料后写入 yml/环境变量并重启 |
| `POST /migrate` / `restore` | 批量迁移；默认 `dryRun=true`；`batchSize` 上限 2000 |
| `POST /migrateOne` / `restoreOne` | 单条；默认预检；非预检须 `confirm=true` |

### 4.1 SysConfig 配置安全页

页面 **`configsecure.html`**，API 前缀 **`/sys/configsecure`**（同样 **系统管理员 + IP 白名单** + CSRF）：

| 能力 | 说明 |
|------|------|
| `GET /status` | 字段加密状态快照 + 目标实体/字段 |
| `GET /list` / `GET /info/{id}` | 明文列表与详情（读路径已解密）；列表行含 `atRestEncrypted` / `canEncrypt` / `canDecrypt`；详情另回 `atRestEncrypted` |
| `POST /save` / `POST /update` | 明文入参；有密钥时**强制** ENC$ 落库（不依赖全局写开关），缓存仍为明文 |
| `POST /migrateOne` / `restoreOne` | 单条加密/还原；`id` + 非预检须 `confirm=true`；成功后刷新配置缓存 |
| `POST /migrate` / `restore` | 仅针对 `SysConfigEntity`；默认 `dryRun=true`；非预检须 `confirm=true` |
| `POST /clearLocalCache` | 清本机 map/JSON 缓存并从 DB 回源 |
| `POST /clearRedis` | 清共享 Redis 中 SysConfig 值缓存 |
| `POST /reload` | 清 Redis + 本机回源 DB + Pub/Sub 通知集群（同迁移后刷新） |

菜单：**运维监控 → 配置安全**；**参数管理**（`modules/sys/config`）工具栏「配置安全」快捷跳转；数据库管理页快捷入口一并提供。列表支持单行「加密 / 解密 / 编辑」；编辑保存后自动加密落库；配置安全页可回跳参数管理 / 字段存储加密。页面「缓存管理」可手动清理本机/Redis 或全量重载。

---

## 5. 与传输加密

| | 存储 `@FieldEncrypt` | 传输 `docs/AI_CRYPTO.md` |
|---|---------------------|---------------------------|
| 密钥 | `autumn.crypto.field.key` | 会话 AES / RSA |
| 范围 | DB 字段 at-rest | HTTP body |

---

## 6. 纪律

**必须**

- `@FieldEncrypt` 实体 Service → `EncryptModuleService`
- 链式读 → `lambdaQuery` / `query` / `ktQuery`（勿 `ChainWrappers` 直 new）
- `@Cache` 在加密 searchable 字段 → 明文键 + 自动 hash 回源（§7）；hash 列用 `@Cache(name="hash")`
- `baseMapper` / Dao 手写 SQL 读实体 → `afterRead`；读 `Map`/标量 → `afterReadMap` / `afterReadMaps` / `afterReadScalar(s)`
- `searchable=true` → 实体声明 `{field}Hash` + `@Column`
- 手写 SQL 等值查 searchable 字段 → 查 **hash 列** 并自行 HMAC 参数

**常见误用**

- 以为关写入开关会自动删 `*_hash` 列
- 以为 `searchable=true` 会自动建 hash 列
- 用固定 `vector` 做等值查询
- 对 `ModuleService` 子类指望自动加解密或加密缓存
- 日志打印解密后明文

---

## 7. 与 `@Cache`（字段加密 + 缓存）

`EncryptModuleService` 通过 **钩子** 与 `BaseCacheService` 打通；`ModuleService` **不依赖** `FieldEncryptService`，行为与改造前一致。

### 7.0 怎么选

| 实体 | Service 基类 | 缓存 / 列表查询 |
|------|--------------|-----------------|
| 无 `@FieldEncrypt` | `ModuleService` | 原 `@Cache` 流程，零加密开销 |
| 含 `@FieldEncrypt` | **`EncryptModuleService`** | CRUD 加解密 + §7 缓存/条件钩子 |

业务侧 **无需** 手工解密 cache 值、拼 hash SQL 或维护双通道失效。

### 7.1 业务使用（三步）

**① 实体**：searchable 加密字段 + hash 列各标 `@Cache`（hash 列 naming 用常量 `FieldEncryptService.HASH_CACHE_CHANNEL`，即 `"hash"`）。

**② Service**：`extends EncryptModuleService<Dao, Entity>`。

**③ 调用**：明文走默认通道；hash hex 走命名通道。

```java
@Column(comment = "API Key")
@Cache
@FieldEncrypt(searchable = true)
private String apiKey;

@Column(length = 64, comment = "API Key哈希:盲索引")
@Cache(name = "hash")
private String apiKeyHash;
```

```java
public class ApiCredentialService extends EncryptModuleService<ApiCredentialDao, ApiCredentialEntity> {

    /** 调用方传明文 apiKey */
    public ApiCredentialEntity getByApiKey(String plainKey) {
        return getCache(plainKey);
    }

    /** 调用方传 64 位 HMAC hex */
    public ApiCredentialEntity getByApiKeyHash(String hashHex) {
        return getNameCache("hash", hashHex);
    }
}
```

同一实体还可有 **普通** `@Cache` 字段（如 `uuid`）：`getCache(uuid)` 走原流程，仅 **值路径** 会因实体含 `@FieldEncrypt` 而在入缓存前解密。

### 7.2 `compute` 流水线

```
getCache(key)
  → compute
      ├─ 快路径：isEncryptCacheEntity=false 且 isEncryptCacheNaming=false
      │           → 与改造前完全一致
      └─ 桥接路径
            → cache miss：supplier → getEntity → applyCacheFieldEq
                  → isEncryptCacheField ? tryEncryptCacheEq (hash 盲查) : wrapper.eq
            → 入缓存：prepareCacheValue 解密实体
            → 键镜像：mirrorEncryptCache（明文 ↔ hash 通道）
```

| 阶段 | 谁负责 | 说明 |
|------|--------|------|
| 缓存索引 | `cacheKey` | 业务明文或 hash hex，**不是**库内密文 |
| cache miss 查库 | `applyCacheFieldEq` → `tryEncryptCacheEq` | 明文 key 转 `WHERE *_hash = HMAC(plain)` |
| 写入缓存 | `prepareCacheValue` | 存解密后的业务态实体 |
| 双通道 | `mirrorEncryptCache` | 命中/加载后镜像到另一 naming |

### 7.3 架构：键按字段、值按实体

| 维度 | 判定钩子 | 行为 |
|------|----------|------|
| **缓存键**（回源 / 失效 / 镜像） | `isEncryptCacheField` | 仅加密或 hash {@code @Cache} 字段 |
| **其它 {@code @Cache} 字段** | 上式为 false | 与改造前 **完全一致** |
| **缓存值**（整实体） | `isEncryptCacheEntity` | 实体含 {@code @FieldEncrypt} 时入缓存解密 |

### 7.4 钩子命名（`BaseCacheService` 默认空/false，`EncryptModuleService` 实现）

| 钩子 | 用途 |
|------|------|
| `isEncryptCacheField(fieldName)` | 是否加密/hash 缓存字段 |
| `isEncryptCacheNaming(naming)` | 当前 `@Cache#name()` 是否加密通道 |
| `isEncryptCacheEntity()` | 入缓存前是否解密整实体 |
| `prepareCacheValue` / `prepareCacheValueList` | 入缓存值变换（→ `onRead`） |
| `tryEncryptCacheEq` | cache miss 回源 hash 盲查 |
| `mirrorEncryptCache` | 加载后镜像 hash 通道 |
| `encryptCacheEvictionKeys` | 失效键（明文 + hash） |
| `encryptCacheEvictionValue` | 复合 key 失效时还原明文 |
| `tryHashQueryCondition` | 列表/分页 searchable 改 hash 列 |

`BaseCacheService` **不引用** `FieldEncryptService`；底层细节在 `FieldEncryptService#resolveCacheDbLookup` / `#resolveCacheMirrorKeys` / `#resolveCacheEvictionKeys`。

### 7.5 `ModuleService` 回归保证

| 路径 | `ModuleService` |
|------|-----------------|
| `getCache` hit | 不调 supplier / `applyCacheFieldEq` |
| `compute` | 两判定均为 false → 直调 `cacheService.compute` |
| `applyCacheFieldEq` | `isEncryptCacheField` false → 原 `wrapper.eq` |
| `getCondition` | `tryHashQueryCondition` false → 原列映射 |
| 失效 | 单 key，不解析 hash 通道 |
