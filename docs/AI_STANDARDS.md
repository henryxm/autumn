# Autumn 应用层开发规范（约束性）

> 用途：约定**业务与数据访问分层、实体与库表、生成层边界、资源与页面**等纪律。  
> 与 `docs/AI_BOOT.md`、`docs/AI_MAP.md` 互补：**多库 SQL、Wrapper 与 Provider 落地以 `docs/AI_DATABASE.md` 为准**；**PostgreSQL 专项实现见 `docs/AI_POSTGRESQL.md`**；**团队强制口径以本文为准**。

## 1. 适用范围

- Autumn 本仓库各模块。
- 基于 Autumn 的**业务子项目 / 多模块工程**（含仅依赖 starter 的下游仓库）。

## 2. 架构原则（必须遵守）

### 2.0 代码版式与 Lombok

- Java **方法签名、调用实参、if 条件**的换行约定见 **`docs/AI_CODE_STYLE.md`**（单条分支可无 `{}` 但须换行；与 Checkstyle 冲突时以该文档与本文为准）。
- **import**：除非类名冲突，**禁止**在业务代码中用全限定类名代替 `import`；见 **`docs/AI_CODE_STYLE.md` §7**。**PR CI**：`bash scripts/check-java-fqn`（白名单 `scripts/fqn-allowlist.txt`）。
- **实体 Getter/Setter（硬性）**：表实体（`*Entity`）**必须**使用 Lombok **`@Getter` + `@Setter`**（与 **`Entity.java.vm`** / 现有实体一致），**禁止**手写 getter/setter。  
  - **不要**对表实体使用 **`@Data`**（避免生成基于可变字段的 `equals`/`hashCode`，干扰持久化与集合语义）。  
  - 对外契约类（`*Request` / `*VO`）可用 **`@Getter`/`@Setter`** 或 **`@Data`**；新代码优先与同模块既有风格一致。
- **日志（硬性）**：需要打日志的类（Service / Controller / Component 等）**必须**使用 Lombok **`@Slf4j`**，通过 **`log`** 字段输出；**禁止**手动 `LoggerFactory.getLogger` / 手写 `Logger` 字段。  
  - `log.info` / `log.debug` 等**必须单行**，禁止把日志调用拆行；消息语言与其它细则见 **`docs/AI_CODE_STYLE.md` §8**。  
  - **级别语义**：**`info`** = 业务里程碑（**禁止**堆栈与 PII）；**`debug`** = 排障细节（**禁止**密码/令牌明文）；**`warn`** = 可恢复降级或预期内拒绝；**`error`** = 须跟进的失败。

### 2.1 高内聚、低耦合（业务逻辑落 Service）

- **实体相关业务逻辑必须落在对应 Service（硬性，默认优先）**：凡与某实体（表 / 领域模型）相关的**业务流程、校验规则、状态机、事务边界、缓存失效、队列投递、`LoopJob` 定时任务、对外 API 的领域处理**，**必须尽量**实现于该实体对应的 **`XxxService`**（跨实体用例可放 Facade，但仍**只调 Service**）。  
  - **`XxxService` 必须继承 `ModuleService`**（含字段加密时用 **`EncryptModuleService`**，见 `docs/AI_MAP.md` §2.7、**`docs/AI_FIELD_ENCRYPT.md`**）。  
  - **禁止**把上述逻辑写在 Controller、ControllerGen、Dao、工具类「壳」或平行 `*Helper` / `*Manager`（详见 **`docs/AI_SERVICE_COHESION.md`**）。  
  - **禁止**仅为定时任务/清理再建平行 Service。  
  - **Controller 只做**：鉴权标注、入参 `@Valid`、调用 Service、包装 `Response`；**禁止**在 Controller 内解析松散 `Map`、拼装业务字段或写状态流转。
- **低耦合**：模块之间避免直接依赖对方**具体实现类**；需要协作时优先 **接口契约**、**事件**、或团队已选型的**远程/本地服务调用**（如 Dubbo、Facade Bean 等），由项目架构统一选型，不在此重复实现细节。
- **类规模**：单实体 Service 实现类（不含 gen）**建议上限约 1000 行**；超过时须拆 Facade / 领域私有方法，**禁止**平行影子 Service（见 **`docs/AI_SERVICE_COHESION.md`** §2、§7）。开发期重构须**删除悬空类与未引用代码**。

### 2.2 分层职责

| 层级 | 职责 | 约束 |
|------|------|------|
| **Controller（可维护层）** | HTTP 入参校验与组装、调用 Service、返回视图或统一响应包装 | 对内接口写在实体 **`*Controller`**（§3）；对外写在独立 Open/Api Controller（§4）；**禁止**承载实体业务规则/事务/状态机；**禁止**引用 **Dao**（§13） |
| **Service（业务唯一落点，持有 Dao）** | 该实体域内**全部**业务流程、缓存/队列/任务与领域编排 | **必须** `extends ModuleService`（或 `EncryptModuleService`）；**唯一允许经 `baseMapper` 使用本实体 Dao**；**禁止**再注入本 Dao 或任何他域 Dao（§13） |
| **跨实体 / 跨模块编排** | 组合多个 Service 完成用例 | Facade/Application Service **只注入 Service**；**禁止**直连 Dao；**禁止**在 Controller 堆叠业务逻辑 |

### 2.3 子项目之间协作

- **尽量减少耦合**：不直接引用对方模块内部包路径下的实现细节；通过**暴露的 API 接口**、**共享 API 模块**、或**注册的服务接口**交互。
- **聚合多个下游能力**：在**当前子项目**中增加专门的 Service 层编排类，而不是在多个子项目的 Controller 之间互相调用。
- **框架仓 vs 业务仓（硬性）**：**领域实体 / 业务表**放在**业务工程**模块；**Autumn 框架仓**（`autumn-modules` 等）仅承载通用框架模块。**禁止**在框架仓堆叠业务方专属领域表；业务仓依赖锁定的 Autumn 版本，扫描脚本路径按 **`docs/AI_INDEX.md`**。

### 2.4 事务边界（`@Transactional`）

- **写路径**（插入/更新/删除/改状态）：在**实体 `*Service` 的公有业务方法**上声明 **`@Transactional(rollbackFor = Exception.class)`**（或项目统一等价配置）。
- **跨实体编排**：仅在 **Facade / Application Service** 方法上开事务；被编排的实体 Service 方法勿再各自乱开嵌套事务除非明确 `REQUIRES_NEW`。
- **只读查询**：**默认不加** `@Transactional`；确需只读一致性时用 `readOnly = true` 并注明原因。
- **禁止**在 Controller 上声明业务事务。

## 3. 对内接口（管理端 / 网页后台）

面向后台管理、内部页面的 HTTP 接口（非对外开放 API）须满足：

1. **实体可维护 Controller 形态（硬性）**  
   - 每个实体必须有 **`XxxController extends XxxControllerGen`**（见 §11）。  
   - 类级映射必须为 **`{module}/{entity名去 Entity 后全小写}`**（与生成器惯例一致），例如：

```java
@RestController
@RequestMapping("auth/scopedefinition")
public class ScopeDefinitionController extends ScopeDefinitionControllerGen {
}
```

   - **对内 / 页面调用**的新增接口**必须**写在该实体对应的 **`XxxController`** 中（方法级路径与 gen CRUD **不冲突**，见下条）。  
   - **禁止**把对外 Open API 写进实体 `*Controller` / `*ControllerGen`（对外见 §4）。

2. **不得与 gen 生成控制器冲突**  
   - **禁止**使用与 **`ControllerGen`**（或同模块代码生成产出的控制器）**相同的请求映射**（类级路径 + 方法级路径 + HTTP 方法组合导致与生成路由重复）。  
   - **禁止**在可维护 `Controller` 中使用与生成控制器中**已存在的、可能引起混淆的同名公开方法名**作为映射方法。  
   - 目的：避免路由覆盖、权限配置错误、Swagger/文档重复与线上排障困难。

3. **跨实体编排接口**：可另立 Facade Controller，**不得**冒充实体 CRUD 的 `{module}/{entitylower}` 路径。

## 4. 对外 API（Open / 第三方调用）

1. **独立控制器（硬性）**：对外 API **必须**使用**单独的 Controller 类**（如 `XxxApiController`、`XxxOpenApiController`），与对内实体 `*Controller` / `*ControllerGen` **分离**。按**能力分类**拆分多个 Controller；杂项可合并进一个 Controller。**禁止**把对外接口写进实体 ControllerGen。

