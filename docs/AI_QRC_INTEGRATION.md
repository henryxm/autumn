# QRC 第三方集成标准

> **OAuth 授权码 / token / userInfo 完整对接**（含用户信息字段、账号绑定、示例代码）见 **`docs/AI_OAUTH_INTEGRATION.md`**。本文侧重 QRC 扫码与 Open API 分支。

## 1. Autumn 系平台

1. 在 AS 注册 OAuth Client（`oauth_client_details`）
2. 配置 `qrc_client_grant`（delivery、Webhook、deep link）
3. **同源 Web**：Intent `SELF_WEB_LOGIN` 或 OAuth B1
4. **跨应用**：Intent `OAUTH_DEVICE` + Open API 轮询

## 2. 非 Autumn 平台

无需部署 Autumn 代码，仅注册 OAuth Client。

### 模式 A：浏览器 Redirect（B1）

1. 浏览器打开：`GET /oauth2/authorize?response_type=code&client_id=...&redirect_uri=...&state=...`
2. 展示统一 `login.html`（扫码 Tab 或账号 Tab）；APP 扫 QR 确认（或密码登录 fallback）
3. 回调：`redirect_uri?code=...&state=...`
4. 服务端：`POST /oauth2/token` 换 `access_token`
5. 获取用户信息：`GET /oauth2/userInfo`（详见 **`docs/AI_OAUTH_INTEGRATION.md` §5**）

### 模式 B：第三方自建 QR（B3）

1. `POST /qrc/open/v1/ticket/create`（client 凭证）
2. 展示返回 `qrUrl`
3. 用户 APP 扫码确认
4. 按 `delivery` 取结果：
   - **POLL_CODE**：轮询 `/qrc/open/v1/ticket/status` 得 `code` → `/oauth2/token`
   - **POLL_TOKEN**：轮询得 `accessToken` → `/oauth2/userInfo`
   - **WEBHOOK**：接收 `qrc.authorized` 事件
   - **DEEP_LINK**：APP 打开 `{scheme}://oauth/callback?code=...`

## 3. 对接检查清单

- [ ] `client_id` / `client_secret` 已登记且 `trusted=1`
- [ ] `redirect_uri` 与注册值**完全一致**
- [ ] `qrc_client_grant.enabled=1`
- [ ] `delivery` 与集成方式一致
- [ ] 使用 `state` 防 CSRF
- [ ] `code` 5 分钟内交换且一次性
- [ ] Webhook 校验 `X-Qrc-Signature`

## 4. delivery 选型

| 场景 | 推荐 |
|------|------|
| 第三方 Web + 服务端 | POLL_CODE |
| 高信任内部服务 | POLL_TOKEN |
| 纯服务端 | WEBHOOK |
| 第三方 Native App | DEEP_LINK |

## 5. 禁止事项

- QR 中携带 `client_secret`、长期 token、用户 uuid
- 自造平行 Token 体系（须走 `/oauth2/token`）
- 跳过 APP confirm 步骤
