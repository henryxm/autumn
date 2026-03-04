# Autumn AI 框架能力地图

> 目标：给 AI 提供统一、结构化的框架上下文，减少“不了解项目能力”导致的误判与重复实现。

## 1. 项目结构（AI 先看）

- `autumn-lib`：框架基础能力（缓存、队列、加解密、通用服务抽象）。
- `autumn-modules`：业务模块与控制器实现（sys/gen/job/db/oauth/usr/oss/lan/spm/wall 等）。
- `autumn-web`：启动与页面入口。

## 2. 核心能力索引（按开发高频）

### 2.1 缓存体系

- 核心类：
  - `cn.org.autumn.service.CacheService`
  - `cn.org.autumn.service.BaseCacheService`
  - `cn.org.autumn.service.ShareCacheService`
  - `cn.org.autumn.modules.sys.controller.CacheController`
- 关键点：
  - 两级缓存：EhCache + Redis。
  - `getCache*` 未命中自动调用 `getEntity*` 回源。
  - 默认回源支持注解+反射，业务可覆盖 `getEntity/getNameEntity`。
  - 失效同步通道：`cache:invalidation`（Redis Pub/Sub）。

### 2.2 队列体系

- 核心类：
  - `cn.org.autumn.service.QueueService`
  - `cn.org.autumn.service.BaseQueueService`
  - `cn.org.autumn.modules.sys.controller.QueueController`
- 关键点：
  - 队列类型：`MEMORY` / `REDIS_LIST` / `REDIS_STREAM` / `DELAY` / `PRIORITY`。
  - 支持延迟、定时、优先级、批量发送。
  - 支持自动消费者启停（空闲超时）。
  - 支持重试、死信、历史消息运维。

### 2.3 混合加解密（RSA + AES）

- 核心类：
  - `cn.org.autumn.modules.oauth.controller.RsaController`
  - `cn.org.autumn.modules.oauth.resolver.EncryptArgumentResolver`
  - `cn.org.autumn.modules.oauth.interceptor.EncryptInterceptor`
  - `cn.org.autumn.service.RsaService`
  - `cn.org.autumn.service.AesService`
- 关键点（务必准确）：
  - 请求解密不是全量触发：仅当请求体包含 `ciphertext + session` 时解密。
  - 响应加密不是全量触发：仅当请求头有 `X-Encrypt-Session` 时进入加密流程。
  - 返回兼容增强：若返回体不是 `Encrypt` 但为 JSON，拦截器会按返回类型规则决定包装后再加密（`Response`/`CompatibleResponse`/`DefaultEncrypt`）。
  - `@Endpoint(compatible=true)` 表示“支持兼容加密”，不直接决定包装与否；包装形态由声明返回类型决定。
  - 无 `X-Encrypt-Session` 时一律不加密并返回原始值；若实际返回为 `CompatibleResponse`，会解包为 `data` 返回（旧客户端兼容）。
  - `/rsa/*` 接口在响应侧排除加密（避免密钥交换循环加密）。
  - 非 JSON 响应（文件流/文本）不会做自动包装，保持原语义。
  - `@Endpoint(force=true)` 接口缺少 `X-Encrypt-Session` 时返回 `FORCE_ENCRYPT_SESSION_REQUIRED`。

### 2.4 加密兼容方案（跨项目复用）

- 核心目标：
  - 同一接口同时兼容“新加密协议（`Request<T>`）”和“旧明文协议（平铺字段）”。
  - 通过统一拦截与解析，避免拆分双接口。
- 推荐组合（最终收敛）：
  - 入参：`CompatibleRequest<T>`
  - 出参：`CompatibleResponse<T>`（需要显示包装时）或原始 DTO（不包装返回时）
- 关键类：
  - `cn.org.autumn.model.CompatibleRequest`
  - `cn.org.autumn.model.CompatibleResponse`
  - `cn.org.autumn.model.Wrap`
  - `cn.org.autumn.model.EndpointInfo`
  - `cn.org.autumn.modules.oauth.resolver.EncryptArgumentResolver`
  - `cn.org.autumn.modules.oauth.interceptor.EncryptInterceptor`
  - `cn.org.autumn.service.RsaService`（`getEncryptEndpoints` 计算 `wrap`）
