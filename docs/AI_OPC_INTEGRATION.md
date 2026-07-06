# OPC 开放接入对接手册

> **适用对象**：将本系统作为**第三方应用**，接入 Autumn 开放平台（OPL）的团队  
> **模块**：`cn.org.autumn.modules.opc`  
> **HTTP 前缀**：`{ORIGIN}/open`（常量见 `OpcConstants`）  
> **双轨总览与路径规则**：**`docs/AI_AUTH_LOGIN_MODES.md` §1.5**

`opc` 不依赖 `opl` Java 包，通过 HTTP 调用远程 OPL（`OAuth2HttpClient` + `CredentialParam.OPL`）。

---

## 1. 架构

```
本系统 (opc)  ──HTTP──▶  开放平台 (opl)
   ├─ opc_connect_app        ├─ opl_open_app
   ├─ opc_connect_bind       └─ openId / unionId
   └─ 本地 sys_user
```

---

## 2. HTTP 路径（OPC）

| 用途 | 方法 | 路径 |
|------|------|------|
| 接入登录入口页 | GET | `/open/oauth2/login?appId=...` |
| 发起授权（跳转 OPL） | GET | `/open/oauth2/opc/authorize?appId=...`（与 OPL `/authorize` 冲突，故加 `opc`） |
| OAuth 回调 | GET | `/open/oauth2/callback?appId=...&code=...` |
| 成功页（无 callback 时默认） | GET | `/open/oauth2/success` |

### 2.1 用户 Open API

根路径 **`POST /open/api/v1/platform/*`**，需 **`@Authenticated`**。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/open/api/v1/platform/app/apply` | 向远程 OPL 申请 appId 并落库 |
| POST | `/open/api/v1/platform/app/save` | 手动保存/更新接入配置 |
| POST | `/open/api/v1/platform/opc/app/list` | 列出当前用户的接入 App |
| POST | `/open/api/v1/platform/login/url` | 获取登录入口 URL |

### 2.2 管理 API

根路径 **`/open/admin/opc/platform/*`**。页面：`opcmanage.html`。

配置项 **`OpcConstants.CONFIG_AUTO_REGISTER`**（`sys_config` 键 `OPC_AUTO_REGISTER`，**默认 false**；开发联调可设为 `true`）。

**安全**：`/open/oauth2/opc/authorize` 绑定 Session state；`/open/api/v1/platform/**` 须有效用户令牌（禁止 hint-only）。

## 3. 同实例联调

1. **OPL** 注册 App：`redirectUri` = `{ORIGIN}/open/oauth2/callback?appId={appId}`
2. **OPC** `POST /open/api/v1/platform/app/save`：`platformBaseUrl` = 本机根地址，凭证与 OPL 一致
3. 访问 `{ORIGIN}/open/oauth2/login?appId=...` → 点击授权 → OPL 确认 → 回调 → Session

**浏览器流程**：

```
/open/oauth2/login → /open/oauth2/opc/authorize → /open/oauth2/authorize（OPL）
→ /open/oauth2/callback → /open/oauth2/success
```

---

## 4. 本地用户绑定

`ConnectBindService.resolveAndBind` 顺序：

1. `(connectApp, openId)` 已绑定 → 登录本地用户
2. `(connectApp, unionId)` 已绑定 → 更新 openId 并登录
3. 无绑定且 `OPC_AUTO_REGISTER=true`（默认）→ 自动注册并绑定
4. 否则需管理员在 `opcmanage` 手工绑定

---

## 5. 与 LOGIN_AUTHENTICATION 的关系

- 传统 `oauth2:{clientId}` 登录**不受影响**
- Open 接入走独立 **`/open/oauth2/*`**，与 `/oauth2/*` 并行

---

## 6. 相关文档

- 开放平台提供方：**`docs/AI_OPL_INTEGRATION.md`**
- 双轨总览：**`docs/AI_AUTH_LOGIN_MODES.md`**
- 传统 OAuth：**`docs/AI_OAUTH_INTEGRATION.md`**

文档页：`{ORIGIN}/modules/docs/opc-integration`
