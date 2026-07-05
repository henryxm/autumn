# OPL 开放平台第三方对接手册

> **适用对象**：接入 Autumn 开放平台（`opl` 模块）的第三方开发者  
> **基础路径**：`{ORIGIN}/opl`  
> **与 OAuth 模块关系**：`opl` 使用独立的 `appId` 体系，与 `/oauth2/*` 的 `client_id` **并行、互不替代**

---

## 1. 角色说明

| 角色 | 说明 |
|------|------|
| **开发者** | 在平台开通主体账号，注册一个或多个 App，获得 `appId` / `appSecret` |
| **终端用户** | 在授权页登录并确认后，第三方获得该用户在本 App 下的 `openId` 与在开发者主体下的 `unionId` |

---

## 2. 快速开始

```
1. POST /opl/api/v1/account/open        开通开发者主体（需用户令牌 @Authenticated）
2. POST /opl/api/v1/app/register        注册 App，获得 appId + appSecret（secret 仅此次明文返回）
3. 浏览器重定向 GET /opl/oauth2/authorize?app_id=...&redirect_uri=...&state=...
4. 用户确认授权 → redirect_uri?code=...&state=...
5. POST /opl/oauth2/token               用 code 换 access_token（服务端，携带 app_secret）
6. GET /opl/oauth2/userInfo             Bearer token → openId / unionId / nickname / icon
```

---

## 3. 开发者 API

所有 Open API 路径前缀 **`POST /opl/api/v1/*`**，需 **`@Authenticated`** 用户令牌（与机器人 Open API 相同鉴权方式）。

### 3.1 开通开发者主体

```http
POST {ORIGIN}/opl/api/v1/account/open
Authorization: Bearer {user_token}
Content-Type: application/json

{}
```

### 3.2 注册 App

```http
POST {ORIGIN}/opl/api/v1/app/register
Authorization: Bearer {user_token}
Content-Type: application/json

{
  "data": {
    "name": "我的应用",
    "appType": "Web",
    "redirectUri": "https://app.example.com/opc/oauth2/callback?appId=ax...",
    "scope": "basic"
  }
}
```

**`appType` 可选值**（默认 `Web`）：

| 值 | 说明 |
|----|------|
| `Web` | 网页应用 |
| `Android` | 安卓应用 |
| `Ios` | 苹果 iOS 应用 |
| `HarmonyOS` | 鸿蒙应用 |
| `OfficialAccount` | 公众号 |
| `ServiceAccount` | 服务号 |
| `MiniProgram` | 小程序 |

**响应 `data`**：

```json
{
  "appId": "ax0123456789abcdef",
  "appSecret": "仅此次返回的明文密钥",
  "name": "我的应用",
  "appType": "Web",
  "redirectUri": "https://...",
  "scope": "basic"
}
```

### 3.3 其他管理接口

| 端点 | 说明 |
|------|------|
| `POST /opl/api/v1/app/list` | 列出当前开发者主体下 App（含 `appType`） |
| `POST /opl/api/v1/app/resetSecret` | 重置 appSecret，`data.appId` 必填 |
| `POST /opl/api/v1/app/update` | 更新名称/类型/回调域/scope；`data.appType` 可选 |

---

## 4. OAuth2 授权码流程

### 4.1 引导用户授权

```http
GET {ORIGIN}/opl/oauth2/authorize?app_id={appId}&redirect_uri={urlencode(uri)}&response_type=code&scope=basic&state={random}
```

| 参数 | 必填 | 说明 |
|------|------|------|
| `app_id` | 是 | 注册的 appId |
| `redirect_uri` | 是 | 须与注册值**字符级一致** |
| `response_type` | 是 | 固定 `code` |
| `scope` | 否 | 默认 `basic` |
| `state` | 推荐 | CSRF 防护 |

### 4.2 用户确认授权

已登录用户访问授权确认页，勾选协议后 `POST /opl/oauth2/authorize/approve`。

### 4.3 换 Token

```http
POST {ORIGIN}/opl/oauth2/token
Content-Type: application/x-www-form-urlencoded

app_id={appId}&app_secret={secret}&grant_type=authorization_code&code={code}&redirect_uri={uri}
```

支持 `grant_type=refresh_token` + `refresh_token=...` 续期。

### 4.4 获取用户信息

```http
GET {ORIGIN}/opl/oauth2/userInfo
Authorization: Bearer {access_token}
```

**响应**（不暴露内部 `uuid`）：

```json
{
  "openId": "o_abc...",
  "unionId": "u_def...",
  "nickname": "张三",
  "icon": "https://..."
}
```

---

## 5. openId 与 unionId 语义

| 标识 | 作用域 | 说明 |
|------|--------|------|
| **openId** | 单个 App（`appId`） | 存储于 `opl_open_identity`，`(appId, user)` 唯一 |
| **unionId** | 开发者账号（`account`） | 存储于 `opl_open_union`，`(account, user)` 唯一；该账号下所有 App 共享 |

**表职责**：

| 表 | 字段 | 唯一约束 |
|----|------|----------|
| `opl_open_union` | `account`, `user`, `unionId` | 账号下用户唯一 |
| `opl_open_identity` | `appId`, `user`, `openId` | App 内用户唯一 |

**要点**：

- `OpenUnionService` 签发 unionId；`OpenIdentityService` 仅签发 openId
- 授权回调 / userInfo 同时返回两者，由 Service 编排组装

---

## 6. 安全规范

- `appSecret` **仅存服务端**，禁止写入前端或移动客户端
- 授权请求必须校验 `state`
- `code` 默认 **5 分钟**有效，仅可使用一次
- `redirect_uri` 首版为**单条精确匹配**

---

## 7. curl 示例

```bash
# 换 token
curl -X POST '{ORIGIN}/opl/oauth2/token' \
  -d 'app_id=ax0123456789abcdef' \
  -d 'app_secret=YOUR_SECRET' \
  -d 'grant_type=authorization_code' \
  -d 'code=AUTH_CODE' \
  -d 'redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback'

# userInfo
curl '{ORIGIN}/opl/oauth2/userInfo' \
  -H 'Authorization: Bearer ACCESS_TOKEN'
```

---

## 8. 后台管理

菜单 **开放平台 → 统一管理**（`oplmanage.html`）为独立全屏管理页（样式同 `database.html`），面向**系统管理员**，提供应用、主体、用户、unionId、授权码、令牌等一站式运维。

管理 API 根路径 **`/opl/admin/*`**，鉴权方式为登录 + 系统管理员校验（与 `DatabaseAdminController` 一致），**不使用** Shiro `@RequiresPermissions` 菜单权限串。

**应用内扩展**（Service/Extension、事件订阅）见 **`docs/AI_OPL_SPI.md`**。

文档页：`{ORIGIN}/modules/docs/opl-integration`