- 适用边界（务必遵守）：
  - 如果请求参数类型已经是 `Encrypt` 接口实现类，则不需要做 `CompatibleRequest<T>` 兼容处理。
  - 如果返回参数类型已经是 `Encrypt` 接口实现类，则不需要做额外兼容性包装处理。
- `CompatibleRequest` 约定：
  - 不再使用 `legacy/resolve`；业务侧统一通过 `request.getData()` 取值。
  - `EncryptArgumentResolver` 负责归一化：标准 `{"data":...}` 取 `data`，非标准对象/数组/基础类型直接写入 `data`。
- `wrap` 参数（端点能力探测）：
  - `wrap.request=true`：请求参数是 `Request`/`CompatibleRequest` 包装。
  - `wrap.response=true`：返回类型是 `Response`/`CompatibleResponse` 包装。
- 行为矩阵（给 AI 的判断规则）：
  - `CompatibleRequest<T> + CompatibleResponse<T>`：有 session 时加密整个兼容包装；无 session 时解包返回 `data`（推荐）。
  - `CompatibleRequest<T> + Response<T>`：请求可明文/密文兼容；响应按 header 决定是否加密（通用可用）。
  - `CompatibleRequest<T> + 老返回对象`：若带 `X-Encrypt-Session` 且为 JSON，会按兼容规则加密；无 session 直接返回原值。
  - 任意请求 + 文件下载/非 JSON 返回：不自动包装，不做强制加密包装。
  - `请求或返回已实现 Encrypt`：保持原有入参与出参类型，不新增兼容层。
- 控制器最小模板（跨项目直接套用）：

```java
@PostMapping("/biz/action")
public CompatibleResponse<BizVO> action(@RequestBody(required = false) CompatibleRequest<BizDTO> request) {
    BizDTO dto = request != null ? request.getData() : null;
    if (dto == null) {
        CompatibleResponse<BizVO> fail = new CompatibleResponse<>();
        fail.setCode(-1);
        fail.setMsg("非法请求");
        return fail;
    }
    return CompatibleResponse.ok(service.action(dto));
}
```

- 迁移顺序建议：
  - 先改高频写接口、敏感数据接口。
  - 再改普通读接口；下载导出接口最后单独核对 `Content-Type`。
  - 每批次都回归“明文请求 + 加密请求 + header 有/无”的组合。

### 2.5 接口式定时任务（LoopJob，常用推荐）

- 核心目标：
  - 用“接口周期”替代“字符串 cron”，降低配置错误率与维护成本。
  - 用统一管理接口完成任务启停、触发、统计、告警和分配。
- 核心类：
  - `cn.org.autumn.modules.job.task.LoopJob`
  - `cn.org.autumn.modules.job.controller.LoopJobController`
- 选型规则（给 AI 的硬约束）：
  - 固定周期任务：优先实现 `LoopJob.OneMinute/FiveMinute/...`。
  - 仅当时间规则复杂（如每月、节假日）时，才使用 `schedulejob + cronExpression`。
- 关键机制：
  - 周期接口覆盖：`OneSecond` 到 `OneWeek`。
  - 运行保护：`skipIfRunning` 防重入，`timeout` 超时观测，`maxConsecutiveErrors` 连续错误自动禁用。
  - 多节点分配：`assignTag` 配合 `server.tag` 控制任务归属。
  - 批量性能：支持并行执行开关与 overrun（批量耗时超间隔）监控。
- 最小模板（跨项目可直接套用）：

```java
@Component
@JobMeta(
    name = "Demo Job",
    skipIfRunning = true,
    timeout = 15000,
    maxConsecutiveErrors = 5
)
public class DemoJob implements LoopJob.OneMinute {
    @Override
    public void onOneMinute() {
        // business logic
    }
}
```

