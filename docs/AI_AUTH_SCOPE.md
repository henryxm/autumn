# 授权 Scope 目录与字段对照

> **适用对象**：OAuth `/oauth2/*` 与 OPL `/open/oauth2/*` 对接开发者、平台管理员、框架维护者  
> **实现模块**：`cn.org.autumn.auth.scope`（`autumn-lib`）、`cn.org.autumn.modules.auth`（`autumn-modules`）  
> **管理页**：`authscopemanage.html`（业务管理）、`scopedef.html`（gen CRUD 审计）、`oauthasmanage.html` / `oplmanage.html`（客户端登记）

---

## 1. 架构分层

| 层次 | 包 / 类 | 职责 |
|------|---------|------|
| **内存目录** | `AuthScopeCatalog`、`AuthScopeDef`、`AuthScopeSet`、`AuthScopeResolution` | 内置 scope 注册、解析/校验、`basic` 展开、label 固定展示顺序 |
| **业务桥接** | `AuthScopeSupport` | OAuth/OPL/QRC 调用 catalog；`validate*Scope`、`grant*Scope`、`labels()` |
| **DB 持久化** | `ScopeDefinitionEntity`（表 `auth_scope_definition`）、`ScopeDefinitionService` | 启动 `syncBuiltins` 合并内置行；自定义 scope CRUD；`refreshCatalog()` 回灌 catalog |
| **页面 Model** | `AuthPageSupport` | 统一注入 `scopeLabels`、`authorizeLoginAction`、consent CSRF |
| **授权 UI** | `login.html` + `_scope_perm_list.html` | OAuth/OPL authorize 登录 + 确认同一页（**无**独立 `opl/authorize.html`） |
| **管理 API** | `ScopeDefinitionAdminController` | `/oauth/admin/scopes/*`（catalog/list/save/enabled/delete） |

**命名约定**：

- **`AuthScopeDef`**：`autumn-lib` 内存 catalog 条目（非 DB 实体）。
- **`ScopeDefinitionEntity`**：DB 行实体（原 `ScopeDefEntity`，全拼命名）；表名为 **`auth_scope_definition`**（与 `@TableName` / Dao Provider 一致）。
- OPL 身份 scope 代码为 **`unionid`**（全小写）；**不提供**旧码 `union` 的兼容归一。

---

## 2. 兼容别名 `basic`

| 轨道 | `basic` 展开为 | userInfo 默认字段 |
|------|----------------|-------------------|
| **OAuth**（uuid） | `identity` + `profile` | `uuid`、`nickname`、`icon`、`username` |
| **OPL**（openId） | `openid` + `unionid` + `profile` | `openId`、`unionId`、`nickname`、`icon` |

未传 `scope` 或传 `scope=basic` 时行为与上述一致。登录页、授权确认页、管理页、QRC confirm 展示的 scope 中文说明**唯一来源**为 **`AuthScopeCatalog.labels()`**，经 Model 属性 **`scopeLabels`** 注入模板。

**展示顺序**（`AuthScopeCatalog.orderCodesForDisplay`）：身份层（OAuth `identity` / OPL `openid`→`unionid`）→ `profile` → `phone` → `email` → `verified` → `status` → `realname_attr` → `realname_person` → `realname_id` → 其余自定义 code 字母序。

---

## 3. 标准 Scope 目录

### OAuth 轨（`/oauth2/userInfo`）

| Scope | 说明 | 输出字段 | 敏感级 |
|-------|------|----------|--------|
| `identity` | 用户唯一标识 | `uuid` | 低 |
| `profile` | 基本资料 | `nickname`、`icon`、`username` | 低 |
| `phone` | 手机号 | `mobile` | 高 |
| `email` | 邮箱 | `email` | 高 |
| `verified` | 实名认证状态（0/1，不含身份证） | `verified` | 中 |
| `status` | 账号状态（-1/0/1） | `status` | 中 |

### OPL 轨（`/open/oauth2/userInfo`）

| Scope | 说明 | 输出字段 | 敏感级 |
|-------|------|----------|--------|
| `openid` | 应用内身份 | `openId` | 低 |
| `unionid` | 跨应用身份（依赖 `openid`） | `unionId` | 低 |
| `profile` | 查看基本资料 | `nickname`、`icon` | 低 |
| `phone` | 手机号 | `mobile` | 高 |
| `email` | 邮箱 | `email` | 高 |
| `verified` | 实名认证状态 | `verified` | 中 |
| `status` | 账号状态 | `status` | 中 |

> OAuth 与 OPL 的 `profile` scope 展示 label 均为「查看基本资料」；OAuth 含 `username` 字段，OPL 不含。

### 3.1 实名详情三档（独立资源接口）

| Scope | Label | 字段（`AuthRealNameInfo`） | 默认 |
|-------|-------|----------------------------|------|
| `realname_attr` | 查看实名人口属性 | `age`、`gender`、`ethnicity` | **关闭** |
| `realname_person` | 查看实名姓名与生日 | `name`、`birthday` | **关闭** |
| `realname_id` | 查看实名证件与地址 | `idNumber`、`address` | **关闭** |

