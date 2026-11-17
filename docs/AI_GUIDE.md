# Autumn 多项目 AI 开发指南（导航版）

> 目标：在多项目场景下，用最小上下文让 AI 快速进入正确执行路径。
> 统一索引入口：`@<autumn-root>/docs/AI_INDEX.md`

## 1. 推荐喂给顺序（3 层）

1. **框架层（固定）**
   - 首轮必读：`@<autumn-root>/docs/AI_BOOT.md`
   - 核心能力：`@<autumn-root>/docs/AI_MAP.md`
   - 应用层强制规范：`@<autumn-root>/docs/AI_STANDARDS.md`
   - 按需追加：`@<autumn-root>/docs/AI_DATABASE.md`（多库 SQL / Wrapper / Provider）/ `@<autumn-root>/docs/AI_POSTGRESQL.md` / `@<autumn-root>/docs/AI_UPGRADE.md` / `@<autumn-root>/docs/AI_CRYPTO.md` / `@<autumn-root>/docs/AI_CODEGEN.md` / `@<autumn-root>/docs/AI_TEMPLATES.md` / `@<autumn-root>/docs/AI_GOVERNANCE.md` / `@<autumn-root>/docs/AI_SECURITY.md` / `@<autumn-root>/docs/AI_PROMPTS.md`
2. **当前项目层（变化）**
   - 当前项目 `README`、模块目录、关键配置
3. **当前任务层（变化）**
   - 目标 + 约束 + 验收标准 + 禁止事项

## 2. 文档分工（避免全量加载）

- `docs/AI_BOOT.md`：最小启动上下文（首轮固定）
- `docs/AI_MAP.md`：高频开发能力主图
- `docs/AI_STANDARDS.md`：约束性开发规范（分层、API、定时任务、权限、FTL、实体与库表、Dao/Provider、资源与 Site）
- **`docs/AI_DATABASE.md`**：多数据库落地（`DatabaseType` 清单、**§1.1 关联键与业务主键**、**§4.0 代码层方言标准写法（`RuntimeSql` / `WrapperColumns`，禁止硬编码引号）**、全库兼容默认、Wrapper 边界、Dao 必须 Provider、`RuntimeSql`、推荐分层；**§8** 注解 Dao / 方言 Wrapper 升级步骤与 `rg` 检索示例）
- `docs/AI_POSTGRESQL.md`：PostgreSQL 专项（DDL、元数据、类型、分页 count、迁移）；通用跨库以 `docs/AI_DATABASE.md` 为准
- `docs/AI_UPGRADE.md`：依赖方升级清单、`scripts/autumn-dependency-scan.sh` 与「一键升级」可执行边界
- `docs/AI_CRYPTO.md`：接口加解密兼容专项
- `docs/AI_SECURITY.md`：签名/灰度/演练专项
- `docs/AI_CODEGEN.md`：代码生成链路（gen、GenUtils、模板、库表反射）、推荐三步开发流程、`BaseCacheService` / `ShareCacheService` / `BaseQueueService` 能力说明
- `docs/AI_TEMPLATES.md`：模块任务模板库
- `docs/AI_GOVERNANCE.md`：治理、术语、协作约定
- `docs/AI_PROMPTS.md`：可复制提问模板

## 3. 常用加载组合

- 日常开发：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md`（写 SQL / Wrapper 时加 `docs/AI_DATABASE.md`）
- 接口加解密改造：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_CRYPTO.md`
- 模块新建/代码生成：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_CODEGEN.md + docs/AI_TEMPLATES.md`
- 多项目模板整合（TemplateFactory）：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_GOVERNANCE.md`
- 安全改造/攻防演练：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_SECURITY.md`
- 多人协作/规范治理：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_GOVERNANCE.md`
- 多库 / Wrapper / Provider：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_DATABASE.md`（PG 专项再叠 `docs/AI_POSTGRESQL.md`）
- **老旧 Dao 注解 SQL / Wrapper 方言化改造**：`docs/AI_DATABASE.md` §8 + `docs/AI_UPGRADE.md` §3.3；体检运行 `scripts/autumn-dependency-scan.sh`
- 升级 autumn 基线版本：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_UPGRADE.md`（多库 SQL 债叠加 `docs/AI_DATABASE.md` §8）
- 快速发起任务：在以上组合基础上追加 `docs/AI_PROMPTS.md`

