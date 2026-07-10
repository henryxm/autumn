# 登录页授权 Provider（AI_AUTH_LOGIN_PROVIDERS）

本文档说明 `/login` 授权登录列表、`GET /auth/login/providers` 契约，以及经典 OAuth RP 与开放平台 OPC 的配置与排查。总览见 [`AI_AUTH_LOGIN_MODES.md`](AI_AUTH_LOGIN_MODES.md)。

## 1. 显式 vs 隐式登录

| 概念 | 说明 |
|------|------|
| **隐式登录** | 用户通过 `/oauth2/login`、`/open/oauth2/login` 或业务内嵌 URL 直接进入授权流程；`pageLogin=0`（默认）时不在 `/login` 展示。 |
| **显式登录** | 在 `/login` Tab 下方展示图标列表；需 `pageLogin=1` 且满足 §4 准入条件。 |

**重要**：无任何 Provider 满足展示条件时，`visible=false`，登录页 UI 与改造前完全一致（无 divider、无额外区块）。

## 2. 类型与 API

### 2.1 类型常量

| type | 含义 | 专有字段 | 登录入口 |
|------|------|----------|----------|
| `oauth2_classic` | 经典 OAuth RP | `clientId`、`sameInstance` | `/oauth2/login?client_id=` |
| `oauth2_open` | 开放平台 OPC | `appId`、`platformBaseUrl` | `/open/oauth2/login?appId=` |

### 2.2 `GET /auth/login/providers`

- Shiro：`anon`
- 注解：`@SkipInterceptor`

**零展示（与原版一致）**：

```json
{
  "code": 0,
  "data": {
    "visible": false,
    "defaultIconUrl": "/statics/img/auth-login-default.svg",
    "providers": []
  }
}
```

**有数据时**：`visible = !providers.isEmpty()`；前端仅以 `data.visible === true` 渲染「授权登录」区块。

`loginUrl` 不含 `callback`；`login.js` 的 `buildProviderUrl` 按当前页安全 callback 追加。

默认图标：`statics/img/auth-login-default.svg`。

## 3. 管理端字段

### WebAuthenticationEntity（oauthrpmanage）

| 字段 | 默认 | 说明 |
|------|------|------|
| `icon` | 空 | 登录页图标 URL，空则前端用 `defaultIconUrl` |
| `hash` | 空 | 上传图标文件 hash，供 `UsingHandler` / 文件清理判断引用 |
| `pageLogin` | `0` | `0` 不展示；`1` Tab；`2` 扫码；`3` Tab+扫码 |

### ConnectAppEntity（opcmanage）

同名字段，语义一致。

## 4. 准入条件（Service 层）

任一不满足则跳过，且不计入 `visible`。

### 经典 `oauth2_classic`

| 条件 | 说明 |
|------|------|
| `pageLogin == 1 或 3` | Tab 显式开启 |
| `clientId`、`clientSecret` 非空 | 可换票 |
| `redirectUri` 非空 | 回调可达 |
| 授权端点可解析 | `WebOauthEndpointResolver.resolveAuthorizeUri` 非空 |
| 站点 RP 已启用 | `AuthSiteRoleService.isRpEnabled()` |

### 开放 `oauth2_open`

| 条件 | 说明 |
|------|------|
| `pageLogin == 1 或 3` | Tab 显式开启 |
| `status == STATUS_ACTIVE` | 未禁用 |
| `appId` 非空、`appSecret` 已配置 | API 不返回 secret |
| `redirectUri` 非空 | OPC 回调可达 |
| 授权端点可解析 | `authorizeUri` 非空或由 `platformBaseUrl` 推断 |

合并后按创建时间倒序；`sortOrder` 递增。响应含 `tabProviders`、`qrProviders`、`tabVisible`、`qrVisible`（`providers` 兼容等于 `tabProviders`）。

### 扫码 Provider（`qrProviders`）

| 条件 | 说明 |
|------|------|
| `pageLogin == 2 或 3` | 扫码区展示 |
| 凭证可解析 | `ScanLoginCredentialService.require(type, id)` 成功 |
| 视图字段 | `qrMode`（`as`/`rp`）、`credentialType`、`id` |

