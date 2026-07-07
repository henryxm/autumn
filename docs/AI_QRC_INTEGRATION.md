# QRC 第三方集成标准

> **OAuth 授权码 / token / userInfo 完整对接**（含用户信息字段、账号绑定、示例代码）见 **`docs/AI_OAUTH_INTEGRATION.md`**。
>
> 本文侧重 **QRC 扫码分支**与 **Open API 自建 QR**。HTTP 字段与错误码见 **`docs/AI_QRC_API.md`**。

---

## 1. 集成模式总览

| 模式 | 适用 | QR 谁生成 | 结果怎么拿 |
|------|------|-----------|------------|
| **A. 浏览器 Redirect（B1）** | 第三方 Web | Autumn `/oauth2/authorize` | `redirect_uri?code=` |
| **B. 同源 Web 扫码（B2）** | Autumn 系站点 PC 登录 | PC 调 `/qrc/scanticket/web/ticket/create` | `exchange` → Session |
| **C. 第三方自建 QR（B3）** | Native / 自建 UI | 第三方调 `/qrc/api/v1/ticket/open/create` | 按 `delivery` 轮询/Webhook/DeepLink |
| **D. Autumn 系 RP Web（B3 代理）** | 另一 Autumn 站点作 RP | 浏览器调 **`/client/oauth2/qrc/web/ticket/create`** | `complete` → **`WebOauthLoginService`**（与 B1 callback 同编排） |

> **D 模式**与 **`AI_AUTH_LOGIN_MODES.md` §3.5** 方式一共用 `WebOauthBindService`；完整整合说明、架构图与配置见 **`docs/AI_AUTH_SITE_ROLES.md`**。

---

## 2. Autumn 系平台（B2）

1. 在 AS 注册 OAuth Client（`oauth_client_details`，`trusted=1`）
2. （可选）配置 `qrc_client_grant`：`enabled`、`delivery`、`consent` 等
3. PC 登录页：`POST /qrc/scanticket/web/ticket/create` → 展示 `qrUrl`
4. 用户用官方 APP 扫码确认（`AI_QRC_CLIENT_API.md`）
5. PC 轮询 `status` → `session/exchange`

OAuth 浏览器授权（B1）仍走 `/oauth2/authorize`，未登录时预建 `OAUTH_AUTHORIZE` 票，UI 与 B2 共用 `login.html`。

---

## 3. 非 Autumn 平台

无需部署 Autumn 代码，仅注册 OAuth Client + 配置 `qrc_client_grant`。

### 3.1 模式 A：浏览器 Redirect（B1）

1. 浏览器打开：
   ```
   GET /oauth2/authorize?response_type=code&client_id={id}&redirect_uri={uri}&state={state}
   ```
2. 用户在同一 `login.html` 完成扫码或密码登录
3. 回调：`{redirect_uri}?code={code}&state={state}`
4. 服务端：`POST /oauth2/token` 换 `access_token`
5. `GET /oauth2/userInfo`

### 3.2 模式 B：第三方自建 QR（B3）

#### 步骤

1. **建票**（服务端，保管 `client_secret`）

   ```http
   POST /qrc/api/v1/ticket/open/create
   Content-Type: application/json

   {
     "data": {
       "clientId": "my-app",
       "clientSecret": "secret",
       "redirectUri": "https://tp.example.com/callback",
       "scope": "basic",
       "state": "random-csrf"
     }
   }
   ```

2. **展示 QR**：使用响应 `qrUrl`（或自行编码 `uuid`）

3. **用户**用 Autumn APP 扫码 → scan → confirm

4. **按 `delivery` 取结果**（见 §4）

5. **换 token / 拉用户**（`POLL_CODE` / Webhook）

   ```
   POST /oauth2/token
   grant_type=authorization_code&code=...&client_id=...&client_secret=...&redirect_uri=...
   ```

#### 轮询示例（POLL_CODE）

```text
interval = 2000ms  // 或读服务端 QRC_CONFIG.pollIntervalMs
deadline = now + expireIn

loop while now < deadline:
  res = POST /qrc/api/v1/ticket/open/status {
    uuid, clientId, clientSecret
  }
  if res.code != 0: handle error; break

  status = res.data.status
  if status == "COMPLETED" and res.data.result.code:
    exchangeCodeForToken(res.data.result.code)
    break
  if status in ("DENIED", "CANCELLED", "EXPIRED"):
    showFailure(status)
    break
  sleep(interval)
```

#### 取消未完成的票

用户关闭 QR 页时可选：

```json
POST /qrc/api/v1/ticket/open/cancel
{ "data": { "uuid": "...", "clientId": "...", "clientSecret": "..." } }
```

---

## 4. delivery 选型与行为

| delivery | 第三方如何拿授权结果 | APP 额外动作 |
|----------|---------------------|--------------|
| `POLL_CODE` | 轮询 `open/status`，读 `result.code` | 无 |
| `POLL_TOKEN` | 轮询读 `result.accessToken` | 无；直接调 userInfo |
| `WEBHOOK` | 接收 POST `qrc.authorized` | 无 |
| `DEEP_LINK` | 轮询或 APP 打开 `result.deepLink` | confirm 后 `openURL(scheme://oauth/callback?code=...)` |

配置表：`qrc_client_grant.delivery`；DeepLink 须在 `schemes` 白名单。

---

## 5. 对接检查清单

**OAuth 基础**

- [ ] `client_id` / `client_secret` 已登记且 `trusted=1`
- [ ] `redirect_uri` 与注册值**完全一致**（大小写、尾斜杠均敏感）
- [ ] 使用随机 `state` 并在 token 前校验
- [ ] `code` 短 TTL、一次性，拿到后立即 `/oauth2/token`

**QRC 专项**

- [ ] `qrc_client_grant.enabled=1`
- [ ] `delivery` 与集成方式一致
- [ ] Webhook 校验 `X-Qrc-Signature`（HMAC-SHA256 hex）
- [ ] QR 内容**不含** `client_secret` 或长期 token
- [ ] Open API 轮询使用建票同一 `clientId`
- [ ] APP 流程含 scan + 用户 confirm（不可跳过）

**联调验证**

- [ ] `open/create` 返回 `qrUrl` 可被 APP 解析
- [ ] confirm 后 `open/status` 达到 `COMPLETED`（或对应 delivery 字段就绪）
- [ ] `POLL_CODE` 路径 `/oauth2/token` 成功
- [ ] 用户拒绝时 status 为 `DENIED`

---

## 6. 禁止事项

- QR 或前端页面中暴露 `client_secret`、用户 uuid、长期 access_token
- 自造平行 Token 体系（须走 `/oauth2/token`）
- 跳过 APP **confirm**（仅 scan 即视为授权）
- 忽略 `state` 校验

---

## 7. 相关文档

| 文档 | 内容 |
|------|------|
| `AI_QRC_API.md` | 全部 HTTP 端点、字段、错误码 |
| `AI_QRC_CLIENT_API.md` | APP 扫码 UX 与伪代码 |
| `AI_QRC.md` | 模块结构、配置、审计 |
| `AI_OAUTH_INTEGRATION.md` | OAuth2 标准 token / userInfo |