2. **新开发 URL（硬性）**：自本规范生效起，**新编写**的对外 HTTP API **必须**为：

   ```text
   POST /{module}/api/v1/{action}
   ```

   - `{module}` = 模块短名；`{action}` = 动作名（建议小写或驼峰与现有模块文档一致）。  
   - **历史接口不动**（如既有 `bot/api/v1`、`safe/api/v1`、`/rsa/api/v1/init` 等）；新能力不得再发明平行前缀，除非列入团队**例外白名单**并文档登记。

3. **统一请求响应模型（硬性）**：凡业务侧 **手写** `@RestController`（路径含 **`/{module}/api/v1/**`**，含 open / host；以及运维 Facade 如 `/{module}/…manage` 等**非**实体 `*ControllerGen` 路由），请求体与成功响应体**必须**类型化。  
   - **新接口默认**：业务 API 用 **`Request<T>` / `Response<T>`**。  
   - **第三方 CA / Webhook 入站回调**：允许 **裸 `@RequestBody *Request`**（不强制外包 `Request<>`，因对端协议通常无 Autumn 信封），但仍须 **类型化模型** + 成功侧 **`Response<*VO>`**（或团队登记的固定 ACK）。  
   - **`CompatibleRequest` / `CompatibleResponse` + `@Endpoint(compatible=true)`** 仅允许用于**已登记迁移清单**中的存量兼容接口（见 `docs/AI_CRYPTO.md`、`docs/AI_MAP.md` §2.4）。  
   - **禁止** `@RequestBody Map<…>`、`Response<Map<…>>`、`Request<Map<…>>`、裸 `Map` 返回、用 `Map`/`HashMap` 现场拼装对外字段。  
   - **禁止**「先收 Map 再手工 `get("xxx")`」充当契约；第三方回调亦须定义 **`*Request` / 类型化 payload**，在 **Service** / Provider 内映射领域对象（嵌套 `data` 优先同结构嵌套对象；若对端「对象或 JSON 字符串」二态，允许用 **`JsonNode` 单字段**承接后**立即**反序列化为 `*Request`，**禁止**回退松散 `Map`）。  
   - **例外（窄）**：**仅**对内 Gen CRUD 的 `queryPage(Map params)` / `list(Map params)`（§3 / §11）允许 Map 查询参数；**不得**把该例外照搬到 `*/api/v1/**`、回调、Open API 或运维 Facade。

4. **契约类命名（硬性）**：入参模型后缀 **`*Request`**；出参模型后缀 **`*VO`**。**禁止**新接口以 `*DTO` 同时充当入参与出参，或混用无约定后缀。

5. **`Request` / `Response` 的业务载荷类型（硬性）**：泛型 **`T` 必须是**上述自定义类型化模型类（或 `List<*VO>` 等元素类型化集合）。  
   - **禁止**：`Map` / `Map<String,Object>` / `Map<String,String>` / `JSONObject` / `Object` / 原始 `String`（承载整段业务 JSON）/ 无界通配。  
   - **禁止**：把 **`*Entity` 直接作为对外契约的 `T`**（管理 Gen CRUD 对内接口除外，见 §3）。  
   - **允许（窄）**：无结构字段的确认结果可用 **`Boolean` / `Integer` 等标量**（如「是否成功」）；一旦有 ≥2 个业务字段须升级为 `*VO`。  
   - 参考：`docs/AI_ROBOT_API.md`（如 `RobotCreateRequest`）。

6. **入参校验（硬性）**：JSON 对外接口使用 **`@Valid @RequestBody Request<*Request>`**（或 `CompatibleRequest` 迁移场景）；校验失败走全局异常或统一 `Response` 错误码，**禁止**在 Controller 内散落手写 null 判断替代 Bean Validation。

7. **错误码（硬性）**：**禁止**新代码对外返回裸 **`setCode(-1)`** 作为业务失败语义。业务失败使用模块**错误码常量类**（段位在模块文档登记，如 safe 专篇）；`msg` 面向调用方可读。

8. **鉴权（硬性）**：对外**写接口禁止**整段 Shiro **`anon` 裸奔**；须登录态、API Token、签名验签等与模块文档一致的机制，并在方法或过滤器层显式校验。读接口若必须匿名，须在安全文档/白名单登记。

9. **应用层加解密**：对外接口须**接入框架应用层自动加解密语义**（请求体 `ciphertext`+`session`、响应头 `X-Encrypt-Session`、握手与业务分离等），**禁止**自造与框架并行的加解密协议。细节以 `docs/AI_MAP.md`、`docs/AI_CRYPTO.md` 为准。

## 5. 定时任务（禁止 `@Scheduled`）

- **禁止**在业务代码中使用 Spring **`@Scheduled`** 作为**生产环境**定时调度入口（避免脱离框架的任务治理、监控、多节点分配与统一启停）。

- **必须**使用 Autumn 任务体系，按场景二选一（详见 `docs/AI_MAP.md` §2.5）：  
  - **固定周期**：实现 **`LoopJob.OneSecond` … `OneWeek`** 中对应接口，配合 **`@JobMeta`**（`skipIfRunning`、`timeout`、`maxConsecutiveErrors`、`assign`、`duty` 等）。同一 Bean 可实现多个周期接口；`JobDuty` 类/方法合并与多接口标法见 **`docs/AI_CLUSTER_JOB_ORCHESTRATION.md` §1.3**。  
  - **`@JobMeta` 硬默认（新代码）**：周期 **≤1 分钟**（含秒级）的任务 **必须** `skipIfRunning = true`，且 **`timeout` 为正数**；多节点下会写库 / 改共享状态的任务须按集群专篇配置 **singleton / `JobDuty`**，**禁止**默认 `duty=ALL`  silently 多节点重复写。  
  - **复杂日历**：使用框架 **`schedulejob` + `cronExpression`**（及 `@TaskAware` / `@JobMeta` 等既有约定），而非裸 `@Scheduled`。

- 例外：仅允许在**本地调试、单元测试、非交付脚手架**中使用 `@Scheduled`，且不得合并入主分支交付配置。

## 6. 权限注解 `@RequiresPermissions`（约束）

- **定位**：`@RequiresPermissions` 出现在**代码生成框架**产出的后台管理链路中，用于 **Shiro 后台管理菜单/按钮级鉴权**，**不面向终端普通用户开放**的「管理端语义」。

- **新增接口（禁止）**：**所有新编写的 Controller 接口**（含对内业务 API、对外 API、可维护层手写接口）**不得**使用 **`@RequiresPermissions`** 做鉴权。  
  - **普通用户 / 已登录用户**可访问的接口：采用**登录态鉴权**（Session、Token、OAuth 等与项目一致的「已登录即可访问」机制），在 Service 或安全过滤器中按业务需要补充校验，**不**套用管理端权限码注解。

- **例外（须登记）**：仅当明确属于**与 gen 同构的后台管理扩展**，且在模块文档或 PR 说明中**书面登记例外原因**后，才允许沿用菜单权限模型；未登记则 **Review 必须拒绝**。

## 7. 页面模板与 FreeMarker（HTML / JavaScript）

默认页面经 **FreeMarker** 渲染。须避免与 FTL 冲突的写法，并保证**渲染后**的 HTML / JavaScript 语法合法、**用户可见文本无多余注释符号**。

**参考实现**：`autumn-modules/src/main/resources/templates/modules/client/webauthentication.html`（HTML 注释包裹）；`autumn-modules/src/main/resources/templates/login.html`（HTML + JS 分支约定）。

### 7.1 总原则

| 原则 | 说明 |
|------|------|
| **分域注释** | HTML 区块用 **HTML 注释**包裹 FTL；`<script>` 内用 **`//` 前缀**包裹 FTL 指令行；**禁止**在 JS 里写 `<!-- -->`。 |
| **指令独立行** | 除 §7.3 允许的**单行裸 FTL** 外，每个 `<!--<#if>` / `<!--<#else>` / `<!--</#if>` / `//<#if>` 等**独占一行**，分支内容在下一行，便于 Code Review 与 diff。 |
| **渲染结果优先** | 注释包裹会在输出中留下字面量 `<!--`、`-->`（如 `<!---->`）；**标题、单行纯文本**等用户可见处**禁止**使用该包裹方式。 |
| **说明性注释** | 普通 HTML/JS 说明用 `<!-- 说明 -->` 或 `// 说明`；**禁止**在说明文字里写 `${}`、`<#if>` 等 FTL 样例字符（会被 FTL 误解析）。 |
| **完全跳过 FTL** | 需原样输出大量 `$`/`#`/`<#` 时，用 **`<#noparse>...</#noparse>`**（§7.6）。 |

