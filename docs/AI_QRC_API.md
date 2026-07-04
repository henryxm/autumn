# QRC 开放 API 参考

> 模块前缀：`/qrc`。面向 APP 与第三方 Open API 的端点统一 **`Request` / `Response`**（`code=0` 成功）；PC 登录页走 **`ScanTicketController`**，返回框架 **`R`**（结构相同：`code` / `msg` / `data`）。
>
> **读者**：后端实现见 `AI_QRC.md`；APP 对接见 `AI_QRC_CLIENT_API.md`；第三方 OAuth 见 `AI_OAUTH_INTEGRATION.md` + `AI_QRC_INTEGRATION.md`。

---

## 0. 通用约定

### 0.1 Base URL

```
https://{auth-host}
```

所有路径均相对站点根路径；`qrUrl` 由服务端 `sys_config.baseUrl` 拼装。

### 0.2 Content-Type

| 方法 | Header |
|------|--------|
| POST（JSON body） | `Content-Type: application/json` |
| GET | 无 body |

### 0.3 请求包络（开放 API）

POST body **必须**包一层 `data`：

```json
{
  "data": { }
}
```

### 0.4 响应包络

**开放 API（`Response`）**

```json
{
  "code": 0,
  "msg": "success",
  "data": { }
}
```

**PC 网页（`R`）**

```json
{
  "code": 0,
  "msg": "success",
  "data": { }
}
```

失败时 `code != 0`，`msg` 为可读错误；业务错误码见 §6。

### 0.5 鉴权

| 端点组 | 鉴权 |
|--------|------|
| `/qrc/api/v1/t/*`、`/ticket/open/*` | **匿名**（Shiro `anon`） |
| `/qrc/api/v1/ticket/detail|scan|confirm|deny` | **须已登录用户**（`@Authenticated`） |
| `/qrc/scanticket/web/*` | **匿名** |

APP 用户 Token（与机器人/安全模块一致）：

```
Token: {userToken}
```

或

```
Authorization: Bearer {userToken}
```

Open API 客户端凭证写在 **JSON body** 的 `clientId` / `clientSecret`，**不得**放入 QR 或浏览器 URL。

---

## 1. 票据状态机

```
                    scan
         PENDING ──────────► SCANNED
            │                    │
            │ deny               │ confirm
            ▼                    ▼
         DENIED              CONFIRMED ──► COMPLETED（OAuth 发 code/token 等）
            │
            │ cancel (Open API)
            ▼
        CANCELLED

任意非终态 ──超时──► EXPIRED
```

| 状态 | 含义 | 客户端动作 |
|------|------|------------|
| `PENDING` | 已建票，待扫码 | APP 可 scan |
| `SCANNED` | 已扫码，待用户确认 | APP 展示确认页；PC 可展示 `scannerBrief` |
| `CONFIRMED` | 用户已确认 | `SELF_WEB_LOGIN` / `OAUTH_AUTHORIZE`：PC 用 `exchange` 换 Session |
| `COMPLETED` | 业务完成 | Open API 轮询可取 `code` / `accessToken`；或已有 `redirect` |
| `DENIED` | 用户拒绝 | 停止轮询，提示重新扫码 |
| `CANCELLED` | 第三方取消 | 停止轮询 |
| `EXPIRED` | 超时 | 重新建票 |

**终态**：`COMPLETED`、`DENIED`、`CANCELLED`、`EXPIRED`。

### Intent 与 confirm 结果

| Intent | confirm 后典型状态 | PC/第三方取结果方式 |
|--------|-------------------|---------------------|
| `SELF_WEB_LOGIN` | `CONFIRMED` + `exchange` | PC `session/exchange` |
| `OAUTH_AUTHORIZE` | `CONFIRMED` + `exchange` | 浏览器 `session/exchange` 后继续 OAuth |
| `OAUTH_CONSENT` | `COMPLETED` + `redirect`/`result.code` | 浏览器跟随 `redirect` |
| `OAUTH_DEVICE` | `COMPLETED` + `result.*` | Open API 轮询 / Webhook / DeepLink |

---

## 2. 开放 API（`QrcApiController`，`/qrc/api/v1`）

### GET `/qrc/api/v1/t/{uuid}`

QR 内容 URL（匿名）。用于 APP 可选校验或 WebView 打开。

**响应 `data`（`TicketLinkResult`）**

| 字段 | 类型 | 说明 |
|------|------|------|
| `uuid` | string | 票据 id |
| `intent` | string | 业务场景 |
| `status` | string | 当前状态 |

