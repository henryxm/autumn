# OAuth2 第三方对接手册

> **适用对象**：接入 Autumn 授权服务器（AS）的第三方 Web / 服务端 / 移动应用团队  
> **服务端模块**：`cn.org.autumn.modules.oauth`（AS）、`cn.org.autumn.modules.client`（RP 示例）  
> **基础路径**：`{ORIGIN}/oauth2`（AS 端点）；用户信息 `{ORIGIN}/oauth2/userInfo`  
> **框架版本**：Autumn 2.0.0 / 3.0.0（OAuth 报文格式一致）

扫码登录、Open API 建票等扩展流程见 **`docs/AI_QRC_INTEGRATION.md`**；AS 端流程说明见站内 **`/modules/docs/auth-flow`**。  
**双轨体系总览与选型**（OAuth2 vs OPL/OPC、自连/第三方拓扑）见 **`docs/AI_AUTH_LOGIN_MODES.md`**。

---

## 目录

1. [快速开始](#1-快速开始)
2. [角色与架构](#2-角色与架构)
3. [接入前准备](#3-接入前准备)
4. [标准授权码模式（Web）](#4-标准授权码模式web)
5. [获取用户信息](#5-获取用户信息)
6. [授权范围与字段对照](#6-授权范围与字段对照)
7. [Refresh Token 续期](#7-refresh-token-续期)
8. [第三方账号绑定建议](#8-第三方账号绑定建议)
9. [错误处理](#9-错误处理)
10. [安全规范](#10-安全规范)
11. [代码示例](#11-代码示例)
12. [与扫码登录的关系](#12-与扫码登录的关系)
13. [接口速查表](#13-接口速查表)
14. [FAQ](#14-faq)

---

## 1. 快速开始

第三方 Web 应用最小接入（**授权码模式**）：

```
1. 在 AS 注册 OAuth Client（client_id / client_secret / redirect_uri）
2. 浏览器重定向用户到 GET /oauth2/authorize
3. 用户在 AS 登录并确认授权
4. AS 回调 redirect_uri?code=...&state=...
5. 第三方服务端 POST /oauth2/token 用 code 换 access_token
6. 第三方服务端 GET /oauth2/userInfo 获取 uuid / nickname / icon / username
7. 在第三方系统建立或绑定本地账号
```

**关键结论**：

- 授权页「查看基本账号信息」「获取用户唯一标识」对应 **`/oauth2/userInfo` 返回的 `UserProfile` 字段**，不是两个独立接口。
- **`uuid` 是账号关联主键**；`nickname`、`icon`、`username` 用于展示。
- `client_secret` 与 token 交换**必须在服务端**完成，禁止写入前端或移动客户端。

---

## 2. 角色与架构

| 角色 | 说明 |
|------|------|
| **AS（Authorization Server）** | Autumn 实例，提供 `/oauth2/authorize`、`/oauth2/token`、`/oauth2/userInfo` |
| **RP（Relying Party）** | 第三方应用，引导用户授权并消费 token / 用户信息 |
| **Resource Owner** | 终端用户 |

```
┌──────────┐    ① 重定向授权     ┌─────────────────┐
│ 用户浏览器 │ ────────────────→ │ Autumn AS        │
└──────────┘                    │ /oauth2/authorize│
     ↑                          └────────┬─────────┘
     │ ② 登录 + 确认授权                   │
     │ ③ redirect_uri?code&state          │
     └────────────────────────────────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │ 第三方服务端（RP）     │
         │ ④ POST /oauth2/token  │
         │ ⑤ GET  /oauth2/userInfo│
         └──────────────────────┘
```

---

## 3. 接入前准备

### 3.1 注册 OAuth Client

在 AS 管理端登记客户端，常用入口：

| 入口 | 路径 | 说明 |
|------|------|------|
| 授权管理 | `oauthasmanage.html` | AS 侧客户端、令牌（推荐） |
| 接入管理 | `oauthrpmanage.html` | RP 侧接入、绑定（推荐） |
| 客户端详情 CRUD | `/modules/oauth/clientdetails` | 单独维护 `oauth_client_details` |

**必填/关键字段**（表 `oauth_client_details`）：

| 字段 | 说明 |
|------|------|
| `client_id` | 客户端标识，授权 URL 参数 |
| `client_secret` | 客户端密钥，换 token 时使用 |
| `redirect_uri` | 授权回调地址，**必须与请求完全一致**（含协议、端口、路径） |
| `trusted` | 必须为 `1`，否则 AS 拒绝授权 |
| `archived` | 必须为 `0` |
| `grant_types` | 第三方 Web 登录至少包含 `authorization_code`；可填 `all` |
| `scope` | 默认 `basic` |

### 3.2 对接检查清单

- [ ] `client_id` / `client_secret` 已登记且 `trusted=1`
- [ ] `redirect_uri` 与授权请求、换 token 请求**字符级一致**
- [ ] 授权 URL 携带随机 `state`，回调后校验
- [ ] `code` 仅使用一次，收到后尽快换 token（默认 **5 分钟**有效）
- [ ] token 与 secret 仅存服务端
- [ ] 第三方用户表以 **`uuid`** 作为 Autumn 关联键

### 3.3 环境地址

设 AS 根地址为 `{ORIGIN}`（如 `https://auth.example.com`），则：

| 用途 | 地址 |
|------|------|
| 授权页 | `{ORIGIN}/oauth2/authorize` |
| 换 Token | `{ORIGIN}/oauth2/token` |
| 用户信息 | `{ORIGIN}/oauth2/userInfo` |
| RP 授权登录入口（联调） | `{ORIGIN}/oauth2/login?client_id=...` |
| RP 回调成功页 | `{ORIGIN}/oauth2/success` |

登录入口页展示的授权范围说明来自 **`AuthScopeCatalog`**（`scopeLabels`），与授权确认页、管理页 picker 文案一致；详见 **`docs/AI_AUTH_SCOPE.md`**。

### 3.4 同实例联调步骤

1. 在 `oauthasmanage.html` 创建 AS Client，`redirect_uri` 设为 `{ORIGIN}/client/oauth2/callback`；在 `oauthrpmanage.html` 用同 `clientId` 一键接入
2. 打开 `{ORIGIN}/oauth2/login?client_id=...`（或管理页「打开 OAuth 登录页」）
3. 点击「使用 OAuth 授权登录」→ AS 分栏授权页登录并确认
4. 回调 `/client/oauth2/callback` 换票建会话 → 默认跳转 `/oauth2/success`

---

## 4. 标准授权码模式（Web）

### 4.1 第一步：引导用户授权

将用户浏览器重定向到：

```http
GET {ORIGIN}/oauth2/authorize?response_type=code&client_id={client_id}&redirect_uri={urlencode(redirect_uri)}&scope=basic&state={random_state}
```

**Query 参数**：

| 参数 | 必填 | 说明 |
|------|------|------|
| `response_type` | 是 | 固定 `code` |
| `client_id` | 是 | 注册的客户端 ID |
| `redirect_uri` | 是 | 回调地址，须与注册值一致 |
| `scope` | 否 | 建议 `basic`；缺省时 AS 仍可按默认 scope 处理 |
| `state` | 强烈推荐 | 防 CSRF 的随机串，回调时原样带回 |

**用户侧体验**：

- 未登录：AS 展示统一登录页（账号 / 扫码 / 手机），登录后回到同一授权页
- 已登录：左侧显示当前账号，右侧展示授权信息与「确认授权」按钮
- 用户勾选协议并确认后，AS 才签发 `code`

### 4.2 第二步：接收授权码

用户确认后，AS 302 重定向到：

```http
{redirect_uri}?code={authorization_code}&state={state}
```

**第三方服务端必须**：

1. 校验 `state` 与会话中保存的值一致
2. 取出 `code`，交给后端换 token（不要在前端暴露 `client_secret`）

**用户取消授权**时：

```http
{redirect_uri}?error=access_denied&state={state}
```

第三方应友好提示「用户已取消授权」，引导重新发起。

### 4.3 第三步：用 code 换 access_token

```http
POST {ORIGIN}/oauth2/token
Content-Type: application/x-www-form-urlencoded
```

**Body 参数**：

| 参数 | 必填 | 说明 |
|------|------|------|
| `grant_type` | 是 | 固定 `authorization_code` |
| `client_id` | 是 | 客户端 ID |
| `client_secret` | 是 | 客户端密钥 |
| `code` | 是 | 上一步获得的授权码 |
| `redirect_uri` | 是 | 须与授权请求、注册值一致 |
| `scope` | 否 | 如 `basic` |
| `state` | 否 | 可选 |

**成功响应**（HTTP 200，`Content-Type: application/json`）：

```json
{
  "access_token": "8aefd30777ab305db60264b57a558d8d",
  "refresh_token": "e0efc004d325e0322cbb4fce5f83c43b",
  "token_type": "bearer",
  "expires_in": "86340",
  "scope": "basic"
}
```

| 字段 | 说明 |
|------|------|
| `access_token` | 访问令牌，调用 userInfo 使用 |
| `refresh_token` | 刷新令牌，access_token 过期后续期 |
| `token_type` | 固定 `bearer` |
| `expires_in` | 剩余有效秒数（约 24 小时，响应会略小于 86400） |
| `scope` | 当前实现固定返回 `basic` |

**Token 默认有效期**（`ClientDetailsService`）：

| 类型 | 默认 TTL |
|------|----------|
| `authorization_code` | 5 分钟，**一次性** |
| `access_token` | 24 小时 |
| `refresh_token` | 7 天 |

> Redis / 内存存储场景下，AS 或 Redis 重启可能导致 token 立即失效，第三方应实现「token 无效 → 重新授权」兜底。

---

## 5. 获取用户信息

### 5.1 接口说明

```http
GET {ORIGIN}/oauth2/userInfo
```

也支持 `POST`。有效 `access_token` 时返回授权用户的 **`UserProfile` JSON**。

### 5.2 传参方式（重要）

**推荐（Parallel Profile）**：`Authorization: Bearer {access_token}` 或 query `access_token={裸token}`。

**历史兼容（Autumn 内部 `ClientOauth2Controller` 仍使用）**：`access_token` 参数传 **JSON 包裹**整段 token 响应：

```http
GET {ORIGIN}/oauth2/userInfo?access_token={"access_token":"YOUR_ACCESS_TOKEN","refresh_token":"YOUR_REFRESH_TOKEN","expires_in":86400}
```

> 整段 JSON 须 **URL 编码**。仅传最小 JSON `{"access_token":"..."}` 亦可。

新对接第三方 **优先使用 Bearer**；已有对接继续 JSON 包裹方式 **无需改动**。

### 5.3 成功响应

HTTP 200，JSON 示例：

```json
{
  "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "username": "admin",
  "nickname": "管理员",
  "icon": "https://cdn.example.com/avatar/admin.png"
}
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `uuid` | string | **用户业务唯一标识**，第三方账号绑定的主键 |
| `username` | string | 登录账号名 |
| `nickname` | string | 昵称，用于展示 |
| `icon` | string | 头像 URL |

空字段不输出（`@JsonSerialize(NON_EMPTY)`）。

### 5.4 失败响应

| 情况 | HTTP | 说明 |
|------|------|------|
| token 缺失或格式错误 | 400 | 无法解析 `access_token` |
| token 无效或过期 | 401 | 响应头含 `WWW-Authenticate` |

token 过期后：使用 [Refresh Token 续期](#7-refresh-token-续期)，或重新走授权流程。

---

## 6. 授权范围与字段对照

完整 scope 目录见 **`docs/AI_AUTH_SCOPE.md`**。

### 6.1 默认 `basic`（兼容）

| 轨道 | 展开 scope | userInfo 字段 |
|------|-----------|---------------|
| OAuth | `identity` + `profile` | `uuid`、`nickname`、`icon`、`username` |
| OPL | `openid` + `unionid` + `profile` | `openId`、`unionId`、`nickname`、`icon` |

### 6.2 细粒度 scope（OAuth 示例）

| scope 请求 | userInfo 返回 |
|------------|---------------|
| `identity` | 仅 `uuid` |
| `profile` | `nickname`、`icon`、`username`（无 uuid） |
| `phone` | `mobile`（需客户端登记含 `phone`） |
| `verified` | `verified`（0/1，不含身份证） |

### 6.3 实现说明

- 换 token 响应中 `scope` 为**本次实际授权**值
- `/oauth2/userInfo` 按 token 绑定 scope **裁剪字段**
- 客户端登记 scope 须包含请求的 scope（或使用 `all` 通配）
- 非法 scope 在 authorize 阶段返回 `invalid_scope`

---

## 7. Refresh Token 续期

`access_token` 过期后，可用 `refresh_token` 换取新 token，无需用户再次授权（refresh_token 仍有效时）。

```http
POST {ORIGIN}/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token
&client_id={client_id}
&client_secret={client_secret}
&refresh_token={refresh_token}
```

成功响应格式与 [4.3](#43-第三步用-code-换-access_token) 相同。第三方应：

1. 用新 `access_token` 更新本地存储
2. 若响应含新 `refresh_token`，一并更新
3. `refresh_token` 也失效时，引导用户重新授权

---

## 8. 第三方账号绑定建议

### 8.1 Autumn 内置 RP（`client` 模块）

Autumn 作为 OAuth **接入方（RP）** 时，使用表 **`client_web_oauth_bind`** 维护关联：

| 字段 | 含义 |
|------|------|
| `authentication` | `WebAuthenticationEntity.uuid`，按 client/host 隔离 |
| `upper` | AS `/oauth2/userInfo` 返回的 `uuid` |
| `user` | 本地 `sys_user.uuid`（新用户由 `UuidNamespaceService.allocate()` 分配） |

- **不要** 再假设 `local.uuid == upstream.uuid`（仅懒迁移兼容历史数据）
- 同实例 AS+RP 且 userInfo.uuid 在本地 `sys_user` 已存在时，以 **token 内 upstream 为权威**（不依赖 RP Session）；重复授权 **幂等 NO-OP**；`establishSession` 始终调用（已登录同用户时内部跳过）
- 冲突时回调错误页（含绑定管理入口）+ `POST /client/oauth2/bind/unbind` 自助解绑
- **`userInfoDelivery`**（`WebAuthenticationEntity`）：`legacy` = Autumn 历史 query JSON 包裹；`bearer` = 标准 Bearer；空则同实例自动 `legacy`、跨实例自动 `bearer`
- 自动注册用户名 `oauth_{upper前缀}`，冲突时追加 `_1`…`_4` 重试

### 8.2 外部第三方系统推荐数据模型

```text
third_party_user
├── id              （第三方本地主键）
├── autumn_uuid     （Autumn 本地 uuid，来自绑定解析后的 Session 用户；或外部系统自管映射表）
├── upstream_uuid   （Autumn AS 返回的 uuid，关联键之一）
├── username        （可选，来自 UserProfile.username）
├── nickname        （展示，来自 UserProfile.nickname）
├── avatar_url      （展示，来自 UserProfile.icon）
├── last_login_at
└── created_at
```

### 8.3 绑定逻辑（外部 RP 伪代码）

```text
profile = GET /oauth2/userInfo   // profile.uuid = 上游 AS 用户 uuid

binding = db.findBind(oauthClientConfig, profile.uuid)
if binding != null:
    localUser = db.findByUuid(binding.local_uuid)
else if session.loggedIn:
    if session.user already bound other upstream: error
    db.createBind(oauthClientConfig, profile.uuid, session.user)
    localUser = session.user
else:
    localUser = db.findByAutumnUuid(profile.uuid)   // 可选：历史同 uuid 迁移
    if localUser == null:
        localUser = db.create({ uuid: allocateNew(), nickname: profile.nickname, ... })
    db.createBind(oauthClientConfig, profile.uuid, localUser.uuid)

session.login(localUser)
sync nickname / icon from profile
```

### 8.4 注意事项

- **Autumn RP**：以 **`client_web_oauth_bind`** + 本地 `user` 为关联键；**外部系统**：至少持久化 `(client, upper) → localUser`
- **永远使用业务 `uuid` 关联**，不要使用 Autumn 内部 Long 型 `id`
- `username` 可能被管理员修改，不宜作为唯一关联键
- 头像 URL 可能为空，前端需占位图
- 每次登录可刷新 nickname / icon，保持展示一致

### 8.4 Autumn 系 RP 内置联邦（推荐）

Autumn 站点作 **RP** 时无需自行实现 §8.2：`WebOauthLoginService.completeRemoteOAuthCallback` → **`WebOauthBindService.resolveAndBind`**（`client_web_oauth_bind`，`upper`=上游 uuid、`user`=本地 uuid）。跨实例未登录且无 bind 时进入 **`/client/oauth2/bind/choice`**。  
B1 回调 `/client/oauth2/callback` 与 D 模式联邦入站 `qrc.authorized`（自动 `completeRemoteOAuthCallback`）均走同一套 `finishOAuthLogin` 逻辑。  
完整配置见 **`docs/AI_AUTH_SITE_ROLES.md`**。

---

## 9. 错误处理

### 9.1 授权阶段

| 场景 | 回调 / 表现 | 第三方处理 |
|------|-------------|-----------|
| 用户取消 | `?error=access_denied&state=...` | 提示已取消，可重新授权 |
| `redirect_uri` 不匹配 | AS 返回 JSON 错误 | 检查注册值与请求是否完全一致 |
| 客户端未信任 / 已归档 | AS 返回 JSON 错误 | 联系 AS 管理员 |
| `state` 不一致 | — | 拒绝处理，防 CSRF |

### 9.2 换 Token 阶段

AS 返回 JSON 错误体（HTTP 400/401），常见 `error`：

| error | 含义 |
|-------|------|
| `invalid_client` | client_id 无效或未信任 |
| `unauthorized_client` | client_secret 错误 |
| `invalid_grant` | code 无效、已使用或过期；refresh_token 无效 |
| `invalid_request` | 参数缺失 |

### 9.3 获取用户信息阶段

| 场景 | 处理 |
|------|------|
| 401 INVALID_TOKEN | 尝试 refresh；失败则重新授权 |
| 400 | 检查 access_token 传参格式（见 [5.2](#52-传参方式重要)） |
| userInfo 无 `uuid` | RP 回调普通错误页，提示重新授权 |
| 绑定冲突 | 冲突专用错误页：退出登录 / 绑定管理 / 解绑 API |

### 9.4 RP 绑定冲突（Autumn `client` 模块）

| ConflictType | 表现 | 用户操作 |
|--------------|------|----------|
| `UPSTREAM_BOUND_TO_OTHER` | 上游账号已绑其他本地用户 | 退出后换账号；或后台/绑定管理页解绑 |
| `LOCAL_ALREADY_BOUND` | 当前 Session 已绑其他上游 | `POST /client/oauth2/bind/unbind` 后重试 |
| `UPSTREAM_UUID_INVALID` | 普通授权失败页（非冲突页） | 重新发起授权 |

---

## 10. 安全规范

### 10.1 必须遵守

1. **`client_secret` 仅存放于服务端**，不得出现在前端、App、日志、Git
2. **校验 `state`**，防止 CSRF
3. **`redirect_uri` 精确匹配**，不使用通配回调
4. **HTTPS** 传输（生产环境）
5. **`code` 一次性**：换 token 成功后 AS 会删除 authCode
6. **最小存储**：服务端按需存 access_token / refresh_token，勿下发到浏览器

### 10.2 禁止事项

- 在前端 JS 中直接用 `client_secret` 换 token
- 自造平行 Token 体系绕过 `/oauth2/token`
- 将 `uuid` 暴露为可猜测的序列号对外 API 参数（应配合会话鉴权）
- 跳过用户「确认授权」步骤（AS 已强制勾选 + 手动确认）

---

## 11. 代码示例

### 11.1 cURL：完整流程

```bash
# 假设已拿到 code=AUTH_CODE_FROM_CALLBACK

# 1. 换 token
curl -s -X POST 'https://auth.example.com/oauth2/token' \
  -d 'grant_type=authorization_code' \
  -d 'client_id=YOUR_CLIENT_ID' \
  -d 'client_secret=YOUR_CLIENT_SECRET' \
  -d 'code=AUTH_CODE_FROM_CALLBACK' \
  -d 'redirect_uri=https://app.example.com/oauth/callback'

# 响应示例：
# {"access_token":"...","refresh_token":"...","token_type":"bearer","expires_in":"86340","scope":"basic"}

# 2. 拉用户信息（注意 URL 编码）
TOKEN_JSON='{"access_token":"YOUR_ACCESS_TOKEN"}'
ENCODED=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$TOKEN_JSON")

curl -s "https://auth.example.com/oauth2/userInfo?access_token=${ENCODED}"
```

### 11.2 Java（Spring RestTemplate 思路）

```java
// 换 token
MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
form.add("grant_type", "authorization_code");
form.add("client_id", clientId);
form.add("client_secret", clientSecret);
form.add("code", authCode);
form.add("redirect_uri", redirectUri);

String tokenBody = restTemplate.postForObject(tokenUrl, form, String.class);
JSONObject tokenJson = JSON.parseObject(tokenBody);
String accessToken = tokenJson.getString("access_token");

// userInfo：Autumn 要求 access_token 参数为 JSON 字符串
String tokenParam = "{\"access_token\":\"" + accessToken + "\"}";
String userInfoUrl = userInfoUri + "?access_token=" + URLEncoder.encode(tokenParam, StandardCharsets.UTF_8);
String profileBody = restTemplate.getForObject(userInfoUrl, String.class);
UserProfile profile = JSON.parseObject(profileBody, UserProfile.class);

// profile.getUuid() → 绑定第三方账号
```

### 11.3 Node.js（axios 思路）

```javascript
const tokenRes = await axios.post(`${ORIGIN}/oauth2/token`, new URLSearchParams({
  grant_type: 'authorization_code',
  client_id: CLIENT_ID,
  client_secret: CLIENT_SECRET,
  code,
  redirect_uri: REDIRECT_URI,
}));

const { access_token } = tokenRes.data;

const tokenParam = JSON.stringify({ access_token });
const profileRes = await axios.get(`${ORIGIN}/oauth2/userInfo`, {
  params: { access_token: tokenParam },
});

const { uuid, nickname, icon, username } = profileRes.data;
```

### 11.4 授权 URL 生成（JavaScript 前端）

```javascript
function buildAuthorizeUrl(origin, clientId, redirectUri, state) {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: clientId,
    redirect_uri: redirectUri,
    scope: 'basic',
    state: state,
  });
  return `${origin}/oauth2/authorize?${params.toString()}`;
}

// 使用前在 sessionStorage 保存 state，回调页比对
```

---

## 12. 与扫码登录的关系

除浏览器 Redirect 外，第三方还可通过 QRC Open API 自建二维码，最终仍落到 **code → token → userInfo** 或直接拿 token：

| 模式 | 说明 | 获取用户信息 |
|------|------|-------------|
| 浏览器 Redirect | 本文 [第 4 节](#4-标准授权码模式web) | code → token → userInfo |
| QRC `POLL_CODE` | 轮询得 `code` | 同上 |
| QRC `POLL_TOKEN` | 轮询得 `accessToken` | 直接调 userInfo |

详见 **`docs/AI_QRC_INTEGRATION.md`**、**`docs/AI_QRC_API.md`**。

---

## 13. 接口速查表

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/oauth2/authorize` | 无（用户 Session） | 引导授权，返回 HTML 或 302 |
| POST | `/oauth2/authorize/approve` | 用户 Session | 用户确认授权（表单） |
| GET | `/oauth2/authorize/logout` | 无 | 授权页切换账号 |
| POST | `/oauth2/token` | client_secret | 换 token / 刷新 token |
| GET/POST | `/oauth2/userInfo` | access_token | 获取 UserProfile |

**UserProfile 字段速查**：

| 字段 | 授权能力 |
|------|----------|
| `uuid` | 用户唯一标识 / 第三方登录关联 |
| `nickname` | 基本账号信息 |
| `icon` | 基本账号信息 |
| `username` | 基本账号信息 |

---

## 14. FAQ

### Q1：授权页两项权限分别对应哪个接口？

都是 **`GET /oauth2/userInfo`** 的返回字段；`uuid` 用于关联，`nickname`/`icon`/`username` 用于展示。

### Q2：`/oauth2/userInfo` 支持 Bearer Header 吗？

**支持**。Parallel Profile 下推荐使用 `Authorization: Bearer {access_token}` 或 query 传裸 `access_token`。  
历史对接使用的 **JSON 包裹 query** 方式仍然兼容。Autumn 内置 RP（`WebOauthLoginService`）按 `WebAuthenticationEntity.userInfoDelivery` 或同实例/跨实例自动选择 **LEGACY** 或 **Bearer**。

### Q3：`redirect_uri` 总是报不匹配？

须与 `oauth_client_details.redirect_uri` **完全一致**（`http`/`https`、端口、路径、末尾斜杠均敏感）。授权请求与换 token 请求必须使用同一值。

### Q4：`code` 换 token 报 `invalid_grant`？

常见原因：code 已用过、超过 5 分钟、`redirect_uri` 不一致、client_secret 错误。

### Q5：同一用户多次授权会换 uuid 吗？

不会。`uuid` 是用户稳定业务标识；重复授权仅签发新 token，userInfo 中 uuid 不变。

### Q6：Autumn 同源站点还要自己对接吗？

若第三方就是同一 Autumn 部署且走 `/client/oauth2/callback`，框架会自动换票、`resolveAndBind`、`establishSession`。跨系统或非 Autumn 应用需按本文自行对接。

### Q7：scope 可以传 `all` 吗？

客户端登记支持 `all`；userInfo 当前仍返回完整 `UserProfile`，未做字段级 scope 过滤。

---

## 相关文档

- **`docs/AI_QRC_INTEGRATION.md`** — 扫码 / Open API 第三方集成
- **`docs/AI_QRC_API.md`** — QRC 端点参考
- 站内 **`/modules/docs/auth-flow`** — 授权与扫码流程（含运维视角）
- 站内 **`oauthasmanage.html`** / **`oauthrpmanage.html`** — 经典 OAuth2 管理