### 7.2 HTML 条件渲染：注释包裹（默认）

在 **HTML 标签体、块级结构、表单分支** 中做 `#if` / `#else` / `#list` 时，用 **HTML 注释**把 FTL 指令包起来，使源文件对 HTML 校验器/IDE **结构合法**；服务端仍**正常解析**注释内的 FTL。

**标准写法**（每个指令一行，参考 `webauthentication.html`）：

```html
<!--<#if oauthLogin?? && oauthLogin>-->
<form class="login-panel" method="post" action="${request.contextPath}/oauth2/login">
  ...
</form>
<!--<#else>-->
<div class="login-panel">
  ...
</div>
<!--</#if>-->
```

**嵌套与循环**（`#list` / 内层 `#if` 同样各占一行）：

```html
<!--<#if scope?? && scope?has_content>-->
<!--<#list scope?split(' ') as s>-->
<!--<#if s?has_content>-->
<span class="scope-tag">${s}</span>
<!--</#if>-->
<!--</#list>-->
<!--<#else>-->
<span class="scope-tag">基础访问</span>
<!--</#if>-->
```

**禁止**：

- 在 HTML 注释**说明文字**里写 `${}` 或 `<#if>` 字面量（会触发 `ParseException` 或空插值）。
- 把多个 FTL 指令与正文**挤在同一行**（可读性差，且 `#list` 难维护）。

### 7.3 HTML 例外：单行裸 FTL（无 HTML 注释包裹）

下列场景**必须**用**单行、不换行、无 `<!-- -->` 包裹**的裸 FTL，否则浏览器/标签页会显示 `<!---->` 等垃圾字符：

| 场景 | 写法 |
|------|------|
| **`<title>`** | `<title><#if cond>A<#elseif cond2>B<#else>C</#if></title>` |
| **其他仅输出纯文本、且不能有 HTML 注释残留的属性/节点** | 同上：单行裸 `<#if>`，按实际场景评估 |

```html
<!-- 推荐：title 单行裸 FTL -->
<title><#if oauthAuthorize?? && oauthAuthorize>应用授权<#elseif oauthLogin?? && oauthLogin>用户登录<#else>Autumn管理系统</#if></title>

<!-- 禁止：注释包裹 + 换行 → 渲染结果含 <!----> -->
<title>
  <!--<#if oauthLogin?? && oauthLogin>-->
  用户登录
  <!--</#if>-->
</title>
```

**`<body class="...">` 等可接受空白折叠的属性**：可用注释包裹 + 换行（类名间空白合法）；若仍出现可见 artifacts，改为 Controller **`Model` 拼好 class 字符串**或单行裸 FTL。

### 7.4 JavaScript 中的 FreeMarker

**禁止**在 `<script>` 内使用 **HTML 注释** `<!-- -->` 包裹 FTL（会破坏 JS 语法或产生不可预期注释块）。

#### 7.4.1 条件分支：`//` 前缀 + 独立行

FTL 指令行以 **`//`** 开头（与 HTML 侧 `<!--<#if>-->` 对称），**每个指令独占一行**：

```javascript
//<#if oauthLogin?? && oauthLogin>
var oauthLogin = true;
//<#else>
var oauthLogin = false;
//</#if>
```

```javascript
//<#if error?? && error?has_content>
vm.error = true;
vm.errorMsg = '${error?js_string}';
//</#if>
```

**禁止**：

```javascript
// 错误：HTML 注释包裹 FTL
<!--<#if oauthLogin?? && oauthLogin>-->
var oauthLogin = true;
<!--</#if>-->

// 错误：行内 HTML 注释 + 条件（整行可能被 JS 当注释吞掉）
var oauthLogin = <!--<#if ...>-->true<!--</#if-->;
```

#### 7.4.2 简单标量：优先 `${}` 插值

布尔、数字等**无需分支**时，用 **`${}` + 默认值**，避免在 JS 里堆 `#if`：

```javascript
var pollIntervalMs = ${pollIntervalMs!2000};
var oauthLogin = ${(oauthLogin!false)?c};
```

字符串必须 **`?js_string`** 转义：

```javascript
var ctx = '${request.contextPath}';
var uuid = '${uuid!""?js_string}';
```

#### 7.4.3 JS 说明性注释

仅作阅读的注释用 **`//`** 或 **`/* */`**，且**不要**在注释正文里写未转义的 `${}` / `<#if>` 样例。

### 7.5 决策流程（写模板前 10 秒）

```text
1. 是否在 <script> 内？
   └─ 是 → //<#if> 块（§7.4.1）或 ${}（§7.4.2）；禁止 <!-- -->
2. 是否是 <title> 或必须零注释残留的纯文本？
   └─ 是 → 单行裸 <#if>（§7.3）
3. 否则（HTML 块、表单、列表）→ <!--<#if>--> 每指令独立行（§7.2）
4. 是否仅展示文档/示例、且含大量 FTL 特殊字符？
   └─ 是 → <#noparse>（§7.6）
```

### 7.6 无法避免的语法冲突：`<#noparse>`

当**无法避免**与 FreeMarker 冲突的片段（文档示例、内嵌含 `$`/`#`/`<#` 的大段文本）时，使用 **`<#noparse>...</#noparse>`**，使内部**按字面输出**、不被 FTL 解析。

### 7.7 自检清单（改 `.html` 模板后）

- [ ] `<script>` 内无 `<!-- -->` 包裹的 FTL？
- [ ] JS 条件是否为 `//<#if>` 且**每指令一行**？
- [ ] `<title>` 是否为**单行裸 FTL**（无注释包裹）？
- [ ] HTML 块条件是否为 `<!--<#if>` 且**每指令一行**？
- [ ] 说明性 HTML 注释是否未包含 `${}` / `<#if>` 字面量？
- [ ] 插入 JS 的字符串是否使用 `?js_string`？
- [ ] 渲染后查看「查看网页源代码」：标题与正文无 `<!---->` 等多余注释残留？

## 8. 实体扫描、注解建表与禁止手工 DDL 脚本

- 框架对**已加载的实体类型**进行扫描，在配置允许时**自动建表 / 自动对齐表结构**（启动期执行检测与更新；开关与行为见 `autumn.table.*` 等配置，`docs/AI_BOOT.md` §3 与运行库说明）。

- **禁止**在研发交付物中新增**用于初始化或变更库结构的独立 DDL `.sql` 文件**作为常规做法（例如随代码提交的 `schema.sql` / `init.sql` 专供手工执行）。**表结构以实体注解 + 框架同步为准**；若生产必须离线 DDL，走发布流程与 DBA 规范，**不**作为日常开发默认路径。

- **实体主键纪律**（与 **§10.4** 一致）：自增 **`id`** 仅供后台生成器与框架默认 CRUD；第二主键按需选型见 **`docs/AI_DUAL_KEY.md`**；**存量实体默认不强制升级**（**§1.2**）。**禁止**用自增 **`id`** 承担业务关联与对外标识（新开发）。

## 9. 模块目录、包名、表前缀与实体命名（强制）

- **模块根**：`modules/<模块名>/` = 包 `cn.org.autumn.modules.<模块名>` = 表前缀 **`<模块名>_`**。
- **实体类名**：领域短名 + `Entity`（见 **`docs/AI_BOOT.md` §3.2**）。  
  - **硬性 · 表前缀不进类名**：表前缀（如 `auth_`、`safe_`、`bot_`、`esign_`）**只出现在物理表名**（`@TableName`）中，**不得**作为 `*Entity` 类名的前缀或词缀。  
  - **硬性 · 至少两个单词**：去掉 `Entity` 后缀后，类名须由 **≥2 个 PascalCase 单词**组成（例：`SignTask`、`TaskActor`、`ScopeDefinition`、`PayUserPin`），**禁止**单单词实体名（反例：`TaskEntity`、`ActorEntity`、`SealEntity`、`LogEntity`），以免跨模块/生成物撞名与语义过窄。  
  - **正例**：`@TableName("auth_scope_definition")` → **`ScopeDefinitionEntity`**（不是 `AuthScopeDefinitionEntity`）。  
  - **正例**：`@TableName("esign_sign_task")` → **`SignTaskEntity`**（不是 `EsignTaskEntity` / `TaskEntity`）。  
  - **正例**：`@TableName("safe_pay_user_pin")` → **`PayUserPinEntity`**（不是 `SafePayUserPinEntity`）。  
  - **反例**：`AuthScopeDefinitionEntity`、`EsignActorEntity`、`SafePayUserPinEntity`、`BotRobotConfigEntity`、`TaskEntity`。
