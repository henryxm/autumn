# Autumn

Autumn 是一个可扩展的后台基础框架，覆盖权限、系统管理、代码生成、缓存、队列、加解密、定时任务、对象存储、防火墙等常见能力，并提供在线文档与 AI 协作指引。

## 1. 当前版本与定位

- **本分支 `3.0.0`**：框架版本 **`3.0.0`**，**JDK 17+**，**Spring Boot 3.5.10**，**MyBatis-Plus 3.x**，**`jakarta.*`**，Shiro Jakarta 分类器、**SpringDoc（OpenAPI）** 等（详见根 `pom.xml`）。
- **`master`（2.0.0 线）**：JDK 8、Spring Boot 2.7.18、MyBatis-Plus 2.x、`javax.*`；与 3.x **勿混用 Skill 与依赖坐标**。
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

## 3. 文档入口

### 3.1 在线文档（应用内）

- 文档首页：`/modules/docs/index`
- 推荐阅读顺序：`quickstart` → `architecture` → `ai-collab` → `handler` → `sys` → `cache` / `queue` / `job` / `oauth` / `hybrid-crypto`
- 文档模板目录：`autumn-modules/src/main/resources/templates/modules/docs/`

### 3.2 仓库内 Markdown（`docs/`）

面向 AI 与研发的约束性文档已全部放在 **`docs/`** 目录（**不再使用仓库根目录下的 `AI_*.md` 路径**）。

| 文件 | 用途 |
|------|------|
| `docs/AI_INDEX.md` | 总索引与按场景加载组合 |
| `docs/AI_BOOT.md` | 最小启动上下文 |
| `docs/AI_MAP.md` | 框架能力地图（硬约束） |
| `docs/AI_STANDARDS.md` | 应用层与数据访问规范 |
| `docs/AI_DATABASE.md` | 多库、`RuntimeSql`、Provider、Wrapper 边界 |
| `docs/AI_CODEGEN.md` | 代码生成与三步开发 |
| `docs/AI_GUIDE.md` | 多项目导航与 `@` 引用示例 |
| 其它 `docs/AI_*.md` | 专项（PG、升级、安全、模板等） |

**路径说明（避免引用错误）**

- 在 **本仓库** 内用 Cursor：`@docs/AI_INDEX.md` 等。
- 业务仓库与 **autumn 并列**（例如 `../autumn`）：`@../autumn/docs/AI_BOOT.md`（见 `docs/AI_INDEX.md` §4）。
- 在线章节「AI 协作」中的 `@` 示例已改为占位符 **`&lt;autumn-root&gt;/docs/...`**，请替换为本地 autumn 根路径。

### 3.3 升级体检脚本

- `scripts/autumn-dependency-scan.sh`：只读扫描（`pom`、配置、`FIND_IN_SET`、Dao 注解 SQL 线索等），详见 `docs/AI_UPGRADE.md` §4。

## 4. AI 协作与 Cursor Skill

- **本分支（3.0.0）**：`.cursor/skills/autumn-framework-3x/SKILL.md`（JDK 17+ / Spring Boot 3.5 / Jakarta / MP3）。
- **master（2.0.0 线）**：`.cursor/skills/autumn-framework-2x/SKILL.md`（JDK 8 / Spring Boot 2.7 / `javax` / MP2）。
- 上述 Skill 与 **`docs/AI_*.md`** 路径一致；可同步到个人 `~/.cursor/skills`。业务工程请在 `AGENTS.md` 或首轮提示中写明 **Autumn 主版本**，避免 2x / 3x 规范混用。

## 5. 工程结构

```text
autumn
├─ autumn-handler    # Handler 扩展接口与默认实现
├─ autumn-lib        # 基础能力（缓存/队列/加解密/通用模型）
├─ autumn-modules    # 业务模块与控制器实现
├─ autumn-starter    # 一站式引入 Autumn 模块（聚合依赖）
├─ web               # Web 启动入口（主类：cn.org.autumn.Web）
├─ docs/             # AI 与研发用 Markdown 文档
└─ scripts/          # 维护与升级辅助脚本
```

## 6. 依赖与构建

- 父 POM 在 **`validate` 阶段** 启用 **`maven-enforcer-plugin` 的 `dependencyConvergence`**，并在 **`dependencyManagement`** 中统一易冲突传递依赖版本，避免“同名不同版本”静默共存。
- 若升级依赖后构建失败，请根据 enforcer 报告在父 POM 的 **`dependencyManagement`** 中补齐或调整版本后重试。

## 7. 环境要求

- JDK：`17`
- Maven：`3.8+`
- MySQL：`5.7+`（建议 8.x）
- Redis：建议开启（缓存、分布式会话、队列/加密等场景更完整）

## 8. 快速启动

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
- API 文档（SpringDoc）：`http://localhost/swagger-ui.html`（或 `springdoc` 默认路径，以实际配置为准）
- 默认账号：`admin/admin`

## 9. 定时任务选型建议（重点）

Autumn 提供两类任务机制：

- `schedulejob`：cron 表达式任务（复杂时间规则适用）
- `LoopJob`：接口式固定周期任务（推荐常规业务优先）

建议规则：

- 固定周期任务优先 `LoopJob.OneMinute/FiveMinute/...`，可显著降低 cron 表达式错误率。
- 复杂日历规则（如每月指定日、节假日）再使用 cron。
- 上线前至少校验：幂等、防重入（`skipIfRunning`）、超时（`timeout`）、连续错误阈值（`maxConsecutiveErrors`）、多节点分配（`assignTag/server.tag`）。

## 10. 分布式与运维说明

- 建议开启：
  - `autumn.redis.open=true`
  - `autumn.shiro.redis=true`
- 多节点任务建议结合 `LoopJob` 分配能力统一管理，避免重复执行。
- 缓存、队列、加密能力建议统一复用框架内置服务，避免项目内重复实现。
