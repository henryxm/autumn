# Autumn 多项目 AI 开发指南（导航版）

> 目标：在多项目场景下，用最小上下文让 AI 快速进入正确执行路径。
> 统一索引入口：`@<autumn-root>/AI_INDEX.md`

## 1. 推荐喂给顺序（3 层）

1. **框架层（固定）**
   - 首轮必读：`@<autumn-root>/AI_BOOT.md`
   - 核心能力：`@<autumn-root>/AI_MAP.md`
   - 应用层强制规范：`@<autumn-root>/AI_STANDARDS.md`
   - 按需追加：`@<autumn-root>/AI_POSTGRESQL.md` / `@<autumn-root>/AI_UPGRADE.md` / `@<autumn-root>/AI_CRYPTO.md` / `@<autumn-root>/AI_CODEGEN.md` / `@<autumn-root>/AI_TEMPLATES.md` / `@<autumn-root>/AI_GOVERNANCE.md` / `@<autumn-root>/AI_SECURITY.md` / `@<autumn-root>/AI_PROMPTS.md`
2. **当前项目层（变化）**
   - 当前项目 `README`、模块目录、关键配置
3. **当前任务层（变化）**
   - 目标 + 约束 + 验收标准 + 禁止事项

## 2. 文档分工（避免全量加载）

- `AI_BOOT.md`：最小启动上下文（首轮固定）
- `AI_MAP.md`：高频开发能力主图
- `AI_STANDARDS.md`：约束性开发规范（分层、API、定时任务、权限、FTL、实体与库表、Dao/Provider、资源与 Site）
- `AI_POSTGRESQL.md`：PostgreSQL 支持与多库兼容（配置、方言、类型、分页 count、迁移）
- `AI_UPGRADE.md`：依赖方升级清单、`scripts/autumn-dependency-scan.sh` 与「一键升级」可执行边界
- `AI_CRYPTO.md`：接口加解密兼容专项
- `AI_SECURITY.md`：签名/灰度/演练专项
- `AI_CODEGEN.md`：代码生成链路（gen、GenUtils、模板、库表反射）、推荐三步开发流程、`BaseCacheService` / `ShareCacheService` / `BaseQueueService` 能力说明
- `AI_TEMPLATES.md`：模块任务模板库
- `AI_GOVERNANCE.md`：治理、术语、协作约定
- `AI_PROMPTS.md`：可复制提问模板

## 3. 常用加载组合

- 日常开发：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md`
- 接口加解密改造：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_CRYPTO.md`
- 模块新建/代码生成：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_CODEGEN.md + AI_TEMPLATES.md`
- 多项目模板整合（TemplateFactory）：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_GOVERNANCE.md`
- 安全改造/攻防演练：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_SECURITY.md`
- 多人协作/规范治理：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_GOVERNANCE.md`
- PostgreSQL / 多库适配：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_POSTGRESQL.md`
- 升级 autumn 基线版本：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_UPGRADE.md`
- 快速发起任务：在以上组合基础上追加 `AI_PROMPTS.md`

## 3.1 代码生成模板边界（MVC 约束）

- 可重生（默认不改）：`ControllerGen.java`、`*Pages.java`、`list.html`、`list.js`（由模板重生成）。
- 可维护（业务落点）：`Controller.java`、`Service.java`、`Site.java`、`Menu.java`。
- 先判定修改类型：
  - 单模块业务需求 -> 改可维护层；
  - 框架级批量行为统一 -> 改 `template/*.vm` 后重生成。
- 向 AI 下达任务时应显式声明：`禁止修改 gen 层` 或 `允许修改模板并重生成`，避免误改被覆盖。

## 3.2 应用层开发规范（约束性）

完整条文见 **`AI_STANDARDS.md`**。摘要：

- **高内聚、低耦合**：业务流程在实体对应的 **Service**；HTTP 入参出参与调用链在实体对应的 **可维护 Controller**；子项目间优先 **接口 / 服务调用**，跨服务编排放在 **当前项目 Service**。
- **对内管理端接口**：不得与 **`ControllerGen` 等生成控制器**产生 **相同请求映射** 或易混淆的 **同名映射方法**；自定义接口使用 **独立 URL 前缀**。
- **对外 API**：**独立 Controller**；统一 **`Request<T>` / `Response<T>`**（兼容用 `Compatible*`）；接入框架 **应用层加解密**（见 `AI_CRYPTO.md`）。
- **定时任务**：生产代码 **禁止 `@Scheduled`**；使用 **`LoopJob.*` + `@JobMeta`** 或框架 **`schedulejob` + cron**。
- **`@RequiresPermissions`**：仅代码生成/后台管理语义；**新接口一律不用**，普通用户接口用**登录态**鉴权（见 `AI_STANDARDS.md` §6）。
- **FreeMarker 页面**：避免与 FTL 冲突；条件等用 **`<!-- -->`** 包裹以保持 HTML 规范；**`<script>` 内插值**须保证渲染后 JS 合法，必要时注释/拆脚本/`<#noparse>`（见 `AI_STANDARDS.md` §7）。
- **实体与库表**：框架扫描实体并依开关**自动建表/更新**；**禁止**常规 **`DDL .sql`** 初始化脚本（§8）。模块目录名 = 包段 = **表前缀**，**勿**作实体类名前缀（§9）。表/字段 **`comment`** 用 **半角 `:`** 分隔短标题与说明；**勿**同列叠 `@Index` 与 `@Column(isUnique=true)`；整型/布尔优先**基本类型 + 默认值**（§10）。
- **生成层**：**gen**、`SitePages` 生成的 **Pages/html/js** **不修改、不加逻辑**；在生成给出的**空壳** `Controller/Service/Dao` 中实现（§11）。
- **SQL**：新代码 **禁止** Dao 上内联 SQL，**必须** **Provider**（§12）；多库见 `AI_POSTGRESQL.md`。
- **调用链**：**Controller** 只用 **Service**；**Service** 用 **`baseMapper`**，**勿**再注入本 Dao；跨域 **调别的 Service**，**勿**用他域 **Dao**（§13）。
- **资源**：**`statics/`** 公共静态、匿名可访问、优先复用框架资源；新后台页放模块 **`pages`**；**`site/*Site`** 字段上 **`@PageAware(login=false)`**（匿名）或 **`login=true`**（需登录）注册 SPM 路径（§14）。

## 3.3 代码生成流程与三步开发（详细）

端到端链路（`GeneratorService`、`TableDao` 反射、`GenUtils`、Velocity 模板）、**优先后台生成 ZIP**、AI 生成须与生成器目录一致、以及 **`@Cache` / 基类缓存队列** 用法：见 **`AI_CODEGEN.md`**。

## 4. 相对路径最小示例

```md
请先读取：
- @../autumn/AI_BOOT.md
- @../autumn/AI_MAP.md
- @../autumn/AI_STANDARDS.md
- （按需）@../autumn/AI_POSTGRESQL.md / @../autumn/AI_UPGRADE.md / @../autumn/AI_CRYPTO.md / @../autumn/AI_CODEGEN.md / @../autumn/AI_TEMPLATES.md / @../autumn/AI_GOVERNANCE.md / @../autumn/AI_SECURITY.md / @../autumn/AI_PROMPTS.md
- @./README.md
- @./<目标模块目录>

任务目标：
- <一句话>
```

## 5. 一句话工作流

每次提需求时固定：`BOOT -> MAP -> STANDARDS -> 按需专项（含 CODEGEN） -> 项目目录 -> 目标与约束`。