- **物理表名**：`{模块名}_` + 类名去 `Entity` 后蛇形；**只**在 **`@TableName`** 声明，**`@Table` 禁止写 `value`**（由框架合并）；Dao `quote` 与 `@TableName` 一致。
- **配套命名**：与实体一一对应的 **`XxxDao` / `XxxDaoSql` / `XxxService` / `XxxController` / `XxxControllerGen`** 使用**同一领域短名**（不含表前缀）；对外 Open API Controller 可用模块能力前缀（如 `EsignOpenApiController`），**不得**回写到实体类名。

## 10. 表/字段注释、索引与字段类型

### 10.1 注释格式（`@Table` / `@Column.comment`）

- **`comment` 的用途**：描述**字段或整张表**在业务上的含义，以及生成侧会用到的**表头、菜单名、多语言短名**等展示语义；写法上统一为 **`简介名:合理说明`**——**简介名**放在**半角冒号 `:`** 之前，**冒号后**写用途、取值、与其它字段关系等补充说明。
- **短标题（冒号前）长度（硬性）**：**2～8** 个字符（汉字或等价长度），后接半角 **`:`** 与说明。短于 2 或长于 8 均属违规（`id` 等框架保留短名除外）。
- **冒号前片段必须字段间互不相同（硬性）**：框架在列表、表单、多语言初始化等链路中会对含 `:` 的 `comment`**只取第一个冒号之前**的片段作为**列标题 / 短标题**（详见 `docs/AI_BOOT.md` §3、`docs/AI_MAP.md` §2.10.5）。因此**不同字段**须使用**不同的**冒号前简介名；即使含义相近、语义相同，或同属「统计类」但**统计口径 / 维度不同**的列，也**不得**共用同一冒号前片段，否则生成界面会出现**多个列共用一个表头**、无法区分。
- **`@Table(comment = "...")`** 与字段级规则相同：表级展示名同样可能只取冒号前段，须避免与别表或同模块其它实体在生成物中的短名无意撞车（按产品语义自行区分即可）。

### 10.2 索引与唯一约束

> **业务仓统一风格与模式目录（SQL→注解、isUnique、命名/指纹身份、单列保留与组合对齐 Dao、PR 清单）**见专篇 **`docs/AI_INDEX_ANNOTATIONS.md`**（§5 / §6 / §7）。本节保留**硬性纪律**与 ASC/DESC 基线；落地示例以专篇为准。

**怎么选写法（速查）**

| 需求 | 写法 |
|------|------|
| 单列普通索引 | 字段上 `@Index`（默认本列 + ASC） |
| 单列要 DESC / 前缀 | `@Index(order = IndexOrder.DESC)` / `@Index(length = n)` |
| 单列部分索引等 | `@Index(where = "...")` 等（方言不支持则 WARN 跳过） |
| 组合索引 / 多列 FULLTEXT | 类上 `@Indexes({ @Index(name=..., fields={@IndexField...}) })` |
| 唯一约束（单列） | 仅字段 **`@Column(isUnique = true)`**，**禁止**再 `@Index`；**以字段注解为准** |
| 组合唯一（多列） | 类级 `@Index(..., indexType = UNIQUE)` + `@IndexField`（参与列**不得**再单列 `isUnique`） |

- **硬性约束（无例外）——字段 `isUnique` 优先**：凡在 **`@Column` 上声明 `isUnique = true` 的字段**，**一律不得**再对该字段声明 **`@Index`**（字段级）。**单列唯一性只认字段上的 `isUnique = true`**，不以类级 `@Index` / `@Indexes` 再表达同一列的唯一或「唯一键 + 附带过滤」组合。类级**单列**索引若含该列：框架**剔除**并 WARN。类级**组合**若含该列：框架**保留列序**并 WARN（`含已 isUnique 列 … 建议去掉冗余唯一首列`）——业务须按专篇决策表整理：通常**删除**含该列的组合；列表查询另建**不含**唯一列、最左对齐列表谓词的组合。
- **原因（为何字段为准）**：`isUnique` 已落库为单列 `UNIQUE KEY`；按该列等值查询最多命中 1 行，再拼 `visible`/`audit`/`enable` 等附带列**不能**比 UNIQUE 点查更优，只会双开索引、放大写与启动 diff。整理时以字段注解为真相源，组合侧让位。
- **C 类误区（禁止当优化）**：列表 SQL 没有 `唯一列=?` 时，**把唯一列调到组合最左不会更快**（最左用不上）；正确做法是最左用列表等值列，组合内**不要**再挂唯一列。详见 **`docs/AI_INDEX_ANNOTATIONS.md` §5.6**。
- **同类重复唯一声明**：亦不要与 `@Indexes` 等在同一列上重复描述同一约束（细则见 **`docs/AI_BOOT.md` §3**、**`docs/AI_MAP.md` §2.10.2**；统一整理方案见 **`docs/AI_INDEX_ANNOTATIONS.md` §5**）。
- **查询列默认建索引（新实体硬性默认）**：凡 **Dao / Service 查询谓词**会等值或高频过滤的列（如 `user`、`status`、`taskUuid`、`bizSource`、`ownerUser`、外键式关联 uuid），**默认须**在实体上声明索引能力——单列用字段 **`@Index`**，多列等值/排序用类级 **`@Indexes`**；**禁止**「先裸查上线、再补丁加索引」。已 `isUnique=true` 的列**不要**再叠 `@Index`（见上条）。主键 `id`、纯展示大文本列除外。
- **单字段索引声明位置（新代码默认）**：**单字段索引一律**写在对应**字段**上的 **`@Index`**。**类级 `@Indexes`** 仅用于**组合索引**（`fields` 含 **≥2** 列）或 FULLTEXT/SPATIAL/部分索引等特殊声明。
- **组合索引列序（硬性）**：`@IndexField` 数组顺序 = 落库 `SEQ_IN_INDEX` 顺序；框架用有序模型发射 DDL，**禁止**再假设 HashMap 乱序。设计时遵循最左前缀：等值列靠左，范围/排序列靠右。**列序优先于 ASC/DESC**。**频道/开关等值列**（如 `xsign`/`community`）必须靠左，禁止放在范围列之后。
- **列方向 ASC/DESC（长期基线，见下「列方向」专节）**：**默认 ASC**（未写 = ASC）。单列用字段级 **`@Index(order = …)`**；组合用 **`@IndexField(order = …)`**。DESC **仅**按主查询 `ORDER BY` 写在**排序键**上。禁止无 `EXPLAIN` 依据批量改 DESC。主查询含稳定 `ORDER BY …, id DESC` 时，索引末尾宜带上 `id`（方向与 ORDER BY 一致）。
- **字段 `@Index` 与组合（§6）**：**既有字段级 `@Index` 默认保留**；**禁止**以「组合最左前缀已覆盖同族查询」为由批量删除单列索引（Dao 未见不等于无 `baseMapper`/Wrapper 直查）。优化重点是类级 **`@Indexes` 列序/完整度对齐 Dao**。仅当任务**明确写明**某单列无用时才删该字段 `@Index`。`isUnique` 与组合的整理见专篇 **§5**（删的是含唯一列的组合，不是普通字段索引）。
- **命名与身份（推荐）**：索引 `name` 用可读压缩语义（如 `statuslivingat`）；可空（框架按列派生）；超长截断 WARN。`@IndexField(field)` **推荐驼峰属性名**，兼容下划线。库↔实体同一索引以**定义指纹**为准，名字仅配对/落库标识；仅改名走 RENAME。见专篇 **§7**。
- **扩展能力（可选，带默认值）**：`@Index`：`where` / `include` / `parser` / `invisible` / `expression`，以及字段级快捷 `order` / `length` / `collation`；`@IndexField`：`order` / `length` / `expression` / `collation`。不支持的方言 **WARN 并跳过/降级**。索引列 `collation` 已进模型，各方言 DDL **尚未统一发射**——列排序规则优先用 **`@Column.collation`**。
- **同步配置**：`autumn.table.index.rebuild-on-order-mismatch`（默认 `true`）——列序/方向不一致时**按索引成对** DROP+ADD（失败 WARN 不阻断启动）；日志关键字 `Index rebuild upgrade`。设 `false` 退回仅列集合比对（运维缓冲，不是放弃列序真相）。
- **依赖仓兼容**：升级 autumn 后，旧 `@Index`/`@IndexField(field,length)` **无需改实体**即可编译启动；`IndexInfo.getFields()` 仍可用（有序 Map 视图）。历史库方向本就是 ASC，与默认一致，**不会仅因默认 ASC 触发重建**。
- **Dao**：字符串 `*DaoSql` 禁止「幻想」索引。索引两条轨：Entity `@Index`/`@Indexes`，或 **查询即索引**（方法空 `@Index` + `IndexedSelect`）。同一索引 **二选一**；同指纹以查询即索引为准。**AI 必读** **`docs/AI_INDEX_ANNOTATIONS.md` §1**。

