# Autumn 字段存储加密（按需加载）

> 用途：实体 `@FieldEncrypt` 字段落库加密、读库解密；与传输层 `docs/AI_CRYPTO.md` **完全独立**（不同密钥、不同算法用途、不同 Service）。

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
| **`EncryptModuleService`** 的 `insert*` / `update*` / `select*` | 自动 `onWrite` + `restoreAfterWrite` | 自动 `onRead` |
| **`baseMapper` / Dao 手写 SQL** | 须手动 `encrypt.onWrite`（或改走 Service 写方法） | 返回实体须 **`afterRead(...)`**；返回 `Map`/标量须 **`afterReadMap` / `afterReadMaps` / `afterReadScalar(s)`** |
| **`ModuleService` 子类** | 无加解密 | 无自动解密 |

列表/分页 **searchable** 等值条件：`BaseService#getCondition` → `EncryptModuleService#tryHashQueryCondition`（改 hash 列）。

约束单测：`FieldEncryptConventionTest`（存在 `@FieldEncrypt` 实体时，其 Service 须 extends `EncryptModuleService`）。

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
| 集群 | Redis `{ns}:autumn:field-encrypt:write-enabled` | Redis 优先，空则 env 回填 |

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
    // ServiceImpl 写：onWrite → 落库 → restoreAfterWrite（业务侧保持明文）
    // ServiceImpl 读：select* 自动 onRead
}

// baseMapper / Dao 手写 SQL（示例）
SomeEntity row = baseMapper.getByUuid(uuid);
afterRead(row);
```

---

## 3. 关键类

| 类 | 职责 |
|----|------|
| `FieldEncrypt` | 字段注解（见 §0） |
| `FieldEncryptService` | `onWrite` / `onRead`、盲索引、`useHashForQuery` |
| `EncryptModuleService` | 带 `@FieldEncrypt` 实体的 Module Service 基类 |
| `FieldEncryptRuntimeService` | 启动加载运行时开关；集群 Redis 同步 |
| `FieldEncryptAdminController` | 管理 API：`/sys/crypto/field/*` |

---

## 4. 管理后台（开发）

页面 **`fieldencrypt.html`**，API 前缀 **`/sys/crypto/field`**（系统管理员）：

| 能力 | 说明 |
|------|------|
| `GET /status` | 当前开关、密钥来源、集群模式 |
| `POST /writeEnabled` | 运行时写入开关 |
| `POST /test` | 内存加解密往返测试 |
| `POST /decrypt` | 对 `ENC$v1$...` 密文解密（仅内存，不写库） |
| `POST /generateKey` | 生成开发用 key/hash-key（复制到 yml 后重启） |
| `GET /list` | 已扫描到的 `@FieldEncrypt` 实体清单 |

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
