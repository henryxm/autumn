# 扫码登录标准（Autumn 框架）

> **版本线**：Autumn 2.0.0（master）/ 3.0.0（`3.0.0` 分支）API 契约一致。  
> **唯一入口**：本文定义两套标准流程；细节见子文档索引 §8。  
> **流程图 / 时序图 / 拓扑**：**`docs/AI_SCAN_LOGIN_FLOWS.md`**（与当前代码一致）。  
> **进程内调用**：`cn.org.autumn.modules.client.service.ScanLoginFacade`（见 §4）。

---

## 1. 角色模型

| 角色 | 说明 | 典型实例 |
|------|------|----------|
| **AS（Authorization Server）** | 签发 OAuth 授权码 / token，托管用户体系 | a.com（A应用） |
| **RP（Relying Party）** | 接入外部 AS，在本地建立会话 | b.com（B网站） |
| **第三方服务端** | 非 Autumn 应用，持 `client_id` / `client_secret` 调 Open API | 自建后台 |
| **APP（扫码端）** | 已登录用户扫码并确认 | A应用客户端 |

**与 RSA / 传输加密无关**：账号密码表单的 `AccountUniformRsaService`、OAuth `EncryptInterceptor` 与 QRC 扫码通道独立；排查扫码失败时不要查 RSA 密钥。

---

## 2. 标准一：网页授权登录（Web）

适用于浏览器登录页、OAuth 授权页、Autumn 系 RP 联邦扫码（B网站 ← A应用）。

### 2.1 子模式选型

| 子模式 | 场景 | 建票入口 | 完成登录 |
|--------|------|----------|----------|
| **B1** | 第三方 Web Redirect | `GET /oauth2/authorize` | `redirect_uri?code=` → token → userInfo |
| **B2** | 同源 Autumn PC 扫码 | `POST /qrc/scanticket/web/ticket/create` | `POST .../session/exchange` |
| **D** | Autumn RP 联邦扫码 | `POST /client/oauth2/qrc/web/ticket/create` | SSE `GET .../ticket/stream` → 自动 `completeRemoteOAuthCallback` |

前端统一使用 **`autumn-qrc-core.js`**：

- `mode: 'as'` → B2（`/qrc/scanticket/web/*`）
- `mode: 'rp'` → D（`/client/oauth2/qrc/web/*`，**SSE** `GET /ticket/stream`）
- **跨站 RP 联邦**：QR 内容为 **AS 域名**（如 `https://a.com/qrc/api/v1/t/{uuid}`），不得使用 RP 本域自建 QR。

### 2.2 B网站 ← A应用 联邦时序（D 模式 · 双 Webhook + SSE）

1. B网站登录页 `AutumnQrc.createMethods({ mode: 'rp', type, id })` 或 `pageLogin=2|3` 的 `qrProviders`
2. `POST /client/oauth2/qrc/web/ticket/create` → RP 代理 AS `open/create`，payload 注入 `delivery=WEBHOOK`、`webhook={B}/client/oauth2/qrc/web/inbound`，并绑定 `browserSessionId` → 返回 `qrUrl`（**a.com 域**）
3. 浏览器建立 **一条** `GET /client/oauth2/qrc/web/ticket/stream?uuid=`（SSE），连接时 catch-up 当前状态
4. A应用扫 QR → `scan` → AS `POST` Webhook `qrc.scanned` → B `inbound` → SSE 推送 `SCANNED` + `scannerBrief`（「请在手机点击确认」）
5. A应用 `confirm` → AS `POST` Webhook `qrc.authorized` → B `inbound` → 后台按 `browserSessionId` 自动 `completeRemoteOAuthCallback` → SSE 推送 `COMPLETED` + `redirectUrl` → 浏览器跳转
6. **全程无轮询**：B 前端不调用 `local-status` / `ticket/status` / `ticket/complete`；B 后台不 HTTP 调用 AS `open/status`
7. Session 绑定失效时 SSE 推送 `SESSION_EXPIRED`，前端提示刷新二维码

完整配置见 **`docs/AI_AUTH_SITE_ROLES.md` §3**。

### 2.3 路径说明：`/client/oauth2/qrc/web/*`

路径中的 `web` 表示 **RP 浏览器登录编排**（含 Session 建立与绑定选择）。当前 D 模式端点：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ticket/create` | 代理 AS 建票，绑定 `browserSessionId` |
| GET | `/ticket/stream` | **SSE** 推送状态（唯一浏览器推送通道） |
| POST | `/ticket/cancel` | 取消 |
| POST | `/inbound` | AS Webhook 入站（服务端回调，非浏览器） |

完整时序与拓扑见 **`docs/AI_SCAN_LOGIN_FLOWS.md` §3.3**。

---

## 3. 标准二：服务端建票轮询（无网页 UI）

适用于第三方后台、Native 壳、自建 UI：不打开 Autumn 登录页，由**服务端**建票、展示 QR、轮询结果。

### 3.1 非 Autumn 第三方（B3，直连 AS）

**前提**：在 AS 登记 `oauth_client_details`，并配置 `qrc_client_grant`（`enabled=1`，`delivery=POLL_CODE`）。

#### 建票

```http
POST /qrc/api/v1/ticket/open/create
Content-Type: application/json