#### 列方向 ASC / DESC（性能、用法与长期方案）

以 **MySQL 8 + InnoDB** 为主路径（5.7 上索引定义里的 DESC 常被忽略存储，长期以 8.0+ 真实 DESC 为准）。

**性能边界**

| 场景 | 行为 | 建议 |
|------|------|------|
| `(col ASC)` + `ORDER BY col DESC` | 可用 **Backward index scan**，通常仍走索引、不必 filesort | 常见「最新在前」可接受；反向扫描大约慢 ~10%–15%（热点页争用时更大） |
| 组合全 ASC + 后缀整段 `ORDER BY … DESC`（前缀等值） | 常可反向扫描 | 多数列表够用 |
| `ORDER BY a DESC, b ASC` 等**混合方向**，索引全 ASC | MySQL 8 易 **filesort** | 建与 ORDER BY **同构**的方向（如 `(a DESC, b ASC)`） |
| 等值列 `user` / `status` / `deleted` | 方向几乎无收益 | **保持 ASC** |

**真正需要写 `IndexOrder.DESC` 时**：① 高频大范围倒序，希望 Forward scan；② 混合升降序要避免 filesort；③ `EXPLAIN` 已确认 filesort / 错索引且 QPS、数据量大。

**建模规则（硬性纪律）**

1. **默认 ASC 是永久语义基线**（不是权宜兼容）；未写 `order` = ASC。
2. **先列序、后方向**：等值/IN 在左；范围与排序键在右；DESC 只写在排序键上。
3. **方向对齐「主查询」ORDER BY**：一个索引服务一族主 SQL；正序/倒序/混合冲突时加第二索引，**不要**改框架默认。
4. **用 EXPLAIN 决策**：改 DESC 前后对比 `Backward index scan` → Forward，或 filesort 消失；**禁止**「凡倒序列表就全库改 DESC」。

**长期落地三阶段**

1. **语义基线**：默认 ASC；升级优先纠正组合索引**列序**（日志 `reason=ORDER`）；**不**批量加 DESC。
2. **热点定向 DESC**：盘点大表高频 `ORDER BY … DESC` / 混合序；按主查询改注解并接受单索引 DROP+ADD；大表低峰或短暂关同步开关后处理。
3. **纪律固化**：新索引 Review 须能指出主查询；DESC 仅出现在排序键。

```java
// 推荐：单字段索引在字段上（默认本列 + ASC）
@Column(length = 32, comment = "用户:所属用户uuid")
@Index
private String user;

// 推荐：字段级单列需要 DESC / 前缀时，用 @Index 快捷属性（不必再写 fields）
@Column(comment = "开播时间:列表倒序")
@Index(order = IndexOrder.DESC)
private Date livingAt;

// 可选：部分索引等（方言支持时）仍挂在字段级 @Index 上
// @Index(where = "deleted = 0")
// private String user;

// 推荐：组合索引——等值 ASC，排序键按主查询 ORDER BY 显式 DESC（必须用 @IndexField）
// 主查询: WHERE status=? ORDER BY living_at DESC, id DESC
@Indexes(@Index(name = "living_hot", fields = {
    @IndexField(field = "status"),
    @IndexField(field = "livingAt", order = IndexOrder.DESC),
    @IndexField(field = "id", order = IndexOrder.DESC)
}))
public class DemoEntity { ... }

// 禁止（新代码）：类级 @Indexes 仅含单列，除非明确要求
// @Indexes(@Index(fields = { @IndexField(field = "user") }))
// 禁止：字段级再写 @Index(fields={@IndexField(field="本字段",...)}) 重复本列名（用 order/length 快捷即可）
// 禁止：无 EXPLAIN 依据把等值列或全表索引批量改成 DESC
```

更多业务模式（热度多排序键、宽 URL 前缀、组合 UNIQUE、投放位正序、§5 isUnique / §6 单列保留、PR 清单）→ **`docs/AI_INDEX_ANNOTATIONS.md`**。

### 10.3 整型、布尔与空值

- **新代码（硬性）**：无特殊原因时，字段**必须**使用 **基本类型**（如 `int`、`boolean`）并在 **`@Column`** 上声明**默认值**；确需可空包装类型须在字段注释或 JavaDoc 写明原因。

### 10.4 双键模型：管理用自增 `id` + 业务主键（按需）

每个实体**必须**具备自增 **`id`**（技术主键）；**业务主键（第二主键）按表语义按需选型**，二者**不得混用职责**：

1. **自增 `id`（技术主键 / 后台 CRUD 专用）**  
   - **必须**存在，类型与注解与代码生成、MyBatis-Plus 默认主键语义一致，例如：
     ```java
     @TableId
     @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
     private Long id;
     ```
   - **仅用于**：代码生成产出的后台列表/表单 CRUD、框架按主键更新删除等**管理端默认路径**。  
   - **禁止**：在业务代码、对外 API、缓存键、日志对外暴露中将其作为资源标识；**禁止**作为其它表的外键或被其它表引用；**禁止**在实体关系、领域模型中用它表达「业务上的同一实体」——需全局业务键时由 **`uuid`** 或唯一 **`user`** 承担（见下）。

2. **业务主键（第二主键，按需）**  
   - **通用实体（默认，需全局行标识）**：须增加唯一列 **`uuid`**（`String` 默认 **`length = 32`**，或雪花 **`Long`**），实现 **`UuidBased`** / **`SnowBased`**。细则 **`docs/AI_DUAL_KEY.md` §3.1**。  
   - **按用户唯一（每真人一行）**：第二主键为唯一列 **`user`**，实现 **`UserBased`**；**禁止**再定义 **`uuid`**。细则 **§3.3、§4.3**。  
   - **仅用户维度、无全局行标识**：**可不设第二主键**；自增 **`id`** + 非唯一 **`user`**（及业务字段）即可；**`user`** 可单独非唯一，或由 **`user` + 其它字段** 联合唯一。细则 **`docs/AI_DUAL_KEY.md` §3.4、§4.4**。  
   - **仍有 `uuid` 第二主键时**：可增加**非唯一** **`user`** 外键（调用主体，可为真人或机器人 uuid，§1.1）。细则 **§3.5**。  
   - **唯一约束纪律**：业务第二主键列（**`uuid`** 或 **`UserBased` 的 `user`**）须 **`isUnique = true`** 且**禁止**再叠 **`@Index`**（§10.2）。**§3.4** 的 **`user`** **不得**单独 `isUnique`。  
   - **生成与填充**：**`uuid`** 经 **`Uuid` / `Snow`** 与 **`AutoIdService`** 自动补全；**`UserBased.user`** 由业务赋值；**§3.4** 的 **`user`** 由业务赋值。  
   - **外键列（引用它实体 `uuid`）**：Java 字段名 **新代码一律 `{concept}`**；**禁止**新写 **`{concept}Id`**。存量 `{concept}Id` 保持不回退。细则 **`docs/AI_DUAL_KEY.md` §3.2～§3.5**。  
   - **必须用于表间关联 / 对外 ID / 缓存与消息引用的**，须使用 **`uuid`** 或按用户唯一表的 **`user`**，**禁止**用自增 **`id`**。

