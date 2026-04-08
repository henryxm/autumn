# Autumn 模块任务模板库（按需加载）

> 用途：仅在“模块新建/批量生成/跨模块组合”时加载。  
> 默认不作为日常开发首轮上下文。

## 1. 使用说明

- 先复制对应模块模板，再替换 `{}` 占位符。
- 需求跨模块时，先选主模板，再补从模板约束。
- **实体扫描与注解建表、禁止常规 DDL `.sql`、Dao 仅经 Provider 写 SQL、gen/Pages/html/js 不可改、`statics`/`pages`/`Site`/`@PageAware` 等**：以 **`AI_STANDARDS.md`** §8～§14 为强制约束，模板任务不得与之冲突。

## 2. 模块任务指令模板

### 2.1 sys 模块模板（权限/菜单/配置/字典）

```md
你正在开发 Autumn 的 sys 模块能力。

任务目标：
- {一句话目标}

涉及对象：
- 实体：{Entity}
- 接口：{path}
- 页面：modules/sys/{page}

必须复用：
- Service 继承 ModuleService
- 分页与查询：BaseService.queryPage/getCondition
- 缓存：getCache/getListCache + 写后 removeCacheAll
- 菜单与语言：init() 自动注册（不要手写平行初始化器）

输出要求：
- 给出 controller/service/dao/entity/site 变更清单
- 给出权限串（sys:{resource}:list/info/save/update/delete）
- 给出回归点（权限、菜单、缓存失效、分页查询）
```

### 2.2 oauth 模块模板（混合加密/兼容协议）

```md
你正在开发 Autumn 的 oauth 模块接口，必须遵循混合加密标准。

任务目标：
- {一句话目标}

接口模式：
- 默认：Request<T> -> Response<R>
- 兼容：CompatibleRequest<T> -> CompatibleResponse<R> + @Endpoint(compatible=true)

必须复用：
- 握手入口：/rsa/api/v1/init（仅握手，不混入业务）
- 能力探测：/rsa/api/v1/endpoints
- 请求解密：EncryptArgumentResolver
- 响应加密：EncryptInterceptor
- 安全特征：init 返回 agent/auth，客户端附加 User-Agent（或 X-Encrypt-Agent）+ X-Encrypt-Auth
- 防重放签名：X-Encrypt-Timestamp + X-Encrypt-Nonce + X-Encrypt-Signature

强约束：
- 不新增平行加密协议字段结构
- 非文件流接口默认按 JSON 包装与加密规则
- 必须提供明文/密文/header有无三组回归点
- 强力模式下补充 403 校验路径回归（特征缺失/不匹配）
```

### 2.3 usr 模块模板（用户域/资料/token/open）

```md
你正在开发 Autumn 的 usr 模块用户域能力。

任务目标：
- {一句话目标}

涉及对象：
- 实体：{Entity}
- 服务：{Service}
- 登录态或身份：{是否涉及}

必须复用：
- Service 继承 ModuleService
- 用户相关缓存：getCache/getShareCache（跨端资料优先考虑 share）
- 异步事件：BaseQueueService（登录日志、行为采集等）
- 周期整理任务：LoopJob.*（如过期清理、同步刷新）

输出要求：
- 给出数据一致性策略（DB/缓存/队列）
- 给出安全边界（越权、会话、敏感字段）
- 给出回归点（登录态、缓存一致性、异步补偿）
```

### 2.4 job 模块模板（周期任务治理）

```md
你正在开发 Autumn 的 job 模块任务能力。

任务目标：
- {一句话目标}

任务类型：
- 固定周期：LoopJob.{OneSecond|...|OneWeek}
- 复杂日历：仅在必要时使用 cron，并说明原因

必须复用：
- @JobMeta(skipIfRunning, timeout, maxConsecutiveErrors, assign)
- LoopJob 统计与告警接口（list/stats/alerts）
- 任务逻辑放 Service，不新建平行调度器

输出要求：
- 给出任务周期、超时、重入、连续失败策略
- 给出多节点分配策略（server.tag / assignTag）
- 给出压测/观测点（执行耗时、overrun、错误率）
```

### 2.5 gen 模块模板（实体驱动全自动生成）

```md
你正在开发 Autumn 的 gen 模块代码生成链路，目标是“实体驱动建表 + 模板生成 + 可扩展落地”。

任务目标：
- {一句话目标}

输入：
- 模块名：{moduleName}
- 生成方案ID：{genId}
- 实体：{Entity}（含 @Table/@Column）
- 表前缀：{prefix}
- 目标表：{tableNames}

必须复用：
- 建表链路：TableInit -> MysqlTableService（启动或重置后自动建/对表）
- 生成入口：/gen/generator/code + GeneratorService + GenUtils
- 模板目录：autumn-modules/src/main/resources/template/*.vm
- 模板输出：entity/dao/service/controller/controller/gen/site + templates/modules/{module}/*.html|*.js

模板一致性约束：
- 若模板缺失，先补齐：Entity/Dao/Service/Controller/ControllerGen/Site/Menu/list.html/list.js
- 不改已有占位符语义（className/moduleName/pathName/columns/pk等）
- 生成后业务逻辑只能落在非 gen 层（controller/service），避免再次生成覆盖

输出要求：
- 先给“将生成文件清单”（按路径）
- 再给“模板/生成链路是否需要补齐”的判断
- 再给“生成后业务扩展点”与“不可放置点（gen层）”
- 最后给回归点（建表、下载zip、解压结构、菜单入口、页面访问、权限串）
```

### 2.6 跨模块组合模板（多能力并发场景）

```md
你正在处理跨模块需求：{主模块} + {从模块列表}。

任务目标：
- {一句话目标}

能力组合：
- 数据访问：ModuleService + BaseService.queryPage
- 缓存：BaseCacheService/ShareCacheService
- 异步：BaseQueueService
- 周期任务：LoopJob.*
- 加密协议：Request/Response 或 Compatible*

执行顺序：
- 1) 先确定主模块与实体边界（避免职责漂移）
- 2) 再确定缓存键、队列消息体、周期任务触发点
- 3) 最后定义接口签名与兼容策略

输出要求：
- 给出模块职责分割表
- 给出事务边界与最终一致性方案
- 给出失败补偿策略（缓存回滚/重试/死信/任务兜底）
```

## 3. 快速索引（按场景秒选）

- 系统配置/权限/菜单：`2.1 sys`
- 加密协议/开放接口：`2.2 oauth`
- 用户资料/账号体系：`2.3 usr`
- 周期任务治理：`2.4 job`
- 实体驱动代码生成：`2.5 gen`
- 跨模块组合：`2.6 跨模块组合模板`

## 4. 组合复制示例

```md
请按 Autumn 模板库执行开发：
- 主模板：2.5 gen 模块模板
- 从模板：2.2 oauth 模块模板
- 组合规则：2.6 跨模块组合模板

任务目标：
- {一句话}

补充约束：
- 必须复用 ModuleService 继承链
- 必须给出明文/密文/header有无回归点
- 业务逻辑不得落在 controller/gen 可重生层
```