```json
{
  "code": 0,
  "data": {
    "uuid": "a1b2c3d4",
    "intent": "SELF_WEB_LOGIN",
    "status": "PENDING"
  }
}
```

> APP **推荐**本地解析 QR 得到 `uuid`，本接口仅作校验；响应**不含** secret、token、用户信息。

---

### POST `/qrc/api/v1/ticket/detail`

**鉴权**：用户 Token。

**请求**

```json
{ "data": { "uuid": "a1b2c3d4" } }
```

**响应 `data`（`TicketDetailResult`）**

| 字段 | 类型 | 说明 |
|------|------|------|
| `uuid` | string | 票据 id |
| `intent` | string | `SELF_WEB_LOGIN` / `OAUTH_*` |
| `status` | string | 当前状态 |
| `clientId` | string | OAuth client（非 OAuth 场景可空） |
| `clientName` | string | 应用展示名 |
| `clientIconUri` | string | 应用图标 URL |
| `scope` | string | 原始 scope 字符串 |
| `scopeLabels` | string[] | 展示用 scope 列表 |
| `redirectUri` | string | OAuth 回调（展示用） |
| `payload` | object | 完整载荷（一般无需客户端解析） |
| `intentTitle` | string | 确认页标题 |
| `intentHint` | string | 风险提示 |
| `deviceHint` | string | 发起端设备描述 |

**`intentTitle` / `intentHint` 示例**

| intent | intentTitle 示例 | intentHint 示例 |
|--------|------------------|-----------------|
| `SELF_WEB_LOGIN` | 网页版登录确认 | 你的账号正在请求登录网页版，请确认是否本人操作 |
| `OAUTH_AUTHORIZE` | 授权「{应用名}」登录 | 该应用请求使用你的 Autumn 账号登录，请确认是否授权 |
| `OAUTH_CONSENT` | 授权「{应用名}」 | 该应用请求访问你的账号信息，请确认是否授权 |
| `OAUTH_DEVICE` | 授权「{应用名}」 | 该应用请求访问你的账号，请确认是否授权 |

---

### POST `/qrc/api/v1/ticket/scan`

**鉴权**：用户 Token。标记「已扫码」，**不登录、不授权**。

**请求**

```json
{ "data": { "uuid": "a1b2c3d4" } }
```

**响应 `data`（`TicketStatusResult`）**：见 §2.6。

---

### POST `/qrc/api/v1/ticket/confirm`

**鉴权**：用户 Token。用户点击「确认」后调用。

**请求**

```json
{ "data": { "uuid": "a1b2c3d4" } }
```

**响应 `data`（`TicketStatusResult`）**：见 §2.6。OAuth 完成时 `result` 含 `code` / `accessToken` / `deepLink` 等。

---

### POST `/qrc/api/v1/ticket/deny`

**鉴权**：用户 Token。用户拒绝授权。

**请求**

```json
{ "data": { "uuid": "a1b2c3d4" } }
```

**响应 `data`**：`status` 为 `DENIED`。

---

### 2.6 `TicketStatusResult` 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `uuid` | string | 票据 id |
| `status` | string | 状态，见 §1 |
| `intent` | string | 业务场景 |
| `exchange` | string | 一次性 PC 交换令牌；**仅** `CONFIRMED`/`COMPLETED` 且 Web 登录类 intent 有值 |
| `redirect` | string | OAuth 浏览器跳转 URL（含 `code=`） |
| `result` | object | 扩展结果，见下表 |
| `expireIn` | long | 票据剩余有效秒数 |
| `scannerBrief` | object | 已扫码时：`displayName`、`icon` |

**`result` 常见键**

| 键 | 何时出现 | 说明 |
|----|----------|------|
| `code` | `COMPLETED` + `POLL_CODE` / consent | OAuth 授权码 |
| `accessToken` | `COMPLETED` + `POLL_TOKEN` | 访问令牌 |
| `state` | OAuth | 回传 state |
| `clientId` | OAuth | 客户端 id |
| `redirectUri` | OAuth | 注册回调地址 |
| `deepLink` | `DEEP_LINK` delivery | `{scheme}://oauth/callback?code=...` |

**`scannerBrief` 展示规则**：`status` 为 `SCANNED`、`CONFIRMED`、`COMPLETED` 时返回；`icon` 可能为相对路径，需拼接站点 origin。

**示例（已扫码，待确认）**