{
  "data": {
    "clientId": "my-app",
    "clientSecret": "secret",
    "redirectUri": "https://tp.example.com/oauth/callback",
    "scope": "basic",
    "state": "csrf-random"
  }
}
```

响应 `data.qrUrl` 用于生成二维码；`data.uuid` 用于轮询。

#### 轮询

```http
POST /qrc/api/v1/ticket/open/status
Content-Type: application/json

{
  "data": {
    "uuid": "{uuid}",
    "clientId": "my-app",
    "clientSecret": "secret"
  }
}
```

#### 伪代码

```text
interval = 2000ms   // 或读 QRC_CONFIG.pollIntervalMs
deadline = now + expireIn

loop while now < deadline:
  res = POST open/status { uuid, clientId, clientSecret }
  if res.code != 0: break
  status = res.data.status
  if status == "COMPLETED" and res.data.result.code:
    POST /oauth2/token (authorization_code)
    GET /oauth2/userInfo
    break
  if status in ("DENIED", "CANCELLED", "EXPIRED"):
    break
  sleep(interval)
```

#### 取消

```http
POST /qrc/api/v1/ticket/open/cancel
{ "data": { "uuid": "...", "clientId": "...", "clientSecret": "..." } }
```

### 3.2 Autumn RP 站点（浏览器 D 模式）

| 方式 | 说明 |
|------|------|
| **HTTP** | 浏览器：`POST create` + `GET ticket/stream`（SSE）；详见 **`AI_SCAN_LOGIN_FLOWS.md` §3.3** |
| **进程内** | `ScanLoginFacade.createRpTicket` / `streamRpTicket` / `handleRpInbound` |

服务端若需持票换 `code` 换 token、**无** RP 浏览器 Session，应使用 **B3** 直连 AS `open/*`（`delivery=POLL_CODE` 或 `WEBHOOK`），而非 D 模式 Web 路径。

### 3.3 delivery 选型

| delivery | 取结果方式 |
|----------|------------|
| `POLL_CODE` | 轮询 `open/status`，读 `result.code` → `/oauth2/token` |
| `WEBHOOK` | AS 回调 `webhook` URL（见 `AI_QRC_INTEGRATION.md` §4） |
| `DEEP_LINK` | APP confirm 后 `result.deepLink` |

---

## 4. Autumn 应用内集成（ScanLoginFacade）

业务模块（account、shop、console 等）**不得**重复实现建票/轮询协议，应注入：

```java
@Autowired
ScanLoginFacade scanLoginFacade;

// B2 同源 Web
TicketCreateResult as = scanLoginFacade.createAsWebTicket(request, ticketCreateRequest);
TicketStatusResult status = scanLoginFacade.pollAsWebStatus(uuid);
String redirect = scanLoginFacade.exchangeAsWebSession(exchangeRequest, request);

// D RP 联邦（双 Webhook + SSE）
TicketCreateResult rp = scanLoginFacade.createRpTicket(request, callback);
TicketCreateResult rpByCred = scanLoginFacade.createWebTicketByCredential(request, "oauth2_classic", clientId, callback);
TicketCreateResult openRp = scanLoginFacade.createWebTicketByCredential(request, "oauth2_open", appId, callback);
SseEmitter stream = scanLoginFacade.streamRpTicket(uuid);
scanLoginFacade.handleRpInbound(rawBody, headers);

// B2 开放同源（OAUTH_DEVICE + complete）
TicketCreateResult openAs = scanLoginFacade.createWebTicketByCredential(request, "oauth2_open", appId, callback);
String openRedirect = scanLoginFacade.completeOpenWebLogin(request, "oauth2_open", appId, code, callback);

// B3 Open API（AS 侧或集成测试）
TicketCreateResult open = scanLoginFacade.createOpenTicket(openCreateRequest, request);
TicketStatusResult openStatus = scanLoginFacade.pollOpenTicket(openStatusRequest);
```

HTTP 层：`ClientOauth2QrcController`、`ScanTicketController`、`QrcApiController` 均委托同一门面或等价 Service。

---

## 5. 配置清单

### 5.1 系统参数

| 键 | 说明 |
|----|------|
| `AUTH_SITE_CONFIG` | `siteRole`：`AS_ONLY` / `RP_ONLY` / `AS_AND_RP`；`qrcWebMode`：`auto` / `as` / `rp` |
| `LOGIN_AUTHENTICATION` | `oauth2:{clientId}` — RP 默认 OAuth 客户端 |
| `QRC_CONFIG` | 票据 TTL、轮询间隔、`oauthQrFirst` 等 |

### 5.2 数据表

| 表 | 角色 |
|----|------|
| `oauth_client_details` | AS 侧 OAuth 客户端 |
| `qrc_client_grant` | Open API 扫码策略（`enabled`、`delivery`） |
| `client_web_authentication` | RP 凭证：`originUri`、`clientId`、`clientSecret`、`redirectUri` |
| `client_web_oauth_bind` | 经典跨站用户绑定 |
| `opc_connect_app` | 开放接入应用（`pageLogin`、`platformBaseUrl`） |
| `opc_connect_bind` | 开放接入用户绑定（`openId`） |
| `opl_open_app` | OPL 应用登记（扫码 `appId` 发 OPL 授权码时须活跃） |

**开放同源 B2 额外要求**：`appId` 同时存在于 `oauth_client_details`（trusted）与 `opl_open_app`（活跃）。

### 5.3 B网站（RP）+ A应用（AS）检查清单

**a.com（AS）**

- [ ] `oauth_client_details`：`b-web`，`redirect_uri=https://b.com/client/oauth2/callback`，`trusted=1`
- [ ] `qrc_client_grant`：`enabled=1`（建票 payload 可覆盖 `delivery=WEBHOOK`）

**b.com（RP）**

- [ ] `AUTH_SITE_CONFIG.siteRole` = `RP_ONLY` 或 `AS_AND_RP` + `qrcWebMode=rp`
- [ ] `LOGIN_AUTHENTICATION` = `oauth2:b-web`
- [ ] `client_web_authentication`：`originUri=https://a.com`，凭证与 AS 一致；`pageLogin=2` 或 `3` 开启登录页扫码
- [ ] 登录页加载 `autumn-qrc-core.js`；联邦入口 `mode: 'rp'` 或消费 `qrProviders`

**验收**：`POST /client/oauth2/qrc/web/ticket/create` 返回的 `qrUrl` **主机名必须为 a.com**；扫码后 B 收到 `qrc.scanned` / `qrc.authorized` 入站，SSE `ticket/stream` 推送 `SCANNED` → `COMPLETED`。

---

## 6. 错误排查

| 现象 | 常见原因 |
|------|----------|
| QR 指向 B 本域 | 误用 B2 或业务仓遗留自建 QR；应走 D / `mode: 'rp'` |
| SSE 无 `SCANNED` | AS 未能 POST `qrc.scanned` 到 B `inbound`；检查网络与 `X-Qrc-Signature` |
| SSE `SESSION_EXPIRED` | 建票浏览器 Session 已过期；刷新二维码重试 |
| SSE 无 `COMPLETED` | `qrc.authorized` 未到达或 Session 绑定失败 |
| `当前站点未启用 RP 角色` | `AUTH_SITE_CONFIG.siteRole` 不含 RP |
| `未配置 RP OAuth 客户端` | 缺 `LOGIN_AUTHENTICATION` 或 `client_web_authentication` |
| 建票失败 / 解密错误 | `client_secret` 与 AS 不一致；grant 未启用 |
| 扫码后无法登录 | `redirect_uri` 与 AS 登记不一致；绑定冲突见 bind/choice |
| RSA 解密失败 | **与扫码无关**；检查密码登录 `publicKey` 流程 |

---

## 7. 状态机（摘要）

```
PENDING → SCANNED → CONFIRMED → COMPLETED
         ↘ DENIED / CANCELLED / EXPIRED
```

| 子模式 | COMPLETED 后 |
|--------|----------------|
| **B2** | PC `session/exchange` 换 Shiro Session |
| **D** | 入站 `qrc.authorized` 自动 OAuth；SSE 推送 `redirectUrl` |
| **B3** | 轮询/Webhook 取 `result.code` → `/oauth2/token` |

图示见 **`docs/AI_SCAN_LOGIN_FLOWS.md` §6**。

---

## 8. 子文档索引

| 文档 | 内容 |
|------|------|
| **`docs/AI_SCAN_LOGIN_FLOWS.md`** | **时序图、拓扑图、鉴权流程（与代码一致）** |
| **`docs/AI_SCAN_LOGIN_DUAL_MODE_REGRESSION.md`** | **经典 OAuth × 开放 OPC 扫码回归评估** |
| **`docs/AI_AUTH_SITE_ROLES.md`** | AS/RP 双角色、联邦配置与时序 |
| **`docs/AI_QRC_API.md`** | HTTP 端点、报文、错误码 |
| **`docs/AI_QRC_INTEGRATION.md`** | 第三方集成模式 A～D |
| **`docs/AI_QRC_CLIENT_API.md`** | APP scan/confirm |
| **`docs/AI_OAUTH_INTEGRATION.md`** | OAuth2 token、userInfo、绑定 |
| **`docs/AI_AUTH_LOGIN_MODES.md`** | 双轨授权总览 |
| **`docs/AI_QRC.md`** | 模块结构与配置 |

集成测试：`web/.../ScanLoginIntegrationTest`、`RpFederatedLoginIntegrationTest`、`OpenApiServerScanLoginIntegrationTest`、`ScanLoginFacadeIntegrationTest`。