## 3.1 代码生成模板边界（MVC 约束）

- 可重生（默认不改）：`ControllerGen.java`、`*Pages.java`、`list.html`、`list.js`（由模板重生成）。
- 可维护（业务落点）：`Controller.java`、`Service.java`、`Site.java`、`Menu.java`。
- 先判定修改类型：
  - 单模块业务需求 -> 改可维护层；
  - 框架级批量行为统一 -> 改 `template/*.vm` 后重生成。
- 向 AI 下达任务时应显式声明：`禁止修改 gen 层` 或 `允许修改模板并重生成`，避免误改被覆盖。

## 3.2 应用层开发规范（约束性）

完整条文见 **`docs/AI_STANDARDS.md`**。摘要：

- **高内聚、低耦合**：**与实体相关的业务逻辑必须在对应 `*Service`**（§2.1）；Controller 只编排；实体 **`@Getter`/`@Setter`**，日志 **`@Slf4j`**（§2.0）；子项目间优先接口/服务调用。
- **对内管理端接口**：实体 **`*Controller extends *ControllerGen`**，`@RequestMapping("{module}/{entitylower}")`；不得与 gen **相同请求映射**；对内扩展写在该 Controller（**`docs/AI_STANDARDS.md` §3**）。
- **对外 API**：独立 Controller；**新**接口 **`POST /{module}/api/v1/{action}`**；入参 `*Request`、出参 `*VO`；`@Valid`；**`T` 禁止 Map/Entity**；历史 URL 不动（§4）。
- **定时任务**：生产 **禁止 `@Scheduled`**；`LoopJob` + `@JobMeta`（≤1min 须 `skipIfRunning` + `timeout`）或 `schedulejob` + cron。
- **`@RequiresPermissions`**：新接口一律不用（例外须登记）；普通用户用登录态（§6）。
- **FreeMarker 页面**：见 `docs/AI_STANDARDS.md` §7。
- **实体与库表**：注解建表、禁常规 DDL（§8）；表前缀不进类名（§9）；**短标题 2～8 + `:`**（§10.1）；索引/双键/字段加密见 §10.2～§10.5；触结构变更/新外键/新开放 API 须对齐双键。
- **生成层**：**`docs/AI_STANDARDS.md` §11.0**（可替换 · 禁特有逻辑 · 空壳除外）；未 ZIP 可预演模板 Gen（§11.2）；禁 AI 写 list/Pages。
- **SQL**：新代码 **Provider**（§12）；多库见 `docs/AI_POSTGRESQL.md`。
- **调用链**：**任何外部位置禁止直接引用 Dao**；对外只经 **Service**（Service 持有 Dao / `baseMapper`）。Controller / Facade / 他域 Service 等**只注入 Service**（§13）。
- **资源**：**`statics/`** 公共静态、匿名可访问、优先复用框架资源；新后台页放模块 **`pages`**；**`site/*Site`** 字段上 **`@PageAware(login=false)`**（匿名）或 **`login=true`**（需登录）注册 SPM 路径（§14）。

## 3.3 代码生成流程与三步开发（详细）

生成纪律与三步流程：**`docs/AI_STANDARDS.md` §11** + **`docs/AI_CODEGEN.md`**。

## 4. 相对路径最小示例

```md
请先读取：
- @../autumn/docs/AI_BOOT.md
- @../autumn/docs/AI_MAP.md
- @../autumn/docs/AI_STANDARDS.md
- （按需）@../autumn/docs/AI_POSTGRESQL.md / @../autumn/docs/AI_UPGRADE.md / @../autumn/docs/AI_CRYPTO.md / @../autumn/docs/AI_CODEGEN.md / @../autumn/docs/AI_TEMPLATES.md / @../autumn/docs/AI_GOVERNANCE.md / @../autumn/docs/AI_SECURITY.md / @../autumn/docs/AI_PROMPTS.md
- @./README.md
- @./<目标模块目录>

任务目标：
- <一句话>
```

## 5. 一句话工作流

每次提需求时固定：`BOOT -> MAP -> STANDARDS -> 按需专项（含 CODEGEN） -> 项目目录 -> 目标与约束`。
