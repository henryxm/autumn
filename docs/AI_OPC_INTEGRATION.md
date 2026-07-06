# OPC 开放接入对接手册

> **适用对象**：将本系统作为**第三方应用**，接入 Autumn 开放平台（`opl`）的团队  
> **模块**：`cn.org.autumn.modules.opc`  
> **与 client 模块关系**：`opc` 面向 `appId/openId/unionId` 体系；`client` 面向传统 OAuth `client_id`，二者并行

---

## 1. 架构角色

```
本系统 (opc)  ──HTTP──▶  开放平台 (opl)
   │                         │
   ├─ ConnectAppEntity       ├─ OpenAppEntity (appId)
   ├─ ConnectBindEntity      └─ OpenIdentityEntity (openId/unionId)
   └─ 本地 sys_user 绑定
```

`opc` **不直接依赖** `opl` Java 包，仅通过 HTTP 调用 `/opl/api/v1/*` 与 `/opl/oauth2/*`。

---

## 2. 接入步骤

### 2.1 在 opl 平台申请 appId

**同实例（localhost）**：

1. 使用平台用户令牌调用 `POST /opl/api/v1/account/open`
2. 调用 `POST /opl/api/v1/app/register`，`redirectUri` 设为：
   ```
   {ORIGIN}/opc/oauth2/callback?appId={将获得的appId}
   ```
3. 记录返回的 `appId` 与 `appSecret`

**跨实例**：将 `{ORIGIN}` 换为远程 opl 平台根地址，其余相同。

### 2.2 在本系统配置 ConnectApp

**方式 A：Open API 申请（远程平台）**

```http
POST {ORIGIN}/opc/api/v1/app/apply
Authorization: Bearer {user_token}
Content-Type: application/json

{
  "data": {
    "platformBaseUrl": "https://auth.example.com",
    "name": "本系统",
    "redirectUri": "https://my.example.com/opc/oauth2/callback?appId=...",
    "scope": "basic",
    "accessToken": "opl平台用户令牌"
  }
}
```

**方式 B：手动保存凭证**

```http
POST {ORIGIN}/opc/api/v1/app/save
Authorization: Bearer {user_token}

{
  "data": {
    "appId": "ax...",
    "appSecret": "...",
    "platformBaseUrl": "https://auth.example.com",
    "redirectUri": "https://my.example.com/opc/oauth2/callback?appId=ax...",
    "name": "本系统"
  }
}
```

### 2.3 接入登录按钮

前端跳转：

```
GET {ORIGIN}/opc/oauth2/authorize?appId={appId}&state={random}
```

流程：

```
/opc/oauth2/authorize → opl 授权页 → 用户确认
→ /opc/oauth2/callback?code=...&appId=...
→ 换 token → userInfo → ConnectBindService 绑定本地用户 → Shiro 登录
```

---

## 3. 本地用户绑定策略

`ConnectBindService.resolveAndBind` 按以下顺序：

1. `(connectApp, openId)` 已有绑定 → 登录对应本地用户
2. `(connectApp, unionId)` 已有绑定 → 更新 openId 并登录
3. 均无绑定且 `OpcConstants.CONFIG_AUTO_REGISTER` 为 true（默认）→ 自动注册本地账号并绑定
4. 均无绑定且该配置为 false → 报错，需管理员手工绑定

配置项 **`OpcConstants.CONFIG_AUTO_REGISTER`**（`sys_config` 键名 `OPC_AUTO_REGISTER`）：`true` / `false`。

---

## 4. 管理 API 速查

| 端点 | 说明 |
|------|------|
| `POST /opc/api/v1/app/apply` | 向远程 opl 申请 appId 并落库 |
| `POST /opc/api/v1/app/save` | 手动保存/更新接入配置 |
| `POST /opc/api/v1/app/list` | 列出当前用户的接入 App |
| `POST /opc/api/v1/login/url` | 获取登录入口 URL |

---

## 5. 与 LOGIN_AUTHENTICATION 的关系

- 现有 `oauth2:{clientId}` / `shell:{host}` 登录模式**不受影响**
- `opc` 登录为**独立入口** `/opc/oauth2/authorize`，适用于需要 openId/unionId 的业务场景
- 可在业务页提供「开放平台登录」按钮，与传统 OAuth 登录并存

---

## 6. 同实例自连示例

假设根地址 `http://localhost:8080`：

1. opl 注册 App，`redirectUri` = `http://localhost:8080/opc/oauth2/callback?appId=ax...`
2. opc 保存同一 `appId` / `appSecret`，`platformBaseUrl` = `http://localhost:8080`
3. 浏览器访问 `http://localhost:8080/opc/oauth2/authorize?appId=ax...`
4. 授权成功后本地 Session 建立，`opc_connect_bind` 写入 openId/unionId

---

## 8. 后台管理

菜单 **开放接入 → 统一管理**（`opcmanage.html`）为独立全屏管理页，面向**系统管理员**，提供：

- 接入应用（ConnectApp）全量列表、手动保存、远程申请 appId、启用/禁用、复制登录 URL
- 用户绑定（ConnectBind）分页查询、手工绑定、解除绑定
- 运行配置 `OpcConstants.CONFIG_AUTO_REGISTER` 开关

管理 API 根路径 **`/opc/admin/*`**，鉴权方式与 `database.html` / `oplmanage.html` 一致（登录 + 系统管理员），不使用 Shiro 菜单权限串。

`appSecret` 在库内经 `@FieldEncrypt` 加密存储，API 与管理页均不回显明文。

gen CRUD 页：`modules/opc/connectapp`、`modules/opc/connectbind`（代码生成后可用）。

文档页：`{ORIGIN}/modules/docs/opc-integration`

---

## 9. 相关文档

- 开放平台提供方：[`AI_OPL_INTEGRATION.md`](AI_OPL_INTEGRATION.md)
- 传统 OAuth：`AI_OAUTH_INTEGRATION.md`
