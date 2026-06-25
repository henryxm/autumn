# Autumn

Autumn 是一套可扩展的后台基础框架：在 Spring Boot 之上提供权限、系统管理、代码生成、两级缓存、分布式队列、HTTP 混合加解密、**字段存储加密**、定时任务、机器人开放 API、支付安全凭证等能力，并配套 **`docs/` 约束文档**与 **Cursor Skill**，支持传统开发与 AI 协作并行。

---

## 版本与分支

| 线 | 分支 /  artifact | 运行时 | 命名空间 | Cursor Skill |
|----|------------------|--------|----------|--------------|
| **2.x（当前默认）** | `master` · `2.0.0` | JDK **8** · Spring Boot **2.7.18** · MyBatis-Plus **2.x** | `javax.*` | `.cursor/skills/autumn-framework-2x/SKILL.md` |
| **3.x** | `3.0.0` · `3.0.0` | JDK **17+** · Spring Boot **3.5.x** · MyBatis-Plus **3.x** | `jakarta.*` | `.cursor/skills/autumn-framework-3x/SKILL.md` |

业务工程请在 `AGENTS.md` 或首轮对话中**写明依赖的 Autumn 主版本**，避免 2.x / 3.x 规范与 Skill 混用。具体版本以根 `pom.xml` 为准。

---

## 核心能力一览

| 能力 | 说明 | 深入阅读 |
|------|------|----------|
| **模块与权限** | `sys / gen / job / db / oauth / usr / oss / lan / spm / wall / bot / safe` 等 | `docs/AI_MAP.md` |
| **Service 继承链** | `ModuleService` → 缓存 / 队列 / 方言 / 自动 uuid | `docs/AI_CODEGEN.md` · `docs/AI_DUAL_KEY.md` |
| **两级缓存** | EhCache + Redis；`@Cache` 注解回源；共享缓存与失效广播 | `docs/AI_MAP.md` §2.1 |
| **分布式队列** | Memory / Redis List / Stream / 延迟 / 优先级；重试与死信 | `docs/AI_MAP.md` §2.2 |
| **分布式锁** | `DistributedService.withLock*`；降级、重试、配置化 | `docs/AI_DISTRIBUTED_LOCK.md` |
| **HTTP 混合加解密** | RSA 握手 + AES 会话；请求/响应按条件触发 | `docs/AI_CRYPTO.md` |
| **字段存储加密** | 实体 `@FieldEncrypt` · AES-GCM 落库 · 盲索引查询 · `@Cache` 联动 | **`docs/AI_FIELD_ENCRYPT.md`** |
| **定时任务** | `LoopJob` 接口周期（推荐）+ `schedulejob` cron | `docs/AI_MAP.md` §2.5 |
| **代码生成** | 实体注解驱动建表 → gen 模板 → 业务只改非 gen 层 | `docs/AI_CODEGEN.md` |
| **多数据库** | `RuntimeSql` / `WrapperColumns`；Dao 仅 Provider 写 SQL | `docs/AI_DATABASE.md` |
| **机器人 API** | `/bot/api/v1/*` · Hook 验签 · `rbt_` 推送 | `docs/AI_ROBOT.md` |
| **支付安全** | `/safe/api/v1/*` · 闸门 · PIN/生物识别 · `PayPinVerifier` | `docs/AI_SAFE_CREDENTIAL.md` |
| **Handler 扩展** | `autumn-handler` 条件注入与顺序扩展 | 在线文档 `handler` 章节 |

**传输加密**（`docs/AI_CRYPTO.md`）与 **字段存储加密**（`docs/AI_FIELD_ENCRYPT.md`）使用**不同密钥与服务**，请勿混用。

---

## 字段存储加密（at-rest）

面向需要在数据库中加密 `String` 字段、又要在业务层保持明文的场景。

### 怎么用

1. 实体字段标注 `@FieldEncrypt`（可选 `searchable=true` + 手写 `{field}Hash` 列）。
2. 模块 Service 继承 **`EncryptModuleService`**（无加密字段则继续用 **`ModuleService`**，零开销）。
3. 配置 `autumn.crypto.field.*`（见下文）；管理页 **`fieldencrypt.html`** / API **`/sys/crypto/field/*`**。

### 加解密路径（唯一）

