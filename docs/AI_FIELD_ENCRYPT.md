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
| 写入加密 **开** + `searchable=true` | 写密文 | 写 HMAC | `getCondition` → `mobile_hash = HMAC(...)` |
| 写入加密 **关**（运行时） | 写明文 | 不再维护 hash | 回退查 **明文列** `mobile` |
| 注解 `searchable=false` | 按 key 加解密 | 不参与 | 查明文列 |

### 0.5 Service 与 `baseMapper` 纪律

| 继承 | CRUD | `baseMapper` 自定义 SQL |
|------|------|-------------------------|
| `ModuleService` | **零**加解密 | 无自动解密 |
| `EncryptModuleService` | `encrypt.onWrite` / 读路径 `onRead` | 返回实体须 **`afterRead(...)`** |

约束单测：`FieldEncryptConventionTest`（存在 `@FieldEncrypt` 实体时，其 Service 须 extends `EncryptModuleService`）。

### 0.6 实现路径

| 路径 | 说明 |
|------|------|
| **默认** | 实体 `@FieldEncrypt` → Service 继承 **`EncryptModuleService`** → CRUD 自动 `onWrite` / `onRead` |
| **`baseMapper` 直查** | 返回实体须 **`afterRead(...)`** |
| **列表条件 searchable** | `BaseService#getCondition` 在写入开关开时改写到 hash 列 |
| **可选 TypeHandler** | `EncryptStringTypeHandler` 须显式 `@TableField(typeHandler=...)`；与 Service 路径二选一，勿叠用 |

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
    // baseMapper 直查示例
    List<XxxEntity> rows = afterRead(baseMapper.listByMobileHash(hmac));
}
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
- `baseMapper` 读实体 → `afterRead`
- `searchable=true` → 实体声明 `{field}Hash` + `@Column`
- 手写 SQL 等值查 searchable 字段 → 查 **hash 列** 并自行 HMAC 参数

**常见误用**

- 以为关写入开关会自动删 `*_hash` 列
- 以为 `searchable=true` 会自动建 hash 列
- 用固定 `vector` 做等值查询
- 对 `ModuleService` 子类指望自动加解密
- 日志打印解密后明文