- 验证接口（至少覆盖）：
  - `GET /job/loop/list?category=OneMinute`
  - `POST /job/loop/trigger`
  - `GET /job/loop/stats`
  - `GET /job/loop/alerts`

### 2.6 Handler 扩展机制

- 核心目录：`autumn-handler`
- 关键点：
  - 用接口扩展主流程，模块与框架解耦。
  - 常见接口：页面、拦截器、解析器、插件、过滤链等。
  - 常见模式：默认实现 + `@ConditionalOnMissingBean` + `@Order`。

## 3. 模块能力总览（业务域）

- `sys`：用户/角色/菜单/部门/配置/日志/系统运维能力。
- `gen`：代码生成（表 -> 代码）。
- `job`：定时任务管理与执行日志（优先 `LoopJob` 接口周期，复杂时间规则再使用 cron）。
- `db`：数据库备份与恢复。
- `oauth/client`：认证授权与客户端管理。
- `usr`：用户域开放能力与 token。
- `oss`：对象存储与文件管理。
- `lan`：多语言资源管理。
- `spm`：超级位置模型与埋点统计。
- `wall`：防火墙策略（IP/URL/主机访问控制）。

## 4. 开发决策规则（给 AI 的硬约束）

- 能复用框架能力时，禁止重复造轮子（优先找 `Base*Service` / `*Service` 现有能力）。
- 改业务逻辑优先“覆盖扩展点”而非修改内核流程。
- 涉及缓存更新必须同步考虑失效策略（删除单值 + 列表缓存）。
- 涉及加解密必须先判断触发条件（header/请求体字段/接口排除）。
- 涉及兼容改造时，先判断请求/返回类型是否已实现 `Encrypt`；若已实现则禁止再做兼容包装。
- 涉及队列消费必须给出失败、重试、死信处理策略。
- 涉及定时任务时，优先接口式任务（`LoopJob.OneMinute/FiveMinute/...`），避免不必要的 cron 表达式。

## 5. AI 交互输入模板（建议每次需求都带）

将以下模板复制给 AI，并按任务填写：

```md
你是 Autumn 项目的开发助手。

任务目标：
- （一句话描述）

涉及模块：
- （如 sys / oauth / cache / queue / job）

需要复用的框架能力（必须优先）：
- 缓存：BaseCacheService / CacheService / ShareCacheService（若需要）
- 队列：BaseQueueService / QueueService（若需要）
- 加密：EncryptArgumentResolver / EncryptInterceptor / RsaService / AesService（若需要）
- 协议兼容：CompatibleRequest + Response（同接口支持明文/密文）
- 定时任务：LoopJob 接口周期（固定周期优先，复杂规则再 cron）

约束：
- 不要新增重复基础设施
- 不要破坏现有接口返回结构
- 保持权限、缓存、加密、队列语义一致
- 涉及接口改造时，优先采用 CompatibleRequest<T> + Response<T> 组合
- 涉及定时任务时，优先 LoopJob 接口，不要默认写 cronExpression

输出要求：
- 先给实现方案（涉及类、接口、复用点）
- 再给代码改动清单
- 最后给测试与回归检查点
```

## 6. 文档入口（已有在线文档）

- 在线文档目录模板：`autumn-modules/src/main/resources/templates/modules/docs/`
- 推荐先阅读章节：
  - `architecture` / `handler`
  - `cache` / `queue`
  - `oauth` / `hybrid-crypto`
  - `sys`

## 7. 维护约定（建议）

- 新增框架能力时，同步更新：
  - 本文件（AI 能力地图）
  - 对应在线章节（`modules/docs/*.html`）
- 每个能力至少补齐：
  - 功能入口
  - 触发条件/约束
  - 最小可跑通用例
  - 常见故障排查

## 8. 多项目使用入口

- 如果你在 `Idea` 目录下有多个 Autumn 系项目，请先阅读：
  - `AI_GUIDE.md`
- 推荐在提示词中固定引用：
  - `@/Users/mac/Idea/autumn/AI_MAP.md`
  - 再补当前业务项目路径上下文（README + 目标模块目录）