| 调用方式 | 写 | 读 |
|----------|----|----|
| **`EncryptModuleService`** 的 `insert*` / `update*` / `select*` | 自动 `onWrite`，落库后 **`restoreAfterWrite`**（内存保持明文） | 自动 `onRead` |
| **`baseMapper` / Dao 手写 SQL** | 手动 `encrypt.onWrite` 或改走 Service 写方法 | 实体 **`afterRead(...)`**；Map/标量 **`afterReadMap` / `afterReadMaps` / `afterReadScalar(s)`** |
| **`ModuleService` 子类** | 无 | 无 |

- 列表/分页 **searchable** 等值条件：`EncryptModuleService#tryHashQueryCondition` 改写 hash 列。
- **`@Cache` + 加密字段**：明文键与 hash 通道（`FieldEncryptService.HASH_CACHE_CHANNEL`）由框架钩子处理，详见 **`docs/AI_FIELD_ENCRYPT.md` §7**。
- 迁移读库内密文：`FieldEncryptContext.runSkip(...)`（`FieldEncryptMigrationService`）。

### 配置示例

```yaml
autumn:
  crypto:
    field:
      enabled: true
      key: ${AUTUMN_FIELD_ENCRYPT_KEY:}    # Base64，32 字节 AES-256
      hash-key: ${AUTUMN_FIELD_HASH_KEY:}  # 盲索引 HMAC；建议与 key 分开
      prefix: "ENC$v1$"
```

### 关键类

| 类 | 职责 |
|----|------|
| `@FieldEncrypt` | 标记加密字段 |
| `FieldEncryptService` | `onWrite` / `onRead` / 盲索引 / 缓存解析 |
| `EncryptModuleService` | 带加密实体的 Service 基类（CRUD + 缓存钩子） |
| `FieldEncryptRuntimeService` | 运行时写入开关（单机 sys_config / 集群 Redis） |
| `FieldEncryptAdminController` | 管理 API |

约束单测：`FieldEncryptConventionTest` · `FieldEncryptCacheTest` · `EncryptModuleServiceTest`。

---

## Service 默认继承链

业务 Service 通常从 **`ModuleService`** 出发，按需获得以下能力（无需重复实现）：

```text
ModuleService
  → BaseService          # 分页、getCondition、菜单
  → DistributedService   # withLock*
  → ShareCacheService    # 跨模块共享缓存
  → BaseCacheService     # @Cache 回源与失效
  → BaseQueueService     # 队列注册与发送
  → AutoIdService        # UuidBased / SnowBased 第二主键
  → DialectService       # RuntimeSql / columnInWrapper
```

含 **`@FieldEncrypt`** 的实体：将 **`ModuleService`** 换为 **`EncryptModuleService`**，其余链不变。

---

## 文档入口

### 应用内在线文档

| 入口 | 路径 |
|------|------|
| 文档首页 | `/modules/docs/index` |
| 推荐顺序 | quickstart → architecture → ai-collab → handler → sys → cache / queue / job / oauth / hybrid-crypto |
| 模板源码 | `autumn-modules/src/main/resources/templates/modules/docs/` |

### 仓库 `docs/`（AI 与研发约束）

**所有 `AI_*.md` 均在 `docs/` 目录**，不在仓库根目录。

| 第一步 | 文件 |
|--------|------|
| 索引 | [`docs/AI_INDEX.md`](docs/AI_INDEX.md) |
| 最小上下文 | [`docs/AI_BOOT.md`](docs/AI_BOOT.md) |
| 能力地图 | [`docs/AI_MAP.md`](docs/AI_MAP.md) |
| 强制规范 | [`docs/AI_STANDARDS.md`](docs/AI_STANDARDS.md) |

**按场景追加**（见 `AI_INDEX.md` §2）：

| 场景 | 文档 |
|------|------|
| 新模块 / 代码生成 | `AI_CODEGEN.md` · `AI_TEMPLATES.md` |
| SQL / 多库 / Provider | `AI_DATABASE.md` |
| 字段存储加密 | **`AI_FIELD_ENCRYPT.md`** |
| HTTP 加解密 | `AI_CRYPTO.md` |
| 机器人对接 | `AI_ROBOT.md` · `AI_ROBOT_API.md` |
| 支付安全 | `AI_SAFE_CREDENTIAL*.md` |
| 升级 autumn 版本 | `AI_UPGRADE.md` |

**Cursor / 业务仓引用**

- 本仓库：`@docs/AI_INDEX.md`
- 与 autumn 并列的业务仓：`@../autumn/docs/AI_BOOT.md`（见 `AI_INDEX.md` §4）

---