```json
{
  "code": 0,
  "data": {
    "uuid": "a1b2c3d4",
    "status": "SCANNED",
    "intent": "SELF_WEB_LOGIN",
    "exchange": null,
    "expireIn": 278,
    "scannerBrief": {
      "displayName": "张三",
      "icon": "/statics/img/avatar.png"
    },
    "result": {}
  }
}
```

**示例（网页登录已确认）**

```json
{
  "code": 0,
  "data": {
    "uuid": "a1b2c3d4",
    "status": "CONFIRMED",
    "intent": "SELF_WEB_LOGIN",
    "exchange": "e5f6g7h8-one-time-token",
    "expireIn": 250,
    "result": {}
  }
}
```

---

### POST `/qrc/api/v1/ticket/open/create`

第三方自建 QR（匿名）。创建 `OAUTH_DEVICE` 票据。

**请求**

```json
{
  "data": {
    "clientId": "my-app",
    "clientSecret": "secret",
    "redirectUri": "https://tp.example.com/oauth/callback",
    "scope": "basic profile",
    "state": "csrf-random",
    "payload": {}
  }
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `clientId` | 是 | OAuth 客户端 id |
| `clientSecret` | 是 | 客户端密钥 |
| `redirectUri` | 否 | 与 `oauth_client_details.redirect_uri` **完全一致**；有值时服务端校验 |
| `scope` | 否 | 空格分隔 scope |
| `state` | 否 | CSRF 回传 |
| `payload` | 否 | 扩展键值，合并进票据 payload |

**响应 `data`（`TicketCreateResult`）**

| 字段 | 说明 |
|------|------|
| `uuid` | 票据 id |
| `qrUrl` | QR 内容 URL：`{baseUrl}/qrc/api/v1/t/{uuid}` |
| `expireIn` | 有效秒数（与全局 `ticketTtlSeconds` 一致） |
| `intent` | 固定 `OAUTH_DEVICE` |
| `status` | 固定 `PENDING` |

```json
{
  "code": 0,
  "data": {
    "uuid": "a1b2c3d4",
    "qrUrl": "https://auth.example.com/qrc/api/v1/t/a1b2c3d4",
    "expireIn": 300,
    "intent": "OAUTH_DEVICE",
    "status": "PENDING"
  }
}
```

---

### POST `/qrc/api/v1/ticket/open/status`

轮询票据状态（匿名）。须携带与建票一致的 `clientId` / `clientSecret`；服务端校验票据归属。

**请求**

```json
{
  "data": {
    "uuid": "a1b2c3d4",
    "clientId": "my-app",
    "clientSecret": "secret"
  }
}
```

**响应 `data`**：同 `TicketStatusResult`（§2.6）。

**轮询建议**

- 间隔：参考全局配置 `pollIntervalMs`（默认 2000ms）
- 停止条件：`status` 为终态，或已取得 `result.code` / `result.accessToken`
- `COMPLETED` 后**立即**用 `code` 调 `/oauth2/token`（授权码一次性、短 TTL）

---

### POST `/qrc/api/v1/ticket/open/cancel`

取消未完成的票据（匿名）。

**请求**

```json
{
  "data": {
    "uuid": "a1b2c3d4",
    "clientId": "my-app",
    "clientSecret": "secret"
  }
}
```

**响应 `data`**：`status` 为 `CANCELLED`。

---

## 3. PC 网页登录（`ScanTicketController`，`/qrc/scanticket/web`）

匿名；返回 **`R`**。供 `login.html` 等同源页面使用。

### POST `/qrc/scanticket/web/ticket/create`

**请求**

```json
{
  "data": {
    "intent": "SELF_WEB_LOGIN",
    "payload": {}
  }
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `intent` | 否 | 默认 `SELF_WEB_LOGIN` |
| `payload` | 否 | 扩展载荷 |

**响应 `data`**：同 `TicketCreateResult`（§2 open/create）。

---

### GET `/qrc/scanticket/web/ticket/status?uuid={uuid}`

**响应 `data`**：同 `TicketStatusResult`（§2.6）。

PC 前端应在 `SCANNED` 展示 `scannerBrief`；在 `CONFIRMED` 且存在 `exchange` 时调用 `session/exchange`。

---

### POST `/qrc/scanticket/web/session/exchange`

用一次性 `exchange` 建立 **Shiro Session**（同源 Cookie）。

**请求**

```json
{
  "data": {
    "exchange": "e5f6g7h8-one-time-token",
    "rememberMe": false
  }
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `exchange` | 是 | confirm 返回的一次性令牌 |
| `rememberMe` | 否 | 是否记住登录（Shiro RememberMe） |

**响应 `data`**：登录后跳转路径（string），由 `SysAuthSupport.resolvePostLoginRedirect` 解析（可能为 `/`、SavedRequest 或账号配置默认页）。

```json
{
  "code": 0,
  "msg": "success",
  "data": "/index.html"
}
```

> `exchange` **一次性**、短 TTL（默认 60s，配置项 `exchangeTokenTtlSeconds`）；消费后不可复用。

---

## 4. OAuth 授权页（非 `/qrc` 前缀）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/oauth2/authorize?response_type=code&client_id=...&redirect_uri=...&state=...` | 未登录且 `oauthQrFirst=true` 时返回 `login.html` + 预建 `OAUTH_AUTHORIZE` 票 |
| POST | `/oauth2/login` | OAuth 表单登录（`login.html` 账号 Tab） |

扫码确认后浏览器侧同样走 `/qrc/scanticket/web/session/exchange` 建立 Session，再继续 OAuth 回跳。

---

## 5. Webhook（`delivery=WEBHOOK`）

Autumn POST 到 `qrc_client_grant.webhook`：

**Header**

| 名称 | 说明 |
|------|------|
| `Content-Type` | `application/json;charset=UTF-8` |
| `X-Qrc-Event` | `qrc.authorized` |
| `X-Qrc-Timestamp` | 毫秒时间戳 |
| `X-Qrc-Signature` | HMAC-SHA256 hex（body 原文 + `secret`） |

**Body**

```json
{
  "event": "qrc.authorized",
  "uuid": "a1b2c3d4",
  "timestamp": 1710000000000,
  "data": {
    "code": "auth-code",
    "state": "csrf-random",
    "clientId": "my-app"
  }
}
```

---

## 6. 错误码（业务）

| code | 说明 | 客户端建议 |
|------|------|------------|
| 8600 | intent 无效 | 检查建票参数 |
| 8601 | 不支持的 Intent | — |
| 8602 | 无效的 client_id | 检查 OAuth 注册 |
| 8603 | client 未 trusted | 联系管理员 |
| 8604 | client 已归档 | — |
| 8605 | redirect_uri 为空 | 补全 redirect |
| 8606 | client 未配置 redirect | 后台配置 |
| 8607 | redirect_uri 不匹配 | 必须与注册值**完全一致** |
| 8608 | client_secret 错误 | 检查密钥 |
| 8609 | open API 缺少 client 凭证 | body 补全 |
| 8610 | 票据不存在或已过期 | 重新建票/扫码 |
| 8611 | 票据已过期 | 重新建票 |
| 8612 | 状态不可扫码 | 已扫/已结束 |
| 8613 | 须已登录用户扫码 | APP 先登录 |
| 8614 | 已拒绝或取消 | 重新建票 |
| 8615 | 状态不可确认 | 检查流程顺序 |
| 8616 | 须已登录用户确认 | APP 先登录 |
| 8617 | 扫码与确认用户不一致 | 同一账号操作 |
| 8618 | 无权取消票据 | clientId 不匹配 |
| 8619 | 无权查询票据 | open/status client 与建票不一致 |
| 8620 | exchange 为空 | — |
| 8621 | exchange 无效或过期 | 重新 confirm |
| 8622 | exchange 对应用户不可用 | — |
| 8623 | 用户不可用 | 账号禁用等 |
| 8630 | 客户端未启用 QRC | 配置 `qrc_client_grant.enabled` |

---

## 7. OAuth 衔接

第三方从 `result.code` 或 `redirect` 取得授权码后：

```
POST /oauth2/token
grant_type=authorization_code
&code={code}
&client_id={clientId}
&client_secret={clientSecret}
&redirect_uri={redirectUri}
```

用户信息：`GET /oauth2/userInfo`（详见 **`docs/AI_OAUTH_INTEGRATION.md`**）。

---

## 8. QR 内容规范

```
https://{auth-host}/qrc/api/v1/t/{uuid}
autumn://qrc/t/{uuid}
```

- QR **仅**含 `uuid` 路由，不含 client 密钥与用户 token
- APP 解析 `uuid` 后调用 §2 中带 `@Authenticated` 的接口
- Deep Link scheme 白名单见 `qrc_client_grant.schemes`
