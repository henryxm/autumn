# Autumn 扫码登录（QRC）对接指南

> 版本线：**Autumn 2.0.0（master）**。API 细节见 **`docs/AI_QRC_API.md`**；第三方集成见 **`docs/AI_QRC_INTEGRATION.md`**；**OAuth 授权码 / userInfo 完整对接见 `docs/AI_OAUTH_INTEGRATION.md`**；APP 示例见 **`docs/AI_QRC_CLIENT_API.md`**。

## 1. 文档导读

| 读者 | 推荐阅读顺序 |
|------|--------------|
| 框架 / 后端维护 | 本文 → `AI_QRC_API.md` |
| **第三方平台（服务端）** | `AI_OAUTH_INTEGRATION.md` → `AI_QRC_INTEGRATION.md` → `AI_QRC_API.md` §2 open/* |
| **Autumn APP / 移动端** | `AI_QRC_CLIENT_API.md` → `AI_QRC_API.md` §0～§2 |
| **PC 前端（login.html）** | 本文 §5 → `AI_QRC_API.md` §3 |

### 快速索引

| 需求 | 文档章节 |
|------|----------|
| APP 怎么扫、怎么 confirm | `AI_QRC_CLIENT_API.md` §2、§7 |
| 第三方自建 QR + 轮询 code | `AI_QRC_INTEGRATION.md` §3.2 + `AI_QRC_API.md` open/* |
| 字段含义 / 错误码 | `AI_QRC_API.md` §2.6、§6 |
| 票据状态流转 | `AI_QRC_API.md` §1 |
| Webhook 验签 | `AI_QRC_API.md` §5 |

## 2. 能力概览

QRC 模块（`cn.org.autumn.modules.qrc`）是**扫码交互编排层**，复用现有：

- **Shiro Session**（同源 PC 登录）
- **OAuth2 AS**（`/oauth2/authorize`、`/oauth2/token`）
- **APP 鉴权**（`@Authenticated` + `UserTokenService`）

### Intent 场景

| Intent | 说明 | Handler |
|--------|------|---------|
| `SELF_WEB_LOGIN` | APP 已登录，扫 PC 网站码，换 Web Session | `SelfWebLoginHandler` |
| `OAUTH_AUTHORIZE` | 第三方浏览器跳授权页，APP 扫码登录并回跳 `code` | `OAuthAuthorizeHandler` |
| `OAUTH_CONSENT` | 浏览器已登录，APP 二次确认 scope | `OAuthConsentHandler` |
| `OAUTH_DEVICE` | 第三方自建 QR，Open API 轮询/Webhook/deep link | `OAuthDeviceHandler` |

## 3. 代码结构（与实现一致）

| 层次 | 类/路径 | 职责 |
|------|---------|------|
| 编排 | `ScanTicketService` | 票据生命周期、Redis/锁、exchange、`requireActiveUser` |
| OAuth 桥 | `ClientGrantService` | 发 code/token、Webhook、redirect、`requireEnabledGrant` |
| 展示 | `IntentDisplaySupport` | confirm 页 `intentTitle` / `intentHint` / `deviceHint` |
| 工具 | `TicketPayloads` | payload 空安全读取 |
| 组装 | `QrcApiSupport` | Open API detail、client 校验、建票上下文 |
| 组装 | `ScanWebSupport` | PC 建票上下文、session exchange |
| SPI | `IntentHandler` + `IntentHandlerRegistry` | 按 Intent 分发 |
| SPI | `ConsentProvider` + `ConsentSupport` | 可选 scope 文案（无实现时回退 `scope` 拆分） |
| 开放 API | `QrcApiController` | APP / Open API / QR 链接，`/qrc/api/v1`，`Request`/`Response` |
| PC 网页 | `ScanTicketController` | 登录页建票/轮询/exchange，`/qrc/scanticket/web`，`R` |
| 后台 | `ScanTicketControllerGen` | 票据审计 CRUD，`/qrc/scanticket`，`R` + 权限 |
| OAuth 页 | `AuthorizationController` + `login.html` | 未登录 authorize / consent 统一 UI |
| 实体 | `ScanTicketEntity`、`ClientGrantEntity` | 审计与 client 策略（实体驱动建表） |
| 配置 | `ScanLoginConfig`（`QRC_CONFIG`） | 全局 TTL/轮询/OAuth QR 优先 |

## 4. 登录入口（`login.html`）

| 入口 | 行为 |
|------|------|
| `/login.html` | 三 Tab：账号 / 扫码 / 手机；普通登录走 `/sys/login` |
| 配置 OAuth client 后 | 账号/手机 Tab POST `/oauth2/login` |
| `/oauth2/authorize` 未登录 | 渲染同一 `login.html`（`oauthAuthorize=true`），预建 `OAUTH_AUTHORIZE` 票据 |
| 已登录 + `consent=true` | 同上，`mode=consent`，预建 `OAUTH_CONSENT` 票据 |
| SPM `qrc/authorize` | 跳转桩 → `login.html`（保留 query） |

**安全**：consent/authorize 建票失败时**不会**静默发放 OAuth `code`（consent 返回错误；authorize 降级账号登录并提示）。

## 5. 快速开始（同源 PC 登录）

1. PC：`POST /qrc/scanticket/web/ticket/create`，body `{ "data": { "intent": "SELF_WEB_LOGIN" } }`
2. 展示返回的 `qrUrl`
3. APP：`POST /qrc/api/v1/ticket/scan` → `POST /qrc/api/v1/ticket/confirm`（Header 带用户 token）
4. PC 轮询：`GET /qrc/scanticket/web/ticket/status?uuid=...`
5. PC：`POST /qrc/scanticket/web/session/exchange`，body `{ "data": { "exchange": "..." } }`

## 6. QR 内容规范

```
https://{auth-host}/qrc/api/v1/t/{uuid}
autumn://qrc/t/{uuid}
```

- APP **优先本地解析** URL 得到 `uuid`，再调 `/qrc/api/v1/*`
- 可选：`GET /qrc/api/v1/t/{uuid}` 校验票据并返回 `{ uuid, intent, status }`（不含 secret/token）
- OAuth client、redirect_uri 等**仅存服务端**，不得写入 QR

## 7. 配置（sys_config `QRC_CONFIG`）

| 字段 | 默认 | 说明 |
|------|------|------|
| `ticketTtlSeconds` | 300 | 票据 TTL |
| `exchangeTokenTtlSeconds` | 60 | PC 交换 TTL（配置键名保留，API 字段为 `exchange`） |
| `pollIntervalMs` | 2000 | 前端 / Open API **建议**轮询间隔 |
| `webhookTimeoutMs` | 5000 | Webhook HTTP 超时 |
| `maxPollPerMinute` | 120 | 配置项保留；**当前版本未在服务端强制限流** |
| `oauthQrFirst` | true | authorize 未登录优先 QR 页 |

## 8. 客户端 QRC 策略（表 `qrc_client_grant`）

按 `client_id` 配置（Java 字段均为单词，`clientId` 除外）：

| 字段 | 说明 |
|------|------|
| `enabled` | 是否允许扫码授权 |
| `delivery` | `POLL_CODE` / `POLL_TOKEN` / `WEBHOOK` / `DEEP_LINK` |
| `webhook` / `secret` | Webhook 地址与 HMAC 密钥 |
| `schemes` | DeepLink scheme 白名单（CSV） |
| `scopes` | 允许的 scope CSV |
| `consent` | 浏览器已登录时仍要 APP 确认 |
| `updated` | 最后更新时间 |

## 9. 审计表（`qrc_scan_ticket`）

| 字段 | 说明 |
|------|------|
| `uuid` | 票据业务主键 |
| `intent` / `status` | 意图与状态 |
| `clientId` | OAuth client（可空） |
| `scanner` / `subject` | 扫码用户 / 授权主体 uuid |
| `ip` / `agent` | 来源 IP / UA |
| `payload` / `result` | JSON 文本 |
| `created` / `expired` / `completed` | 时间 |

## 10. 安全基线

- 扫码 ≠ 登录，必须 **confirm**
- ticket / `exchange` **一次性**；状态迁移使用分布式锁
- Open API 校验 `client_id` + `client_secret` + `trusted=1`
- 登录审计：`UserLoginLogService`，`way=qrc_self` / `qrc_oauth`

## 11. 扩展（SPI）

- **`IntentHandler`**：新增 Intent 时注册 Spring Bean
- **`ConsentProvider`**：按 client/intent 自定义 scope 展示文案；未注册时 `ConsentSupport` 按 `scope` 空格拆分

## 12. 故障排查

| 现象 | 检查 |
|------|------|
| 票据不存在 | TTL 过期；Redis 是否开启（生产建议开启） |
| 客户端未启用扫码 | `qrc_client_grant.enabled` |
| redirect 不匹配 | `oauth_client_details.redirectUri` 精确匹配 |
| exchange 失败 | `exchange` 已消费或过期 |
| authorize 扫码变成本站登录 | authorize 建票失败时会降级账号 Tab，需完成 `/oauth2/login` 后回跳 authorize |
