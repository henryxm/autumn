# 会话终止与重新登录守卫（Shiro / RememberMe）

## 1. 目标

- 让业务项目复用 Autumn 基础能力实现「终止会话后不可被 RememberMe 自动恢复」。
- 提供通用会话自助接口与前端轮询守卫，避免每个项目重复造轮子。

## 2. 核心机制

### 2.1 强制重登标记（Redis）

- Key 前缀：`RedisKeys.getForceLogoutPrefix(namespace)`，默认 `system:logout:*`
- 单用户 Key：`RedisKeys.getForceLogoutKey(namespace, userUuid)`
- 语义：被标记用户在 TTL 内不允许通过 RememberMe 自动恢复登录。

### 2.2 RememberMe 拦截

- `ForceLogoutRememberMeManager` 在 `getRememberedPrincipals` 中检查强制重登标记。
- 命中时会调用 `forgetIdentity(...)` 清除 rememberMe cookie，并返回 `null`，要求重新输入密码。

### 2.3 会话删除补标记

- `ShiroSessionService.deleteSession(...)` 删除会话后会按会话归属用户写入强制重登标记。
- 这样即使会话被删除，旧浏览器也不能依靠 rememberMe 静默恢复。

### 2.4 登录成功自动清标记

- `SysUserService.login(...)` 登录成功后会调用 `shiroSessionService.clearForceLogout(userUuid)`。
- 含义：用户已经重新完成密码认证后，可恢复正常 rememberMe 行为。

### 2.5 显式登出写强制重登标记

- `GET /logout`、`/oauth2/authorize/logout`、开放平台 `/open/oauth2/authorize/logout` 在清理 Shiro 身份前会调用 `SysLogoutSupport.logoutAndForceReauth(...)`，写入与 §2.1 相同的强制重登标记。
- 与 `LogoutSkipSupport`（120s 内跳过登录页 `checkenv`）互补：前者阻断 RememberMe 跨页静默恢复，后者阻断 dev 静默 admin 探测。

### 2.6 登录页 autologin 探测（`POST /sys/autologin`）

- **默认关闭**（`ACCOUNT_AUTH_CONFIG.devAutologinEnabled=false`）：登录页**不**自动跳转、**不**静默登录；已登录会话访问 `/login` 也停留登录页（`reason=session_active`）。
- **开启后**（仅 dev 环境）：未登录时返回 `devProbe=true`，供 `checkenv` 静默 `admin/admin` 探测；仍**不**对已认证会话或 RememberMe 自动跳转。
- RememberMe 半登录态始终返回 `reason=remember_me_blocked`。
- 登出后 Cookie `autumn_skip_autologin` 存在时返回 `reason=skipped`。
- 前端：`statics/js/autologin-check.js`；`devAutologinEnabled=false` 时前端不发起探测请求。

## 3. 通用 API（可直接复用）

控制器：`cn.org.autumn.modules.sys.controller.SysSessionController`

- `GET /sys/session/self/list`
  - 当前用户会话列表（`sessions`）；`host`（访问来源）由 `SpmFilter` 经 `IPUtils.getIp` 写入会话，展示时读 `ClientIpSessionSupport.displayHost`
- `POST /sys/session/self/terminate`
  - 终止指定会话（不可终止当前会话）
  - body: `{ "sessionId": "..." }`
- `POST /sys/session/self/terminate-others`
  - 终止当前用户除当前会话外的其它会话
- `GET /sys/session/self/ping`
  - 会话保活检测
  - 若命中强制重登且当前仅 rememberMe 身份，则返回 `401` + `reason=session_terminated`

## 4. 前端守卫（默认实现）

- 文件：`statics/js/autumn-session-guard.js`
- 能力：
  - 定时调用 `/sys/session/self/ping`
  - 收到 401 时写入 `sessionStorage` 并跳转 `login.html?sessionExpired=1&reason=...`
- 入口页默认接入：
  - `templates/index.html`

登录页提示：

- `templates/login.html` 会读取 query/sessionStorage 并显示会话过期或会话已终止提示。

### 4.1 开发环境意外自动登录（复现与验证）

**典型复现（dev + default 主题，修复前）**：Safari 打开 `/login` → 页面加载触发 `checkenv` → 服务端在未登录时返回成功 → 前端静默 `POST /sys/login`（dev 补 `admin/admin`）→ 用户感知为「刷新即自动登录」。

**修复后验证**：

1. 清除站点 Cookie 后打开 `/login`，应停留登录页（默认 `devAutologinEnabled=false`，不发起 checkenv）。
2. 已登录状态下打开 `/login`，仍停留登录页，不自动跳转。
3. 显式登出后刷新 `/login`，不应 autologin；RememberMe 不应静默恢复（见 §2.5）。
4. 需本地单人调试时，将 `ACCOUNT_AUTH_CONFIG.devAutologinEnabled` 设为 `true` 后，dev 环境刷新才可静默 admin 探测。

登录 **成功后的默认落地页**（非 SavedRequest 场景）由 **`ACCOUNT_AUTH_CONFIG.postLoginRedirect`** 配置，见 **`docs/AI_ACCOUNT_AUTH_CONFIG.md`**。

## 5. 业务项目接入建议

- 管理端 iframe/壳页面：直接引入 `autumn-session-guard.js` 并调用 `AutumnSessionGuard.start(60000)`。
- 自定义「我的会话」页面：优先复用 `/sys/session/self/*` API，不再自建重复接口。
- 若项目有已有弹窗组件，可在收到 `401 + reason=session_terminated` 后替换为业务弹窗文案。