- **不进 `basic`**；无 `AuthField`（**不**写入标准 `userInfo`）。
- **兼容别名 `realname`**：请求/登记时 expand 为当前轨道下**已启用**的三档（类似 `basic`）；**不再**作为 builtin 实体行；历史 DB 行按退役码过滤。
- **端点**：
  - OAuth：`GET|POST /oauth2/realName`
  - OPL：`GET|POST /open/oauth2/realName`
- **鉴权**：有效 access_token，且 token `grantedScope`（expand 后）含**任一**实名三档；否则 **`403`** `{"error":"insufficient_scope"}`。
- **供给**：`AuthRealNameProvider` SPI（业务仓返回全量）；框架出口按 granted **并集裁剪**（`AuthRealNameService.project`）。无 Provider → **`501`** `{"error":"unsupported"}`；未实名 → `200` `{}`。
- **RP 预取**：经典 OAuth / OPC 在 token 含任一实名档时，将裁剪后详情挂到 `UserProfile.realName` / `OpenUserInfoSnapshot.realName`；OPC 远端须用 **token 响应 scope**，不得仅用客户端登记 `app.scope`。预取失败仅 `log.debug`（无 PII）。

### 通配

| 代码 | 含义 |
|------|------|
| `all` | 客户端登记通配，允许该轨道下全部**已启用** scope |
| `basic` | 见 §2 |
| `realname` | 见 §3.1（别名，非独立 builtin） |

---

## 4. 解析与校验行为

| 方法 | 场景 | 未知/未启用 code | 超出客户端登记 |
|------|------|------------------|----------------|
| `AuthScopeCatalog.resolve` | authorize 入口（`validateOAuthScope` / `validateOplScope`） | 记入 `invalid` → **`invalid_scope`** | 记入 `denied`，交集为空则拒绝 |
| `AuthScopeCatalog.resolveBounded` | 发码/换票/QRC 下游 scope | **静默剔除** | 交集为空时回退 `basic ∩ 客户端登记` |

**DB 残留行**：历史 builtin 行若 code 已退役（如 `union`），`ScopeDefinitionService.isRecognized()` 过滤，**不删库**、不参与 `refreshCatalog`；`listForAdmin` 同样过滤。

---

## 5. 全链路行为

1. **authorize**：校验请求 scope ⊆ 客户端登记 scope；`basic` 展开；非法 scope 返回 `invalid_scope`
2. **approve / 发码**：将 **granted scope** 写入 auth code / token 存储
3. **token 响应**：`scope` 为本次实际授权值（非硬编码）
4. **userInfo**：按 token 绑定 scope 裁剪字段；扩展字段通过 `OpenPlatformExtension.enrichUserInfo` / `OAuthPlatformExtension.enrichUserInfo`

---

## 6. 授权页 UI（与实现对齐）

| 路径 | 模板 | 说明 |
|------|------|------|
| `GET /oauth2/authorize` | `login.html`（`oauthAuthorize=true`） | `DefaultPage.oauthAuthorize()` → `AuthPageSupport.prepareOauthAuthorize()` |
| `GET /open/oauth2/authorize` | `login.html`（`oplAuthorize=true`） | `DefaultPage.oplAuthorize()` → `AuthPageSupport.prepareOplAuthorize()` |
| `GET …/qrc/authorize`（SPM） | `modules/qrc/pages/authorize.html` | JS 重定向到 `/login`，保留 query |

**共用片段**：

- `_scope_perm_list.html`：授权确认区 scope 列表（读 `scopeLabels`）
- `_scope_perm_box.html`：管理页 scope 多选（`auth-scope-picker.js` + catalog API）

**已废弃**：`templates/opl/authorize.html`（7/6 起统一 `login.html`，删除不影响运行时）。

---

## 7. 管理配置

### 运维顺序（推荐）

1. **`authscopemanage.html`**：启用所需内置 scope（实名三档默认关闭；BigHub 至少启用 `realname_attr`）或新增自定义 scope。
2. **双轨四页登记客户端 scope**（勾选器共用 `auth-scope-picker.js`）：
   - `oauthasmanage.html` — OAuth AS
   - `oauthrpmanage.html` — OAuth RP（不得超过上游 AS 登记范围）
   - `oplmanage.html` — OPL AS
   - `opcmanage.html` — OPC 接入（须与上游 OPL 登记一致）
3. 用户重新授权后，确认页 `login.html` / RP 登录入口按 `scopeLabels` 展示中文说明。

### 页面

| 页面 | 用途 |
|------|------|
| `authscopemanage.html` | **推荐**：全局 scope 启用/禁用、按轨道筛选与分组、实名专区说明、自定义 scope（tracks / sensitivity / requires） |
| `scopedef.html` / `scopedef.js` | gen CRUD 审计（`/auth/scopedef/*`）；改库后须走 `ScopeDefinitionService` 以保证 `refreshCatalog`；日常请用 `authscopemanage` |
| `oauthasmanage.html` | OAuth AS 客户端登记 scope（多选；支持 `realname` 别名预设） |
| `oauthrpmanage.html` | OAuth RP 客户端登记 scope（多选） |
| `oplmanage.html` | OPL AS 应用登记 scope（多选） |
| `opcmanage.html` | OPC 接入应用登记 scope（多选；存储值 DOM 须带 id 供 picker 回写） |
| QRC `clientgrant`（gen） | 扫码授权上限 scope；**gen 页勿手改**，保持文本/CSV；运行时仍经 `ConsentSupport` / `AuthScopeSupport.labels` 展示 |