前端：`AutumnQrc.createMethods({ mode: p.qrMode, type: p.credentialType, id: p.id })` 或 `AutumnQrc.mergeInto`；RP/AS 均订阅 SSE `{prefix}/ticket/stream`（失败降级 `ticket/status`）；开放同源（`qrMode=as`）确认后 `POST /open/oauth2/qrc/web/complete`。

双模式扫码能力与验收见 **`docs/AI_SCAN_LOGIN_DUAL_MODE_REGRESSION.md`**（双模式均已完整支持）。

## 5. client_id 路由

`AuthSiteRoleService.resolveRpClientId` 顺序：

1. 请求参数 `client_id` / `clientId`
2. `LOGIN_AUTHENTICATION` 解析出的 clientId
3. 查 `WebAuthenticationService.getByClientId`

`/oauth2/login`：

| 条件 | 行为 |
|------|------|
| 有 `client_id` + 远程 `originUri` | `OauthRpLoginService.buildAuthorizeRedirect` 302 |
| 有 `client_id` + 同实例 | 渲染 `oauth2/login.html` |
| 无 `client_id` | 用 `LOGIN_AUTHENTICATION` 默认 client；未配置则 redirect `/login` |

`OauthRpStateService` state 携带 `clientId`；callback 优先从 state 恢复 client 上下文。`/client/oauth2/login` 薄代理至 `/oauth2/login`。

## 6. 登录页 UI

- `login.html`：`v-if="authLoginVisible"` 控制授权区块（绑定 API `visible`）
- `login.js`：`mounted` 拉取 providers；失败时静默隐藏
- `AuthPageSupport`：不再因 `LOGIN_AUTHENTICATION` 将整页设为 `oauthLogin=true`
- `oauthAuthorize` / `oplAuthorize` 分栏页永不展示 Provider 列表

## 7. 迁移说明

- 存量与新建 client/app 默认 `pageLogin=0`，登录页与旧版一致
- 需展示时：管理页开启 `pageLogin=1` 并补齐 §4 条件
- 隐式 `/oauth2/login`（无 client_id 用默认 client）不受影响

## 8. 故障排查

| 现象 | 检查 |
|------|------|
| `/login` 无授权区块 | `GET /auth/login/providers` 是否 `visible=false`；各实体 `pageLogin`、secret、redirectUri、端点 |
| 点击 Provider 404 | `loginUrl` 路径、RP/OPL 角色是否启用 |
| 远程 client 误跳本站 authorize | `originUri` 是否正确；`auth-flow.js` 远程应走 `/oauth2/login?client_id=` |
| callback 用了错误 client | state 是否携带 `clientId`；`resolveCallbackWebAuth` 是否 peek state |
| 整页变成 OAuth 表单 | 确认 `DefaultPage.login` 路径下 `oauthLogin` 为 false（非 authorize 模式） |

## 9. 相关代码

| 组件 | 路径 |
|------|------|
| 契约模型 | `autumn-lib/.../AuthLoginProvider*.java` |
| 列表服务 | `AuthLoginProviderService` |
| 公开 API | `AuthLoginProviderController` |
| RP 图标/hash | `WebAuthenticationService`（`UsingHandler`、`updateIcon`、`isIconHashInUse`） |
| OPC 图标/hash | `ConnectAppService`（`UsingHandler`、`updateIcon`、`isIconHashInUse`） |
| 登录页 JS | `statics/js/login.js` |
| 样式 | `statics/css/login-auth.css` |

### 9.1 扩展项目调用示例

```java
// 更新 RP 接入应用图标（clientId 为业务主键）
webAuthenticationService.updateIcon("my-client", "/statics/upload/abc.png", fileHash);

// 判断文件 hash 是否仍被引用（UsingFactory 会聚合所有 UsingHandler）
boolean inUse = webAuthenticationService.isIconHashInUse(fileHash);
// 或
boolean inUse = usingFactory.using(fileHash);

// OPC 同理
connectAppService.updateIcon("app_demo", iconUrl, fileHash);
connectAppService.isIconHashInUse(fileHash);
```

## 10. 相关页面

| 页面 | 路径 | 说明 |
|------|------|------|
| 开放接入管理 | `opcmanage.html` | 应用配置、`pageLogin`、运维绑定 Tab |
| 账号绑定（用户友好） | `/modules/opc/connectbind` | 用户自助解绑；冲突页「绑定管理」入口，见 **`docs/AI_OPC_INTEGRATION.md` §2.3** |
