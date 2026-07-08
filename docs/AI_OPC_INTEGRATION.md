# OPC 开放接入对接手册

> **适用对象**：将本系统作为**第三方应用**，接入 Autumn 开放平台（OPL）的团队  
> **模块**：`cn.org.autumn.modules.opc`  
> **HTTP 前缀**：`{ORIGIN}/open`（常量见 `OpcConstants`）  
> **双轨总览与路径规则**：**`docs/AI_AUTH_LOGIN_MODES.md` §1.5**

`opc` 不依赖 `opl` Java 包，通过 HTTP 调用远程 OPL（`OAuth2HttpClient` + `CredentialParam.OPL`）。

---

## 1. 架构

```
本系统 (opc)  ──HTTP──▶  开放平台 (opl)
   ├─ opc_connect_app        ├─ opl_open_app
   ├─ opc_connect_bind       └─ openId / unionId
   └─ 本地 sys_user
```

---

## 2. HTTP 路径（OPC）

| 用途 | 方法 | 路径 |
|------|------|------|
| 接入登录入口页 | GET | `/open/oauth2/login?appId=...` |
| 发起授权（跳转 OPL） | GET | `/open/oauth2/opc/authorize?appId=...`（与 OPL `/authorize` 冲突，故加 `opc`） |
| OAuth 回调 | GET | `/open/oauth2/callback?appId=...&code=...` |
| 成功页（无 callback 时默认） | GET | `/open/oauth2/success` |

### 2.1 用户 Open API

根路径 **`POST /open/api/v1/platform/*`**，需 **`@Authenticated`**。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/open/api/v1/platform/app/apply` | 向远程 OPL 申请 appId 并落库 |
| POST | `/open/api/v1/platform/app/save` | 手动保存/更新接入配置 |
| POST | `/open/api/v1/platform/opc/app/list` | 列出当前用户的接入 App |
| POST | `/open/api/v1/platform/login/url` | 获取登录入口 URL |

### 2.2 管理 API

根路径 **`/open/admin/opc/platform/*`**（`OpcConstants.ADMIN_PLATFORM`）。页面：**`opcmanage.html`**（应用与绑定运维 Tab）。

**安全**：须系统管理员；由 `SystemAdminApi` 统一鉴权。

### 2.3 接入绑定友好管理页

| 项 | 值 |
|----|-----|
| 页面 | **`/modules/opc/connectbind`**（模板 `opc/connectbind.html`，`OpcConstants.CONNECTBIND_MANAGE_PAGE`） |
| API | **`/opc/connectbind/manage/*`**（`OpcConstants.CONNECTBIND_MANAGE_API`） |
| 实现 | `ConnectBindManageController` + `ConnectBindManageService` |

**与 codegen 列表的区别**：不再使用 `modules/opc/connectbind.html`（jqGrid 原始字段表）；`SysPageController` 将上述路径转发至友好页。

**权限模型**：

| 角色 | 列表范围 | 解除绑定 | 手动添加 | API 字段 |
|------|----------|----------|----------|----------|
| 登录用户 | 仅自己的绑定 | 仅自己的 | 否 | `OpcBindManageView`（应用名、账号昵称、绑定时间） |
| 系统管理员 | 全站 | 任意 | 是 | 同上 + 详情内 `technical`（openId/uuid 等） |

**API 摘要**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/opc/connectbind/manage/overview` | 概览（`admin` 标志 + 计数） |
| GET | `/opc/connectbind/manage/apps` | 筛选用应用列表（`OpcAppBriefView`，不含 connectApp uuid 除非管理员） |
| GET | `/opc/connectbind/manage/binds` | 分页绑定（用户脱敏视图） |
| POST | `/opc/connectbind/manage/bind/delete` | 解除绑定 |
| POST | `/opc/connectbind/manage/bind/create` | 管理员手动添加 |

**冲突页入口**：授权失败页「绑定管理」链至本页；用户可先解除再重新授权。

`opcmanage.html` 绑定 Tab 仍走 **`/open/admin/opc/platform/binds`**，返回 `OpcBindAdminView`（运维字段，系统管理员专用）。

`opcmanage.html` 绑定 Tab 仍走 **`/open/admin/opc/platform/binds`**，返回 `OpcBindAdminView`（运维字段，系统管理员专用）。

**安全**：`/open/oauth2/opc/authorize` 绑定 Session state；`/open/api/v1/platform/**` 须有效用户令牌（禁止 hint-only）。

---

## 3. 同实例联调

1. **OPL** 注册 App：`redirectUri` = `{ORIGIN}/open/oauth2/callback?appId={appId}`
2. **OPC** `POST /open/api/v1/platform/app/save`：`platformBaseUrl` = 本机根地址，凭证与 OPL 一致
3. 访问 `{ORIGIN}/open/oauth2/login?appId=...` → 点击授权 → OPL 确认 → 回调 → Session

**浏览器流程**：

```
/open/oauth2/login → /open/oauth2/opc/authorize → /open/oauth2/authorize（OPL）
→ /open/oauth2/callback → /open/oauth2/success
```

---

## 4. 本地用户绑定（与经典 OAuth 对称）

`ConnectBindService.resolveAndBind(app, userInfo, platformUser)` 顺序：

1. `(connectApp, openId)` 已绑定 → **同平台**且 token 含 **platformUser** 时以 OPL 授权用户为权威，可**校正**陈旧绑定后 **幂等** 登录；跨平台则校验 platformUser 与绑定用户一致，否则冲突
2. `(connectApp, unionId)` 已绑定 → 更新 openId 后同上
3. **同平台**（`appId` 在本地 OPL 注册）且 token 含 **platformUser** → 绑定到该本地用户，**不**新建账号；若本地用户已有其他 openId 行，同平台下可更新 openId
4. **已登录** 本地 Session → 绑定当前 Session 用户
5. **跨平台 + 未登录 + 无绑定** → **`BIND_CHOICE_REQUIRED`** → `/open/oauth2/bind/choice`

**绑定选择端点**：`GET /open/oauth2/bind/choice`、`GET /open/oauth2/bind/confirm`、`POST /open/oauth2/bind/create`（共用 `oauth2/bind-choice.html`）。

**platformUser** 仅同 JVM 内通过 `OpenPlatformService.resolvePlatformUserUuid` 解析，不经 HTTP userInfo 暴露；与 OPC 回调域名、Session 无关。

**自助解绑**：

| 场景 | 方式 |
|------|------|
| 授权回调上下文 | 登录态 `POST /open/oauth2/bind/unbind?appId=...`（`ConnectBindService.unbindForSessionUser`） |
| 冲突页 / 自助 | 打开 **`/modules/opc/connectbind`**，解除对应应用绑定后重新授权 |

**绑定维护（运维/用户）**：见 §2.3；编排核心仍为 `ConnectBindService`，管理查询在 `ConnectBindManageService`。

**表约束**：`opc_connect_bind` 对 `(connectApp, openId)` 与 `(connectApp, user)` 各一唯一索引。

---

## 5. 与 LOGIN_AUTHENTICATION 的关系

- 传统 `oauth2:{clientId}` 登录**不受影响**
- Open 接入走独立 **`/open/oauth2/*`**，与 `/oauth2/*` 并行

---

## 6. 相关文档

- 开放平台提供方：**`docs/AI_OPL_INTEGRATION.md`**
- 双轨总览：**`docs/AI_AUTH_LOGIN_MODES.md`**
- 传统 OAuth：**`docs/AI_OAUTH_INTEGRATION.md`**

文档页：`{ORIGIN}/modules/docs/opc-integration`
