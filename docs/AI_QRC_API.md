# QRC 开放 API 参考

> 模块前缀：`/qrc`。成功响应 `code=0`（`Response`/`R` 包装）。

## 1. PC Web（匿名，`ScanWebController`）

### POST `/qrc/web/v1/ticket/create`

请求：

```json
{ "data": { "intent": "SELF_WEB_LOGIN", "payload": {} } }
```

响应 `data`：`uuid`, `qrUrl`, `expireIn`, `intent`, `status`

### GET `/qrc/web/v1/ticket/status?uuid=`

响应 `data`：`status`, `exchange`, `redirect`, `result`（含 `code`/`accessToken`/`redirectUri`）

### POST `/qrc/web/v1/session/exchange`

请求：

```json
{ "data": { "exchange": "...", "rememberMe": false } }
```

响应：框架 `R`，`data` 为登录后跳转路径（`ScanLoginToken` → Shiro Session）。

## 2. QR 链接（匿名，`ScanLinkController`）

### GET `/qrc/v1/t/{uuid}`

校验票据存在，返回公开字段（供 APP/Universal Link 可选调用）：

```json
{ "code": 0, "data": { "uuid": "...", "intent": "...", "status": "PENDING" } }
```

APP 仍应调用 §3 的 detail/scan/confirm 完成授权。

## 3. APP（`@Authenticated`，`ScanAppApiController`）

Header：`Token` 或 `Authorization: Bearer {userToken}`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/qrc/api/v1/ticket/{uuid}` | 确认页详情（含 `scopeLabels`） |
| POST | `/qrc/api/v1/ticket/scan` | `{ "data": { "uuid" } }` |
| POST | `/qrc/api/v1/ticket/confirm` | 确认授权 |
| POST | `/qrc/api/v1/ticket/deny` | 拒绝 |

## 4. 第三方 Open API（`ScanOpenApiController`）

### POST `/qrc/open/v1/ticket/create`

```json
{
  "data": {
    "clientId": "...",
    "clientSecret": "...",
    "redirectUri": "https://tp/callback",
    "scope": "basic",
    "state": "csrf"
  }
}
```

创建 `OAUTH_DEVICE` 票据。

### GET `/qrc/open/v1/ticket/status`

Query：`uuid`, `clientId`, `clientSecret`

`COMPLETED` 时 `result.code` 或 `result.accessToken`（按 `delivery`）。

### POST `/qrc/open/v1/ticket/cancel`

Query：`clientId`, `clientSecret`；Body：`{ "data": { "uuid" } }`

## 5. OAuth 授权页（非 `/qrc` 前缀）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/oauth2/authorize?response_type=code&client_id=...&redirect_uri=...&state=...` | 未登录且 `oauthQrFirst=true` 时返回 `login.html` + 预建票 |
| POST | `/oauth2/login` | OAuth 表单登录（`login.html` 账号 Tab） |

consent 建票失败返回 OAuth 错误，**不**静默发 `code`。

## 6. Webhook（delivery=WEBHOOK）

Autumn POST 到 `webhook`：

Header：`X-Qrc-Event: qrc.authorized`，`X-Qrc-Signature`（HMAC-SHA256 hex，密钥为 `secret`）

Body：

```json
{
  "event": "qrc.authorized",
  "uuid": "...",
  "timestamp": 1710000000000,
  "data": { "code": "...", "state": "...", "clientId": "..." }
}
```

## 7. 错误码（业务）

| code | 说明 |
|------|------|
| 8600 | intent 无效 |
| 8601 | 不支持的 Intent |
| 8602～8608 | OAuth 客户端/密钥/redirect 校验失败 |
| 8610～8618 | 票据状态/权限 |
| 8620～8623 | exchange / 用户不可用 |
| 8630 | 客户端未启用 QRC |

## 8. OAuth 衔接

第三方拿到 `code` 后：

`POST /oauth2/token`，`grant_type=authorization_code`，与标准 OAuth 一致。

用户信息：`GET /oauth2/userInfo`。