**小结**：**`id`** = 内部技术与生成 CRUD；**第二主键** = 按需 **`uuid`**、唯一 **`user`**（**`UserBased`**），或**无**（**§3.4 仅 `user` 维度**）。**禁止**把 **`id`** 当成业务主键；**禁止**用户维度已够用仍冗余 **`uuid`**；**禁止**按用户唯一表同时使用 **`uuid`** 与唯一 **`user`**。

**存量与触发升级**：历史未达标实体**默认不要求全面 retrofit**；但出现下列任一情况时，**任务范围内必须对齐**本节与 **`docs/AI_DUAL_KEY.md`**：  
1）**触发表结构变更**（增删改列/索引）；2）**新增外键列或关联**；3）**新增对外 Open API** 暴露该资源。其余「仅读写存量字段」任务可不升级。

### 10.5 字段存储加密（`@FieldEncrypt`）

新实体字段若标注 **`@FieldEncrypt`**，须同时满足（细则 **`docs/AI_FIELD_ENCRYPT.md`**）：

1. 对应 Service **必须**继承 **`EncryptModuleService`**（不可仅 `ModuleService`）。  
2. **`searchable = true`** 时必须手写配套 **hash 列**（框架**不会**自动 DDL）；调用与缓存按专篇 §7。  
3. **禁止**对加密字段使用会绕过加解密链路的 **`lambdaUpdate` 直写**等路径。

## 11. 生成层边界与业务落点

权威条文；流程细节见 **`docs/AI_CODEGEN.md`**。其它文档只摘要并回链本节。

### 11.0 总原则（硬性）

| 原则 | 含义 |
|------|------|
| **可替换** | 凡 `GenUtils` + `template/*.vm` 可稳定复现的文件，须能被生成 ZIP **整文件覆盖**后仍编译可用；**不得**依赖被覆盖的手写差异。 |
| **禁特有逻辑** | 模板可重生产物（`*ControllerGen`、`*Pages`、`list.html`/`list.js` 等）中**禁止**非模板标准的业务、流程、对外 API；特有逻辑只写可维护层（`*Service`、实体 `*Controller`、独立 Api Controller）。 |
| **空壳除外** | 仅为编译/继承的**空壳或占位**可存在；一旦填入内容，**只允许模板等价实现**（仍须可替换），**禁止**特有逻辑。 |

### 11.1 必产文件与资源层

每个新实体须有（类名与模板一致）：

| 产物 | 说明 |
|------|------|
| `*Entity` | §8～§10；`@Getter`/`@Setter`（§2.0） |
| `*Service` | `extends ModuleService`（或 `EncryptModuleService`）；**业务唯一落点**（§2.1） |
| `*Dao`（及需要的 `*DaoSql`） | Service 经 `baseMapper` 使用，不另注入本 Dao（§13） |
| `*Controller` | `extends *ControllerGen`；`@RequestMapping("{module}/{entitylower}")`（§3） |
| `controller/gen/*ControllerGen` | 占位，或按 §11.2 填模板等价 CRUD |

```java
@RestController
@RequestMapping("auth/scopedefinition")
public class ScopeDefinitionController extends ScopeDefinitionControllerGen {
}
```

- **`list.html` / `list.js` / `*Pages`**：**禁止 AI 写**；开发者**可选** ZIP 落地，落地后适用 §11.0。  
- ZIP **可不跑**：不阻塞 Service / 实体 Controller / 对外 Api Controller。

### 11.2 `ControllerGen` 预演（未 ZIP 前）

未用后台 ZIP 生成/替换前，若需要与 **`ControllerGen.java.vm`** 相同的管理端 CRUD：**允许 AI 按模板实现到 `*ControllerGen`**，且必须可被后续 ZIP **整文件替换**。  
**禁止**在 Gen 中写对外 API、与模板不一致的定制逻辑。定制写在 `*Controller` / `*Service`。

### 11.3 落点一览

| 层 | 写什么 |
|----|--------|
| `*Service` | 实体相关**全部**业务（§2.1） |
| 实体 `*Controller` / 独立 Api Controller | 薄编排；对内 §3，对外 §4 |
| `*ControllerGen` / Pages / list | **仅**模板标准 CRUD；无特有逻辑（§11.0） |

## 12. Dao、Provider 与多数据库 SQL

- **默认目标**：除非在代码中**明确声明**仅支持某一种库，业务 SQL 与持久化写法应对框架已接入的 **`DatabaseType` 全量兼容**（见 **`docs/AI_DATABASE.md`** §1、§2）。

- **禁止**在 **Dao 接口**的方法上**内联硬编码 SQL**（如 **`@Select`/`@Update` 等注解中直接写死 SQL 字符串**）作为**新代码**的默认写法。

- **必须**通过与之对应的 **MyBatis `Provider`**（如 **`XxxDaoSql`**、`*Provider` 类及 `@SelectProvider` 等）拼接与维护 SQL；推荐 **`extends RuntimeSql`**，统一使用 **`quote`、`limitOne`、`likeContainsAny`、`columnValueInCommaSeparatedList`** 等可移植片段（见 **`docs/AI_DATABASE.md`** §3、§5 与 **`docs/AI_POSTGRESQL.md`** 示例）。

- **Java 里禁止硬编码方言符号**（反引号、双引号、方括号等）：表/列引用、分页排序列、Map 等值键须走 **`RuntimeSql` / `DialectService` / `WrapperColumns`**（**`docs/AI_DATABASE.md` §4.0**），未继承方言基类时**优先** `WrapperColumns.columnInWrapper`、`orderByColumnExpression`、`entityWrapperAllEqQuoted` / `queryWrapperAllEqQuoted`（以当前分支 API 为准）。

- **EntityWrapper / Condition**：仅使用跨库相对安全的条件（等值、范围、`in`、空值、简单 `orderBy`）；**禁止**在 `apply` / 自定义片段中写死单库函数（`FIND_IN_SET`、`IFNULL`、`DATE_FORMAT` 等）。复杂查询、JOIN、报表、强依赖方言的模糊/列表匹配 **必须** 改为 **Dao + Provider**（**`docs/AI_DATABASE.md`** §4～§5）。

- **例外**：仅框架内置或历史存量可在治理计划中逐步迁移；**新开发一律走 Provider**。

## 13. Controller、Service、Dao 协作纪律

- **对外禁止直接引用 Dao（硬性）**：**Dao 仅由所属实体的 `*Service`（`ModuleService` 继承链）持有并使用**（经 **`baseMapper`**）。  
  - **禁止**在 Controller、ControllerGen、Facade、其它 Service、Listener、Job 独立 Bean、Component、工具类等**任何外部位置** `@Autowired` / 注入 / 直接调用 **任意 `*Dao`**。  
  - **对外访问持久化必须经 Service**：需要读写下表数据时，**只**注入并调用对应 **`*Service`** 的公开方法。  
  - 理由：Service 已持有 Dao（`ModuleService`），跨层直连 Dao 会绕过缓存、加密、事务与领域规则。

- **本实体 Service**：须 **`extends ModuleService`**（或 **`EncryptModuleService`**），是该实体**业务逻辑唯一落点**（§2.1）；通过继承链 **`baseMapper`** 访问本实体持久化，**禁止**再 **`@Autowired`** 本实体 **Dao**（避免与基类重复入口）。需日志时类上 **`@Slf4j`**。

- **跨实体访问（硬性）**：**禁止**在 Service 中注入**其他实体的 Dao**；**必须**注入对方 **Service**。Facade / 编排层同样**禁止**直连任何 Dao。

## 13.1 缓存 `@Cache`（摘要硬约束）

- 读走框架 **`getCache` / `getListCache` / `getShareCache`** 等；写库成功后**必须**调用对应 **`removeCache` / `removeListCache` / `removeCacheAll`**（见 **`docs/AI_CODEGEN.md`** §4、**`docs/AI_MAP.md`**）。  
- **`create = true`** 仅用于专篇/任务**明确允许**的「缓存未命中可建记录」场景；默认 **`false`**。  
- 同一实体多套 **`@Cache(name=...)`** 时，写路径须按通道失效，禁止漏删。

## 14. 资源、`statics`、`pages`、`Site` 与 `@PageAware`

- 框架支持从**多个 jar** 加载模板与静态资源；各子项目须把**页面与静态资源放在本项目资源目录**下，随 jar 发布，**不**依赖在入口工程手工拷贝聚合（与 `docs/AI_GOVERNANCE.md` TemplateFactory 约定一致）。