## AI 协作与 Cursor Skill

| Skill | 适用 | 路径 |
|-------|------|------|
| **autumn-framework-2x** | JDK 8 · Boot 2.7 · MP2 · master | `.cursor/skills/autumn-framework-2x/SKILL.md` |
| **autumn-framework-3x** | JDK 17+ · Boot 3.5 · MP3 · 3.0.0 分支 | `.cursor/skills/autumn-framework-3x/SKILL.md` |

Skill 与 `docs/` 口径一致，摘要包括：分层纪律、Dao Provider、双主键、`@FieldEncrypt` / **`EncryptModuleService`**、加密缓存钩子、Bot、safe 等。可同步到 `~/.cursor/skills/` 供全局使用。

**约束扫描（按需）**：全量 `bash scripts/constraints-scan`（含 **I 组 FQN**）；**PR 硬门禁** `bash scripts/check-java-fqn`（见 `AI_CODE_STYLE.md` §7.1、`.github/workflows/java-style-check.yml`）。

---

## 工程结构

```text
autumn/
├── autumn-handler/     # Handler 扩展接口与默认实现
├── autumn-lib/       # 缓存、队列、加解密、RuntimeSql、FieldEncryptService 等
├── autumn-modules/   # 业务模块（sys、gen、bot、safe…）与 Controller
├── autumn-starter/   # 一站式依赖聚合
├── web/              # 启动入口（主类 cn.org.autumn.Web）
├── docs/             # AI_*.md 约束与专项文档
├── scripts/          # constraints-scan、dependency-scan 等
└── .cursor/skills/   # autumn-framework-2x / 3x
```

---

## 环境与构建

| 项 | 要求 |
|----|------|
| JDK | **1.8**（2.x 线） |
| Maven | 3.8+ |
| MySQL | 5.7+（建议 8.x） |
| Redis | 建议开启（缓存、Shiro 会话、队列、集群字段加密开关等） |

父 POM 在 **`validate`** 阶段启用 **`dependencyConvergence`**；升级依赖后若 enforcer 报错，请在 **`dependencyManagement`** 中对齐版本。

```bash
mvn clean package -DskipTests
```

---

## 快速启动

1. 创建数据库 `autumn`（UTF-8 / utf8mb4）。
2. 修改 `web/src/main/resources/application-dev.yml` 中的数据源。
3. 确认 `application.yml` 中 Redis 可连接（若启用）。
4. 构建（见上），然后：
   - IDE 运行 `web/src/main/java/cn/org/autumn/Web.java`，或
   - `java -jar web/target/web.jar`

| 项 | 默认值 |
|----|--------|
| 端口 | `80` |
| 管理后台 | http://localhost/ |
| 在线文档 | http://localhost/modules/docs/index |
| Swagger | http://localhost/swagger/index.html |
| 账号 | `admin` / `admin` |

生产环境建议：`autumn.redis.open=true`、`autumn.shiro.redis=true`；多节点定时任务配合 `LoopJob` 的 `assignTag` / `server.tag`。

---

## 开发纪律（摘要）

完整条文见 **`docs/AI_STANDARDS.md`**。高频项：

- **分层**：Controller 禁止注入 Dao；Service 通过 `baseMapper` 访问本实体；跨域走对方 Service。
- **SQL**：Dao 仅 **Provider**（`*DaoSql extends RuntimeSql`），禁止 Dao 内联 `@Select`；条件列用 **`columnInWrapper`**，禁止硬编码方言引号。
- **代码生成**：`controller/gen`、`Pages/*.html/js` **勿手改**；业务写在非 gen 层。
- **双主键**：关联与对外标识用 **`uuid`**，不用自增 `id`（`docs/AI_DUAL_KEY.md`）。
- **定时任务**：固定周期优先 **`LoopJob.*`**；复杂日历再用 cron。
- **字段加密**：`@FieldEncrypt` 实体 → **`EncryptModuleService`**；手写 SQL → **`afterRead*`**；`searchable` → 手写 hash 列。

---

## 维护脚本

| 脚本 | 用途 |
|------|------|
| `scripts/constraints-scan` | 规范分组扫描 A～H（按需） |
| `scripts/autumn-dependency-scan.sh` | 依赖方升级 autumn 时的只读体检（见 `AI_UPGRADE.md` §4） |

---

## 链接

- 项目站点：http://autumn.org.cn
- 文档总索引：[`docs/AI_INDEX.md`](docs/AI_INDEX.md)
