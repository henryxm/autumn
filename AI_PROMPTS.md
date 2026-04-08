# Autumn AI 提示词模板库（按需加载）

> 用途：集中存放可复制提示词。  
> 日常开发中仅在“要快速起任务/统一提问格式”时加载。

## 1. 最小可用模板（通用）

```md
请先阅读这些上下文再实现：
- @../autumn/AI_BOOT.md
- @../autumn/AI_MAP.md
- @../autumn/AI_STANDARDS.md
- （接口加解密改造）@../autumn/AI_CRYPTO.md
- （模块新建/生成链路）@../autumn/AI_TEMPLATES.md
- （规范治理/多人协作）@../autumn/AI_GOVERNANCE.md
- （仅安全专项任务需要）@../autumn/AI_SECURITY.md
- @./README.md
- @./src  （或对应后端目录）

需求目标：
- <一句话>

实现约束：
- 必须优先复用 Autumn 框架能力，不允许重复造轮子
- 不破坏现有接口与权限语义
- 涉及缓存/队列/加密时必须遵循框架默认流程
- 遵守 `AI_STANDARDS.md`（分层、API、gen 隔离、定时任务、`@RequiresPermissions`、FTL、**禁止 DDL .sql**、**Dao 只用 Provider**、**Controller 禁用 Dao**、**statics/pages/Site**）
```

## 2. 标准提问模板

### 2.1 新增业务功能

```md
你是我的 Autumn 架构开发助手。

先阅读：
- @../autumn/AI_BOOT.md
- @../autumn/AI_MAP.md
- @../autumn/AI_STANDARDS.md
- （接口加解密改造）@../autumn/AI_CRYPTO.md
- （模块新建/生成链路）@../autumn/AI_TEMPLATES.md
- （仅安全专项任务需要）@../autumn/AI_SECURITY.md
- @./README.md
- @./<模块目录>

现在要实现：
- <功能目标>

请按以下顺序输出：
1) 先给实现方案（复用哪些 Autumn 能力、涉及哪些类）
2) 再改代码（最小改动）
3) 最后给测试清单和回归风险

强约束：
- 不重复实现已有基础能力
- 缓存/队列/加解密必须走框架既有机制
- 若发现需求与框架冲突，先提出冲突点与可选方案，不要硬改内核
- 遵守 `AI_STANDARDS.md` 应用层规范
```

### 2.2 修 Bug

```md
先读取：
- @../autumn/AI_BOOT.md
- @../autumn/AI_MAP.md
- @../autumn/AI_STANDARDS.md
- （规范治理/多人协作）@../autumn/AI_GOVERNANCE.md
- （仅安全专项任务需要）@../autumn/AI_SECURITY.md
- @./<报错相关目录>

问题现象：
- <错误日志/复现步骤>

要求：
- 优先定位是否违反 Autumn 既有机制（缓存失效、队列重试、加密触发条件等）
- 给出 root cause
- 最小修复 + 回归点
```

## 3. 专项提示词

### 3.1 加解密兼容改造

```md
涉及 Autumn 加解密兼容改造时，请严格按以下约束实现：

- 入参兼容统一使用 `CompatibleRequest<T>`，业务侧直接使用 `request.getData()`。
- 参数解析由 `EncryptArgumentResolver` 统一归一化：
  - 解密后是标准 `{"data": ...}` 时，取 `data` 写入 `CompatibleRequest.data`
  - 解密后是对象/数组/基础类型时，直接作为 `CompatibleRequest.data`
- 出参优先使用 `CompatibleResponse<T>`（其继承 `Response` 并实现 `Encrypt`）：
  - `X-Encrypt-Session` 不为空：按整个 `CompatibleResponse` 加密返回
  - `X-Encrypt-Session` 为空：一律不加密并返回原始值；若返回体是 `CompatibleResponse`，则解包为 `data`
- `@Endpoint(compatible=true)` 仅表示“支持兼容加密”，不直接决定包装形态；是否包装由接口返回类型（`Response`/`CompatibleResponse`/原始DTO）决定。
- 端点能力探测依赖 `EndpointInfo.wrap`：
  - `wrap.request=true`：请求是 `Request`/`CompatibleRequest` 包装
  - `wrap.response=true`：返回是 `Response`/`CompatibleResponse` 包装
  - 客户端可据此模板化请求/响应数据映射
```

### 3.2 定时任务改造

```md
请优先按 Autumn 的接口式定时任务实现，不要先写 cron。

要求：
- 固定周期任务使用 LoopJob 接口（如 OneMinute/FiveMinute）
- 用 @JobMeta 配置 skipIfRunning、timeout、maxConsecutiveErrors
- 多节点场景给出 assignTag/server.tag 建议
- 给出 /job/loop/list /stats /alerts /trigger 的验证步骤
- 仅当时间规则确实复杂时，才允许回退到 schedulejob + cronExpression
```