- **`resources/statics/`**：跨子模块共享的**公共静态资源**目录；放置于此的静态资源按框架约定**匿名可访问**，**无需**再单独配置匿名 URL。**若框架已有同类资源，须优先复用**，避免重复拷贝。

- **新后台页面**：放在**当前模块资源树下与模块对应的 `pages` 目录**（如 `resources/.../modules/<模块>/pages/`）；**若不存在则创建**。

- **站点入口**：每个新页面须在 **`site` 包**中对应 **`Site` 类（如 `XxxSite`）** 增加**页面指引**（字段 + 注解），与现有模块（如 `oauth` 的 `OauthSite`）一致，供框架扫描并经 **SPM** 形成可访问路径。

- **`@PageAware`**（标注在 **Site 类的字段**上）：  
  - **匿名公共页**：`@PageAware(login = false)`（按需填 `resource` / `url` / `page` 等指向资源路径）。  
  - **需登录后台页**：`@PageAware(login = true)`。  
  - **仅系统管理员**：`@PageAware(login = true, admin = true)`。已登录非 `isSystemAdministrator` 经 SPM 或 `/modules/**` 直链访问 → **404**（`AdminShellAccess`）；未登录仍按 `login` 走登录跳转。  
  - **管理台 / 全表配置页**（含子项目 im、vshort、xsignw、bighub 等）：须标 **`admin = true`**，禁止仅靠菜单权限冒充「后台管理员」。  

  具体属性以 `cn.org.autumn.annotation.PageAware` 为准。

- **生成 CRUD / 管理 API 与管理 IP（硬约束）**：  
  - 强制语义：**系统管理员**；当 **`SECURITY_OPS_CONFIG.adminIpEnable=true`** 时另须 **`wall_ip_admin_white`**（`IpAdminWhiteEntity`）。`adminIpEnable` **默认 false**（wall「管理IP校验」可开；关须 clearToken）。  
  - 客户端 IP：`IPUtils.getIp` **默认**历史语义（XFF 最左）；仅显式配置 `autumn.client-ip.trusted-proxies` 时右侧剥离防伪造。安全加固不得把云 SLB 的 `100.125.x` 当作用户 IP。  
  - 自助 gen（usr/safe/bot/qrc）一般不强制；**`SENSITIVE_SELF_GEN_CONTROLLERS`**（UserProfile/LoginLog/Robot*/ScanTicket/ClientGrant/Token/PIN 等全表）仍强制超管+管理 IP；Web 扫码子类扩展方法不连坐。  
  - Web 扫码建票绑定浏览器 Cookie `autumn_qrc_bind`；OAuth code/refresh 须归属 `client_id`；CSRF 仅对校验通过的 API Token 豁免。  
  - 框架切面 **`GenAdminAccessAspect`** 织入 `*..modules..controller..*` 与 `*..controller.gen..*`（**不限** `cn.org.autumn` 前缀）；判定 `shouldRequireAdminIpApi`，执行 `requireGenAdmin`。  
  - **`ControllerGen.java.vm`**：各 CRUD 入口显式调用 **`AdminAccessGuard#enforceAdminIpApiIfRequired`**（与切面双保险；ZIP 重生后生效）。  
  - **豁免**（不强制超管+IP，仍受权限/行级隔离）：模块 **`usr` / `safe` / `bot` / `qrc`**；SPI **`AdminAccessModuleHandler#selfGenModules()`**。  
  - **业务仓非标准包**：优先改用标准包；手写管理模块用 SPI **`managedModules()`**；异形包薄切面委托 **`AdminAccessGuard#enforceAdminIpApiIfRequired`**。完整标准见 **`docs/AI_ADMIN_IP_BIZ.md`**（模式 A/B/C）；**API/页面强制对比矩阵**见 **`docs/AI_ADMIN_ACCESS_MATRIX.md`**。  
  - **禁止**把匿名首页、公开 CMS 读接口、C 端列表写进 `controller.gen`；写接口禁止整段 Shiro **`anon`**（§4）。  
  - **禁止**为「方便运营」把整站业务模块塞进 `selfGenModules()`；**禁止**用 `WallDefault.ipWhiteEnable` 代替 `adminIpEnable`。  
  - 兼容清单 **`docs/AI_SECURITY_HARDENING_3_0_COMPAT.md`**（**C-20**、§4.4）；拒绝日志 **`SecurityAlert denied`**（§4.4.2）。  
  - **WebShell**：**默认关闭**；开启后仍须超管（+ 可选管理 IP）+ 主机白名单（**C-21**）。

- **页面文案与功能描述（新增硬约束）**：除非需求中有**特殊说明**，所有页面开发都必须以**用户与使用者视角**进行描述与设计，禁止从开发实现视角表达。具体要求：
  - **禁止**出现开发相关专业术语（如接口、数据库、缓存、线程、方法、函数、参数、对象、JSON、SQL、鉴权链路等）。
  - **禁止**出现后台表名、字段名、后台函数名、类名、包名、实现路由等技术标识。
  - **禁止**描述后台开发体系结构、分层实现、调用链路，或“开发者/程序员视角”说明。
  - 页面描述必须聚焦“用户能做什么、看到什么、得到什么反馈”，使用业务语义与操作结果表达。

## 15. Redis TTL / Redisson 与 `RedisExpireUtil`

业务工程若使用 **Redisson + Spring Data Redis**，建议在处理 **键过期、带 TTL 的写入、滑动窗口计数续期** 或排查 **`StackOverflowError`（栈含 `DefaultedRedisConnection` / `pExpire`）** 时：

1. 先阅读 **`docs/REDIS_REDISSON_SPRING_DATA.md`**（依赖对齐，治本）与 **`docs/REDIS_TTL_GUIDE.md`**（何时适合用 **`RedisExpireUtil`**、API 对照、推荐处理顺序）。
2. **推荐**用 **`cn.org.autumn.utils.RedisExpireUtil`** 统一表达 TTL 语义（Lua 或服务端语义），避免各项目复制零散脚本；是否替换存量 `RedisTemplate.expire` / `set(..., TimeUnit)` 由团队按风险与迭代安排决定。
3. 可选使用 **`scripts/constraints-scan`** 的 **H 组**（全文体检默认包含；仅 Redis TTL 时用 **`--redis-expire-only`**）做静态检索，辅助代码评审。

### 15.1 Redis 批量扫描（禁止 `KEYS`）

凡涉及 **按前缀枚举键、清缓存、运维列表、会话批量列举** 等场景：

1. **禁止** **`RedisTemplate.keys` / `StringRedisTemplate.keys`**（底层 **`KEYS`**，大键空间会阻塞 Redis 并拖垮同实例全部连接）。典型事故见 **`docs/REDIS_KEYS_AND_SCAN.md` §2**。
2. **必须** 使用 **`SCAN` + `Cursor` + 分批 `delete`**，管理类接口设 **扫描上限** 并在 API 中可表达 **`truncated`**；全库键数用 **`DBSIZE`**，内存用 **`INFO memory`**，STRING 长度用 **`STRLEN`**，禁止为估大小对 Session 等键全量 **`GET`**。
3. **键命名（硬性）**：业务自定义 Redis 键**必须**经 **`RedisKeys`**（或项目统一键工厂），或符合 **`autumn:{module}:{biz}:...`** 模板；**禁止**在业务代码中裸拼无命名空间的 key 字符串。已知 key 读写走 **`RedisKeys` / `RedisUtils` / `CacheService`**，不要自建扫描工具类。
4. **合并前**：建议在业务仓执行 **`bash scripts/constraints-scan`**（全文）或 **`--redis-keys-only`**（**J 组**）；全文体检默认含 **H + J**。本轮规范**不**将扫描结果设为 CI 硬失败，但仍属合并前自检项（与 **`docs/AI_INDEX.md`**「按需」一致）。

完整禁止/推荐写法与 Checklist：**`docs/REDIS_KEYS_AND_SCAN.md`**。

说明：§15 不否定 Spring 默认写法在「依赖已正确对齐」时的可用性；文档重点是**讲清风险场景**并给出 **Autumn 侧的一致推荐路径**。

## 16. 默认合规与纠偏自检（硬性）

### 16.1 默认：开发过程始终按规范执行

