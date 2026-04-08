# Autumn AI 文档治理与协作约定（按需加载）

> 用途：仅在“规范整理、多项目协作、文档治理”任务时加载。  
> 默认不作为日常开发首轮上下文。

## 1. 文档入口（在线）

- 在线文档目录：`autumn-modules/src/main/resources/templates/modules/docs/`
- 推荐优先阅读：
  - `architecture` / `handler`
  - `cache` / `queue`
  - `oauth` / `hybrid-crypto`
  - `sys`

## 2. 维护约定

- 新增框架能力时，同步更新：
  - `AI_MAP.md`（核心能力地图）
  - 对应在线章节（`modules/docs/*.html`）
- 每个能力至少补齐：
  - 功能入口
  - 触发条件/约束
  - 最小可跑通用例
  - 常见故障排查

## 3. 多项目使用入口

- 多个 Autumn 系项目并行时，先阅读：`AI_GUIDE.md`
- 推荐提示词固定引用：
  - `@AI_MAP.md`
  - 再补当前业务项目路径上下文（README + 目标模块目录）

### 3.1 多项目模板资源打包约定（TemplateFactory）

- 每个项目都可独立提供模板资源并打入本项目 jar（默认放 `resources/templates`）。
- 每个项目至少提供一个 `TemplateFactory.Template` 实现类（建议放在本项目配置包）。
- 如模板根路径非默认 `/templates`，在实现类中覆写 `getBasePackagePath()`。
- 多项目同名模板冲突时，必须用 `@Order` 明确优先级（数值越小优先）。
- 新增项目禁止把模板手工拷贝到入口工程，统一通过模板加载链运行时聚合。
- `LoaderFactory.Loader` 仅保留兼容用途；新项目一律使用 `TemplateFactory.Template`。

## 4. 规则优先级与去重口径

- 规则优先级（高 -> 低）：
  - `AI_STANDARDS.md`：分层、API、gen 路由、定时任务、`@RequiresPermissions`、FTL、实体/注解建表/禁止 DDL、Dao+Provider、Controller–Service–Dao、statics/pages/Site
  - `AI_MAP: 0. AI 最小上下文`
  - `AI_MAP: 4. 开发决策规则`
  - `AI_MAP: 2.x 能力章节`
  - `AI_TEMPLATES: 模板库`
- 去重约定：
  - 多处冲突时，取“更具体且更新”的约束。
  - **应用层与数据访问纪律**（Controller/Service/Dao、SQL Provider、实体与库表、资源与 Site 等）以 **`AI_STANDARDS.md`** 为准；框架机制细节以 **`AI_MAP.md`** 为准。
  - 模板与能力冲突时，以能力语义约束为准。
  - 仍无法判断时，优先“复用现有基类/接口”的实现路径。
- 输出口径统一：
  - 每次实现包含：复用点说明、改动清单、回归清单。
  - 涉及缓存/队列/定时任务/加密任一能力时，必须给对应验证点。

## 5. 术语统一表

- 加密协议：
  - 请求包装：`Request<T>` / `CompatibleRequest<T>`
  - 响应包装：`Response<T>` / `CompatibleResponse<T>`
  - 加密能力接口：`Encrypt`
  - 接口元注解：`@Endpoint(hidden|force|compatible)`
- 任务调度：
  - 固定周期：`LoopJob.*`
  - 复杂日历：`cronExpression`（仅必要时）
- 后台权限注解：
  - `@RequiresPermissions`：仅**代码生成/管理端**链路；**新编写接口禁用**，普通用户接口用登录态鉴权（见 `AI_STANDARDS.md` §6）
- 生成链路：
  - 模板目录：`resources/template/*.vm`
  - 可重生层：`controller/gen/*`
  - 可维护层：`controller/*`、`service/*`
- 库表与 SQL：
  - 结构演进：实体注解 + `autumn.table.*`，**禁止**常规随仓 `DDL .sql`（见 `AI_STANDARDS.md` §8）
  - 自定义 SQL：**Provider**（`*Sql` / `@SelectProvider`），禁止 Dao 方法上内联 SQL 字符串（新代码，`AI_STANDARDS.md` §12）
- 静态与页面：
  - 公共静态：`resources/statics/`（匿名可访问，优先复用框架已有）
  - 后台页：模块下 `pages` + `site/*Site.java` 字段 `@PageAware`（`AI_STANDARDS.md` §14）

## 6. 推荐阅读路径（按角色）

- AI/Agent 首次接入：`AI_MAP 0 -> 4 -> 8 -> 9`
- 后端开发：`AI_MAP 2.4 -> 2.7 -> 2.8 -> 6 -> 7`
- 前端/客户端开发：`AI_MAP 2.4 -> 8`
- 平台维护者：`AI_MAP 1 -> 2 -> 本文 2/4/5`

## 7. 精简维护规则（防膨胀）

- 新内容优先归并到已有章节，避免平行规则。
- 同一规则出现 2 处以上时，仅保留“最具体一处”，其他改引用。
- 模板解释放 `AI_MAP 2.x`，可复制指令放 `AI_TEMPLATES.md`。
- 每次大改后检查：
  - 编号是否连续
  - 术语是否统一（Request/Response、LoopJob、ModuleService）
  - 是否引入冲突约束

## 8. 更新记录约定

```md
- YYYY-MM-DD：更新章节 {章节号}
  - 变更：{一句话}
  - 影响：{AI 执行口径/开发流程/模板库}
```
