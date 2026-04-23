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

## 3. 通用 API（可直接复用）

控制器：`cn.org.autumn.modules.sys.controller.SysSessionController`

- `GET /sys/session/self/list`
  - 当前用户会话列表（`sessions`）
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

## 5. 业务项目接入建议

- 管理端 iframe/壳页面：直接引入 `autumn-session-guard.js` 并调用 `AutumnSessionGuard.start(60000)`。
- 自定义「我的会话」页面：优先复用 `/sys/session/self/*` API，不再自建重复接口。
- 若项目有已有弹窗组件，可在收到 `401 + reason=session_terminated` 后替换为业务弹窗文案。