凡在 Autumn / 业务仓写实体、Dao、Service、Controller、对外 API、索引、注释时，**一律默认遵守本文 §2～§15**（及 `AI_BOOT` / `AI_DATABASE` / `AI_INDEX_ANNOTATIONS` 等专篇），**不依赖**用户是否口头提到「规范」「命名」「索引」等词。

- **禁止**把「用户没提规范」当作可降低命名 / 索引 / 禁 Map / Service 内聚等纪律的理由。  
- 每轮交付前仍应用下文 **§16.3 清单**做一次默读自检（改到的模块范围内）。

### 16.2 触发词 = 已跑偏，进入纠偏模式

当用户、任务或 PR 描述出现下列任一表述时，含义是：**当前实现或讨论已偏离规范**，AI / 开发者**不得**仅当提醒继续往下写，而须进入**纠偏**：对**任务触及的模块（宜扩至同模块相关改动）**做**全面规范核对与修正**（不得只改表面命名或局部一处），并**宜执行** **`scripts/constraints-scan`**（重点 A1/A4/A5 与分层组），**先改违规再结束**。

**触发词（含同义）**：`按照 autumn 框架规范`、`按 Autumn 规范`、`开发约束`、`命名规范`、`索引规范`、`接口规范`、`对外 API 规范`、`实体命名`、`表前缀`、`Request/Response`、`约束扫描`、`规范体检`、`AI_STANDARDS`。

说明：触发词**不是**「才开始遵守规范」的开关；平时就应合规。触发词表示**需要按清单纠偏 + 扫描核实**。

### 16.3 全面检查清单

1. **命名**：`*Entity` 去掉后缀后 **≥2 个单词**；**无**模块/表前缀进类名；表名 = `{module}_` + 蛇形短名；Dao/Service/ControllerGen 与实体短名一致（§9、`AI_BOOT` §3.2）。  
2. **索引**：查询谓词列已 `@Index` / `@Indexes`；`isUnique` 未叠 `@Index`；单列在字段上、组合在类上（§10.2）。  
3. **分层**：实体业务只在对应 `*Service`（`ModuleService`）；Controller / Facade **未**引用 Dao；Controller **无**状态机与松散 Map 拼装（§2.1、§13）。  
4. **对外 / 手写 Rest API**：`POST /{module}/api/v1/{action}`（或已登记子路径 / 运维 Facade）；body 为 **`Request<*Request>` / `Response<*VO>`**（CA 回调可裸 `*Request`）；**禁止** `Map`/`JSONObject`/`Object`/`Entity` 作契约 `T`；回调同样类型化（§4）。  
5. **注释**：`comment` 为 `短标题:说明`，短标题 2～8 且字段间互异（§10.1）。  
6. **其它**：无常规 DDL `.sql`；Dao 仅 Provider；对外写接口鉴权非 blanket anon；生成物无特有逻辑（§8、§11、§12）。

未通过任一项：**先改代码再结束**；不得以「历史兼容」 internally 绕过，除非任务明示并登记产品/协议例外。

## 17. 与 AI 文档的交叉引用

| 主题 | 文档 |
|------|------|
| `@Table` / `@Column` / 字符集 / 表名推导细节 | `docs/AI_BOOT.md` §3 |
| 多库兼容、Wrapper 边界、Provider 标准写法 | **`docs/AI_DATABASE.md`**（权威） |
| PostgreSQL 专项 DDL/元数据、`RuntimeSql` 示例 | `docs/AI_POSTGRESQL.md` |
| 升级扫描与迁移 | `docs/AI_UPGRADE.md` |
| ModuleService、缓存、队列、LoopJob、生成矩阵 | `docs/AI_MAP.md` |
| `isUnique=true` 与 `@Index` 禁止叠用 | **§10.2**；Boot/MAP 索引收集见 **`docs/AI_BOOT.md` §3**、**`docs/AI_MAP.md` §2.10.2** |
| 单字段索引须字段级 `@Index` | **§10.2**；类级 `@Indexes` 仅用于组合索引（≥2 列）；DESC/前缀用 `@Index(order/length)` 快捷 |
| 组合索引列序与 ASC/DESC | **§10.2**；列序优先；默认 ASC；DESC 仅排序键 + 主 ORDER BY |
| 业务仓索引统一风格 / 模式目录 | **`docs/AI_INDEX_ANNOTATIONS.md`**（SQL→注解、**§5 isUnique**、**§6 单列保留 / 组合对齐 Dao**、命名、PR 清单） |
| 双键模型、`UuidBased` / `SnowBased` / **`UserBased`** / `AutoIdService` | **§10.4**、**`docs/AI_DUAL_KEY.md` §3～§4**；SQL 见 **`docs/AI_DATABASE.md` §1.1** |
| Lombok 实体 / `@Slf4j` | **§2.0**、**`docs/AI_CODE_STYLE.md` §5.1 / §8** |
| 实体业务必须落对应 Service | **§2.1**、**`docs/AI_SERVICE_COHESION.md`** |
| 对内路径 / 对外 URL / 契约与校验 / 禁 Map | **§3、§4** |
| 实体 ≥2 词、表前缀不进类名 | **§9**、`AI_BOOT` §3.2 |
| 默认合规；触发词=纠偏自检 | **§16** |
| 生成可替换 / 禁特有逻辑 / 空壳除外 | **§11.0～§11.2**、`docs/AI_CODEGEN.md` |
| 代码生成端到端流程、三步开发节奏、缓存/队列基类详解 | `docs/AI_CODEGEN.md` |
| 加解密 / Compatible | `docs/AI_CRYPTO.md`、**§4** |
| 字段存储加密 | **§10.5**、**`docs/AI_FIELD_ENCRYPT.md`** |
| 多项目协作与术语 | `docs/AI_GOVERNANCE.md`、**§2.3** |
| 索引与加载组合 | `docs/AI_INDEX.md` |
| Redis TTL、Redisson、`RedisExpireUtil` 使用说明 | **`docs/REDIS_TTL_GUIDE.md`**、**`docs/REDIS_REDISSON_SPRING_DATA.md`** |
| Redis 批量扫描、禁止 `KEYS`、键命名 | **`docs/REDIS_KEYS_AND_SCAN.md`**、**§15.1** |
| Service 内聚与行数 | **`docs/AI_SERVICE_COHESION.md`**、**§2.1** |

## 18. 维护约定

- 新增与「实体/库表/Dao/资源/页面/对外契约」相关的团队规则时，**优先更新本文**（含 **§4、§9、§10.2、§16**），并同步 `docs/AI_INDEX.md`、`docs/AI_GUIDE.md`、**Cursor Skill `autumn-framework-3x` / `2x`** 摘要；**多库类型、方言、Wrapper/Provider 纪律**以 **`docs/AI_DATABASE.md`** 为落地专篇，变更时同步该文 §2；**老旧项目注解 Dao / 方言 Wrapper 迁移与扫描清单**见 **`docs/AI_DATABASE.md` §8** 与 **`docs/AI_UPGRADE.md` §3.3**；与代码生成流程或三步节奏相关的补充可落在 **`docs/AI_CODEGEN.md`**。**Redis TTL 与 `RedisExpireUtil`** 以 **`docs/REDIS_TTL_GUIDE.md`** 为专篇；**Redis 禁止 KEYS / 推荐 SCAN** 以 **`docs/REDIS_KEYS_AND_SCAN.md`** 为专篇，变更时同步 **`scripts/constraints-scan`**（**H / J / A 组**）。
- 若与 `docs/AI_MAP.md` §4「开发决策规则」表述重叠，以**本文 §2～§14**、**§15～§16** 为应用层与数据访问纪律的权威表述；MAP 保留框架能力级硬约束与类索引。**索引与唯一约束叠用纪律（§10.2）** 变更时同步 **`docs/AI_BOOT.md` §3**、**`docs/AI_MAP.md` §2.10.2**；**业务模式与统一风格**同步 **`docs/AI_INDEX_ANNOTATIONS.md`**。**实体双键（§10.4）** 变更时同步 **`docs/AI_DUAL_KEY.md`**、**`docs/AI_DATABASE.md` §1.1** 与 **`autumn-lib`** 中 **`Uuid` / `Snow` / `AutoIdService`** 说明（若工具 API 有变）。
- **§16（默认合规 / 纠偏触发词）**变更时同步 Skill「默认合规与纠偏」「约束扫描」与 **`scripts/constraints-scan` A 组**说明。