### Picker 行为（`auth-scope-picker.js`）

- 目录：`GET /oauth/admin/scopes/catalog?track=oauth|opl`；仅 **enabled** 项可勾选。
- 预设：`basic` / `all`；当至少一档实名已启用时增加 **「实名详情」**（`realname` 别名）。
- `expand` / `join`：与后端一致，`realname` ↔ 当前轨道已启用的 `realname_attr`/`person`/`id`；可折叠为 `basic realname` 等存储值。
- 实名均未启用时展示提示，链到 `authscopemanage.html`。

### Admin API（`ScopeDefinitionAdminController`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/oauth/admin/scopes/catalog?track=oauth\|opl` | 当前轨道目录；含 **`basicCodes`**（与 `AuthScopeSet.basicFor` 一致）；含 disabled 项供提示 |
| GET | `/oauth/admin/scopes/list` | 管理列表（已识别行，含 tracks/fields/sensitivity/requires） |
| POST | `/oauth/admin/scopes/save` | 自定义 scope（body：code/label/tracks/fields/sensitivity/requires） |
| POST | `/oauth/admin/scopes/enabled` | 启用/禁用 |
| POST | `/oauth/admin/scopes/delete` | 删除自定义 scope（内置不可删） |

### 验收清单

1. `authscopemanage` 启用 `realname_attr` → 四页 picker 出现该档并可勾选。
2. 勾选后存储值含 `realname_attr` 或折叠为含 `realname` 的别名串；保存客户端成功。
3. `opcmanage` 勾选时「存储值」文案随勾选更新。
4. 授权确认页在请求含实名档时展示对应中文 label。

---

## 8. 刻意不纳入标准 scope / userInfo

- `idCard` 原文 — 经 **`realname_*` scope + `/oauth2/realName`（或 OPL 对称端点）+ `AuthRealNameProvider`** 下发，**不**写入标准 userInfo
- `password` / token / 支付凭证 — 禁止
- 审计字段（`loginIp` 等）— 不进 userInfo
- 退役 code（如 `union`、历史单体 `realname` builtin 行）— 无运行时兼容，按 §4 过滤

---

## 9. 扩展

- 实现 `OAuthPlatformExtension` / `OpenPlatformExtension` 追加校验或 userInfo 字段
- 实现 `AuthRealNameProvider` 供给实名详情（无 Bean 时 realName 接口返回 `501 unsupported`）
- 自定义 scope：管理页或 Admin API 写入 `auth_scope_definition`，`ScopeDefinitionService.refreshCatalog()` 合并进 `AuthScopeCatalog`
- QRC scope 文案：`ConsentProvider` SPI；未注册时 `ConsentSupport` → `AuthScopeSupport.labels()`

---

## 10. 代码索引

| 类 | 路径 |
|----|------|
| `AuthScopeCatalog` | `autumn-lib/.../auth/scope/AuthScopeCatalog.java` |
| `AuthScopeSet.basicFor` | `autumn-lib/.../auth/scope/AuthScopeSet.java` |
| `AuthRealNameInfo` / `AuthRealNameProvider` | `autumn-lib/.../auth/model`、`auth/spi` |
| `AuthRealNameService` / `AuthRealNameHttpSupport` | `autumn-modules/.../auth/service`、`auth/support` |
| `AuthScopeSupport` | `autumn-modules/.../auth/support/AuthScopeSupport.java` |
| `AuthPageSupport` | `autumn-modules/.../site/AuthPageSupport.java` |
| `ScopeDefinitionEntity` | `autumn-modules/.../auth/entity/ScopeDefinitionEntity.java` |
| `ScopeDefinitionService` | `autumn-modules/.../auth/service/ScopeDefinitionService.java` |
| `ScopeDefinitionAdminController` | `autumn-modules/.../auth/controller/ScopeDefinitionAdminController.java` |
| `AuthorizationController` | `autumn-modules/.../oauth/oauth2/AuthorizationController.java`（含 `/oauth2/realName`） |
| `OplAuthorizationController` | `autumn-modules/.../opl/oauth2/OplAuthorizationController.java`（含 `/open/oauth2/realName`） |
| `auth-scope-picker.js` | `autumn-modules/.../statics/js/auth-scope-picker.js` |

单测：`autumn-lib/.../AuthScopeCatalogTest.java`、`AuthUserInfoBuilderTest.java`。

---

## 相关文档

- [AI_OAUTH_INTEGRATION.md](AI_OAUTH_INTEGRATION.md)
- [AI_OPL_INTEGRATION.md](AI_OPL_INTEGRATION.md)
- [AI_AUTH_LOGIN_MODES.md](AI_AUTH_LOGIN_MODES.md)
- [AI_QRC.md](AI_QRC.md)（authorize / consent 与 `login.html`）
