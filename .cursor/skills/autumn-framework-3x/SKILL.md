---
name: autumn-framework-3x
description: >-
  Autumn 3.0.0 line ONLY: JDK 17+, Spring Boot 3.5.x, MyBatis-Plus 3.x, jakarta.* namespace.
  Use on autumn branch 3.0.0 (artifact 3.0.0) or business apps pinned to that stack.
  NOT for Autumn 2.0.0 / JDK 8 / Spring Boot 2.7 / javax-only — use autumn-framework-2x on master.
  Enforces docs/AI_STANDARDS.md + docs/AI_DATABASE.md: entity-driven schema; Dao SQL only via MyBatis Provider (*DaoSql extends RuntimeSql);
  No hardcoded dialect quotes in Java (RuntimeSql quote/columnInWrapper or WrapperColumns per docs/AI_DATABASE.md §4.0);
  Controller must not use Dao; Service uses baseMapper; gen/Pages/list.html/js never hand-edited; statics/pages/Site/PageAware.
  Read docs/AI_CODEGEN.md, docs/AI_DATABASE.md; scripts/autumn-dependency-scan.sh for upgrades.
  Triggers on cn.org.autumn 3.0.0, Spring Boot 3.5, JDK 17, ModuleService, RuntimeSql, PageAware, SpringDoc.
---

# Autumn 3.x 框架开发（3.0.0 / 分支 3.0.0）

## 版本矩阵（本 Skill 唯一适用）

| 项 | 版本 / 约束 |
|----|-------------|
| **Autumn** | **3.0.0**（`cn.org.autumn:*:3.0.0`，**3.0.0** 分支） |
| **JDK** | **17+** |
| **Spring Boot** | **3.5.x**（以根 `pom.xml` 为准） |
| **MyBatis-Plus** | **3.x** |
| **命名空间** | **`jakarta.*`** |
| **2.x 线** | **禁用本 Skill**：Autumn **2.0.0**、JDK **8**、Spring Boot **2.7** 请用 **`autumn-framework-2x`**（**master**） |

业务工程须在 `AGENTS.md` 或首轮对话中写明依赖的 Autumn 主版本，避免 2.x / 3.x 规范混用。

## 何时启用

- 当前工作区是 **autumn 仓库且检出 3.0.0 分支**，或业务工程 **Maven 依赖锁定 `cn.org.autumn` 3.0.0**。
- 提到：`cn.org.autumn`、`ModuleService`、`RuntimeSql`、`DatabaseType`、`WrapperColumns`、`QueryWrapper`、`PageAware`、`SpringDoc` 等，且 **栈为 JDK 17+ + Boot 3.5**。

## 文档加载顺序

1. `docs/AI_INDEX.md` → 2. `docs/AI_BOOT.md` → 3. `docs/AI_MAP.md` → 4. **`docs/AI_STANDARDS.md`**
5. **`docs/AI_DATABASE.md`**（含 **§4.0 代码层方言标准写法**、`WrapperColumns`、`RuntimeSql`、Wrapper 边界、Dao **必须** Provider）
6. 新模块 / 代码生成：追加 **`docs/AI_CODEGEN.md`**
按需：`docs/AI_POSTGRESQL.md`、`docs/AI_UPGRADE.md`、`docs/AI_DISTRIBUTED_LOCK.md`、**`docs/REDIS_STANDALONE.md`**（可选 Redis、**§6 依赖方 `RedisTemplate` 注入**）、`docs/REDIS_RESILIENCE.md` 等。

## 多库与 SQL（与 `docs/AI_DATABASE.md` 一致）

- **禁止硬编码方言**：表/列/排序/Map 键不写死 `` ` `` / `"` / `[]`。
- **Provider**：`**DaoSql extends RuntimeSql**`，用 **`quote` / `columnInWrapper`** 等 §2.1 能力。
- **未继承 `DialectService`** 的 Java 路径：用 **`WrapperColumns.columnInWrapper`**、分页排序 **`orderByColumnExpression`**、Map 等值 **`queryWrapperAllEqQuoted`**（以分支内 `WrapperColumns` 实际方法名为准），详见 **`docs/AI_DATABASE.md` §4.0**；存量反模式见 **§8.1**、检索 **§8.5**。
- **Controller** 禁止 **Dao**；**Service** 用 **`baseMapper`**；复杂 SQL **Dao + Provider**。

## 分布式执行与加锁（新增）

- 涉及跨节点任务互斥、热点写入串行化、任务防重入时，优先复用框架锁能力，不自建锁组件。
- **已继承 `ModuleService` / `BaseService`**：使用 **`DistributedService`** 的 `withLock*` 系列（严格/降级/重试）。
- **未继承基础框架能力**：直接注入 **`DistributedLockService`**。
- 配置统一通过后台 **`DistributedLockConfig`**（`DISTRIBUTED_LOCK_CONFIG`）管理，读取方式为 `sysConfigService.getObject(...)`。
- 强一致场景默认严格失败；非强一致场景可用 `withLockOrFallback` 做服务降级。
- 并发突发场景必须使用 `withLockRetry` 的随机退避机制，避免锁竞争雪崩。
- **依赖方 / 兄弟模块**：未启用 Redis 时 **无 `RedisTemplate`**；勿默认 **`@Autowired RedisTemplate`**，应 **`required = false`** / **`ObjectProvider`** + 判空降级（与 2.x 相同纪律，见 **`docs/REDIS_STANDALONE.md` §6**）。

## 自检清单

- Dao 无内联 SQL？无手写方言引号（§4.0）？
- **Controller** 未碰 **Dao**？未手改 **gen / list.html/js**？

## 多项目一句话

**`docs/AI_BOOT.md` → `docs/AI_MAP.md` → `docs/AI_STANDARDS.md` → `docs/AI_DATABASE.md`（含 §4.0）→ `docs/AI_CODEGEN.md`**（**仅 3.0.0 / JDK17+ / Boot 3.5 栈**）。
