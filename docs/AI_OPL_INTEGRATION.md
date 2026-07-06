# OPL 开放平台第三方对接手册

> **适用对象**：接入 Autumn 开放平台（`opl` 模块）的第三方开发者  
> **HTTP 前缀**：`{ORIGIN}/open`（常量见 `OplConstants`）  
> **与 OAuth 模块关系**：`app_id` / `app_secret` 与 `/oauth2/*` 的 `client_id` **并行、互不替代**  
> **路径规则与 OPC 对照**：见 **`docs/AI_AUTH_LOGIN_MODES.md` §1.5**

---

## 1. 快速开始

```
1. POST /open/api/v1/platform/account/open     开通开发者主体（@Authenticated）
2. POST /open/api/v1/platform/app/register   注册 App，获得 appId + appSecret（secret 仅此次明文）
3. GET  /open/oauth2/authorize?app_id=...&redirect_uri=...&state=...
4. 用户登录并确认 → redirect_uri?code=...&state=...
5. POST /open/oauth2/token                   用 code 换 access_token（服务端带 app_secret）
6. GET  /open/oauth2/userInfo               Bearer → openId / unionId / nickname / icon
```

---

## 2. HTTP 路径（OPL）

| 用途 | 方法 | 路径 |
|------|------|------|
| 引导授权 | GET/HEAD | `/open/oauth2/authorize` |
| 用户确认 | POST | `/open/oauth2/authorize/approve` |
| 授权页内登录 | POST | `/open/oauth2/opl/login`（与 OPC `/login` 冲突，故加 `opl`） |
| 换 Token | POST | `/open/oauth2/token` |
| 用户信息 | GET/POST | `/open/oauth2/userInfo` |

### 2.1 开发者 Open API

根路径 **`POST /open/api/v1/platform/*`**，需 **`@Authenticated`** 用户令牌。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/open/api/v1/platform/account/open` | 开通开发者主体 |
| POST | `/open/api/v1/platform/app/register` | 注册 App（仅此返回明文 appSecret） |
| POST | `/open/api/v1/platform/opl/app/list` | 列出主体下 App（与 OPC 的 `app/list` 冲突，故加 `opl`） |
| POST | `/open/api/v1/platform/app/resetSecret` | 重置 appSecret |
| POST | `/open/api/v1/platform/app/update` | 更新名称/类型/回调/scope |

### 2.2 管理 API

根路径 **`/open/admin/opl/platform/*`**（系统管理员登录 + 校验，无 Shiro 菜单权限串）。页面：`oplmanage.html`。

---

## 3. 授权码流程

### 3.1 引导用户授权

```http
GET {ORIGIN}/open/oauth2/authorize?app_id={appId}&redirect_uri={urlencode(uri)}&response_type=code&scope=basic&state={random}
```

| 参数 | 必填 | 说明 |
|------|------|------|
| `app_id` | 是 | 注册的 appId |
| `redirect_uri` | 是 | 与注册值**字符级一致** |
| `response_type` | 是 | 固定 `code` |
| `scope` | 否 | 默认 `basic` |
| `state` | 推荐 | CSRF 防护 |

### 3.2 换 Token

```http
POST {ORIGIN}/open/oauth2/token
Content-Type: application/x-www-form-urlencoded

app_id={appId}&app_secret={secret}&grant_type=authorization_code&code={code}&redirect_uri={uri}
```

支持 `grant_type=refresh_token` 续期。

### 3.3 userInfo

```http
GET {ORIGIN}/open/oauth2/userInfo
Authorization: Bearer {access_token}
```

响应含 `openId`、`unionId`、`nickname`、`icon`（**不暴露**内部 `uuid`）。

---

## 4. openId 与 unionId

| 标识 | 作用域 | 表 |
|------|--------|-----|
| **openId** | 单个 `appId` | `opl_open_identity` |
| **unionId** | 开发者主体 `account` | `opl_open_union` |

---

## 5. 安全规范

- `appSecret` 仅存服务端
- 授权请求必须校验 `state`（第三方 RP）；本站 OPC 由 Session 校验
- `code` 默认 5 分钟有效、一次性；换 token 时 **`redirect_uri` 必填**
- `redirect_uri` 精确匹配（扩展点可放宽）
- 可选 **PKCE**：`code_challenge` + 换 token 时 `code_verifier`
- Token 端点限流（IP + appId）
- 生产回调须 HTTPS（`OPL_ALLOW_HTTP_REDIRECT` 控制 http 例外）

---

## 6. 相关文档

- 双轨总览与路径规则：**`docs/AI_AUTH_LOGIN_MODES.md`**
- 框架内扩展：**`docs/AI_OPL_SPI.md`**
- 本系统作为接入方（OPC）：**`docs/AI_OPC_INTEGRATION.md`**

文档页：`{ORIGIN}/modules/docs/opl-integration`
