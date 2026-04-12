# Autumn

Autumn 是一个可扩展的后台基础框架，覆盖权限、系统管理、代码生成、缓存、队列、加解密、定时任务、对象存储、防火墙等常见能力，并提供在线文档与 AI 协作指引。

## 1. 当前版本与定位

- 当前版本：`2.0.0`
- 技术基座：Spring Boot 3.x、MyBatis-Plus、Shiro、Redis、EhCache、Vue2、FreeMarker
- 设计目标：
  - 提供可复用的通用能力，减少业务项目重复造轮子
  - 通过 `autumn-handler` 做框架扩展解耦
  - 支持“传统模式 + AI 协作开发模式”并行

## 2. 核心能力总览

- 系统模块：`sys / gen / job / db / oauth / usr / oss / lan / spm / wall`
- 框架能力：
  - 缓存：两级缓存（EhCache + Redis）、共享缓存抽象、失效同步
  - 队列：Memory/Redis List/Redis Stream/Delay/Priority，支持重试与死信
  - 加解密：RSA + AES 混合模式，支持请求/返回兼容改造
  - 定时任务：`schedulejob(cron)` + `LoopJob(接口周期)`，支持统一管理与告警
  - Handler 扩展：默认实现 + 条件注入 + 顺序控制

## 3. 在线文档入口（推荐先看）

项目已内置完整在线文档，建议新同学优先通过文档入门：

- 文档首页：`/modules/docs/index`
- 推荐阅读顺序：
  1. `quickstart`
  2. `architecture`
  3. `ai-collab`
  4. `handler`
  5. `sys`
  6. `cache / queue / job / oauth / hybrid-crypto`

文档模板目录：`autumn-modules/src/main/resources/templates/modules/docs/`

## 4. AI 协作入口（多项目）

当你在维护多个基于 Autumn 的项目时，建议固定引用以下文档：

- `AI_INDEX.md`：文档总索引与按场景加载组合
- `AI_BOOT.md`：最小启动上下文
- `AI_MAP.md`：框架能力地图（给 AI 的硬约束）
- `AI_STANDARDS.md`：约束性开发规范（分层、API、实体/库表、Dao 与 Provider、资源与页面 Site/PageAware 等）
- `AI_CODEGEN.md`：代码生成链路（gen、GenUtils、Velocity 模板、库表反射）、推荐三步开发流程，以及 `BaseCacheService` / `ShareCacheService` / `BaseQueueService` 能力说明（与 `AI_STANDARDS`、生成矩阵配合使用）
- `AI_GUIDE.md`：多项目提示词模板与实操规则

本仓库内 Cursor Agent Skill（可与个人 `~/.cursor/skills` 同步）：`.cursor/skills/autumn-framework/SKILL.md`

## 5. 工程结构

```text
autumn
├─autumn-handler    # Handler 扩展接口与默认实现
├─autumn-lib        # 基础能力（缓存/队列/加解密/通用模型）
├─autumn-modules    # 业务模块与控制器实现
├─autumn-starter    # 一站式引入Autumn框架所有功能
└─web        # Web 启动入口（主类：cn.org.autumn.Web）
```

## 6. 环境要求

- JDK：`1.8`
- Maven：`3.8+`
- MySQL：`5.7+`（建议 8.x）
- Redis：建议开启（缓存、分布式会话、队列/加密等场景更完整）

## 7. 快速启动

1. 创建数据库 `autumn`（UTF-8/utf8mb4）。
2. 修改数据库配置：`web/src/main/resources/application-dev.yml`。
3. 如需 Redis，确认 `web/src/main/resources/application.yml` 中 Redis 参数可连接。
4. 在项目根目录执行：

```bash
mvn clean package -DskipTests
```

5. 启动方式二选一：
   - IDE 启动主类：`web/src/main/java/cn/org/autumn/Web.java`
   - 命令行启动：`java -jar web/target/web.jar`

默认配置参考：

- 服务端口：`80`
- 上下文路径：`/`
- 管理后台：`http://localhost/`
- 文档首页：`http://localhost/modules/docs/index`
- API 文档：`http://localhost/swagger/index.html`
- 默认账号：`admin/admin`

## 8. 定时任务选型建议（重点）

Autumn 提供两类任务机制：

- `schedulejob`：cron 表达式任务（复杂时间规则适用）
- `LoopJob`：接口式固定周期任务（推荐常规业务优先）

建议规则：

- 固定周期任务优先 `LoopJob.OneMinute/FiveMinute/...`，可显著降低 cron 表达式错误率。
- 复杂日历规则（如每月指定日、节假日）再使用 cron。
- 上线前至少校验：幂等、防重入（`skipIfRunning`）、超时（`timeout`）、连续错误阈值（`maxConsecutiveErrors`）、多节点分配（`assignTag/server.tag`）。

## 9. 分布式与运维说明

- 建议开启：
  - `autumn.redis.open=true`
  - `autumn.shiro.redis=true`
- 多节点任务建议结合 `LoopJob` 分配能力统一管理，避免重复执行。
- 缓存、队列、加密能力建议统一复用框架内置服务，避免项目内重复实现。