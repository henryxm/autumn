# AuthContext：请求级鉴权上下文

> **一句话**：解析登录身份时，把「怎么进来的 + 哪把 accessKey」挂在当前请求上，Controller 随时可读。  
> **适用**：Autumn **3.0.0**（`jakarta.*`）；含 minclouds 遗留 `UserInfoResolver`。

---

## 1. 为什么需要

开放 API（如 `/aas/api/v1/register`）往往只知道「哪个用户」，不知道：

- 用的是 **Session**、**OAuth access_token**，还是 **影子 AccessKey**；
- 同一用户下 **哪一把 accessKey** 在调用（防滥用）。

`AuthContext` 在**鉴权解析成功**时写入这些元数据，**不改变**登录成败与用户 uuid。

---

## 2. 30 秒上手（Controller）

```java
import cn.org.autumn.web.AuthContext;
import cn.org.autumn.utils.IPUtils;

// AuthContext 写在 @Authenticated 身份参数之后（先绑定再注入）
@PostMapping("/register")
public Response<?> register(
        @RequestBody DeviceRequest body,
        @Authenticated UserInfo userInfo,
        AuthContext auth,
        HttpServletRequest request) {

    log.info("注册: user={}, AccessKey={}, Source={}, IP={}",
            userInfo.getUuid(),
            AuthContext.accessKeyOf(auth),
            AuthContext.sourceOf(auth),
            IPUtils.getIp(request));
}
```

也可：`AuthContexts.current()`（无参数时）。

| 常用 API | 说明 |
|----------|------|
| `AuthContext.accessKeyOf(auth)` | 空安全 accessKey |
| `AuthContext.sourceOf(auth)` | 空安全来源名 |
| `auth.auditLabel()` | `"Source=…, AccessKey=…"` |
| `AuthContexts.current()` | 从当前请求取 |

---

## 3. 数据长什么样

| 字段 | 含义 |
|------|------|
| `source` | 见下表 `AuthSource` |
| `userUuid` | 最终业务用户（影子换真人后为真人） |
| `actorUuid` | 机器人 / 换人前影子账号（可空） |
| `accessKey` | OAuth `client_id`（秘钥 ID，**不是** secret） |
| `credentialHeader` | 命中的头：`Authorization` / `Token` / … |
| `tokenPrefix` | 安全前缀（长令牌前 8 位；短令牌不写） |
| `hintPresent` / `hintUuid` | 是否带了 `userUuid` hint |

**禁止**写入：`client_secret`、完整 `access_token`。

### AuthSource（怎么进来的）

| 值 | 什么时候 |
|----|----------|
| `OAUTH_TOKEN` | Bearer/OAuth access_token；或 Session 由同源 API 令牌建立 |
| `ACCESS_KEY_SHADOW` | 影子账号 `username=client_id` 换成真人 |
| `ACCESS_KEY_FROM_TOKEN` | Account 远程按 token 反查 |
| `USER_TOKEN` | 遗留 `usr_user_token` / `UserTokenFactory` |
| `SESSION` | 纯浏览器 Session，且无可用同源 OAuth 凭证 |
| `UUID_HINT` | 仅 hint（安全敏感，默认常关闭） |
| `ROBOT_TOKEN` | `rbt_` / 机器人会话 |
| `NONE` | 未解析 |

---

## 4. 请求怎么走到 AuthContext

```text
Headers (Authorization / Token / …)
        │
        ▼
 ApiAuthSupport.extractCredential   →  token + 头名
        │
        ├─ UserContextService.resolve     （新 API：UserContext）
        └─ UserInfoResolver               （遗留：@Authenticated UserInfo）
                │
                │  单次 ApiTokenLoginSupport.resolveOAuth → clientId
                │  Session+Token 同源 → AuthContextBinder.enrichSessionWithOAuth
                ▼
        AuthContexts.bind(request, …)
                │
                ▼
        Controller 注入 AuthContext / current()
```

**凭证头优先级**：`X-Robot-Token` → `Robot-Token` → `Token` → `Auth` → `Authenticated` → `Authorization`。

---

## 5. 完整性约定（实现已保证）

1. **Session + Authorization 并存**（常见：SpmFilter 用 token 建会话）→ 同源则补齐 `accessKey`，source 提升为 `OAUTH_TOKEN`。  
2. **hint + token** → 身份仍按 hint（历史语义）；token 同用户则精化 source / accessKey。  
3. **OAuth 只查一次** TokenStore（`OAuthTokenInfo`）。  
4. **他人 token 的 clientId** 不会写入当前用户上下文。  
5. **参数顺序**：`AuthContext` 放在身份参数**之后**。

---

## 6. 类与文档入口

| 类 | 模块 | 职责 |
|----|------|------|
| `AuthContext` / `AuthSource` / `AuthContexts` | `autumn-lib` | 模型与读写 |
| `AuthContextBinder` / `OAuthTokenInfo` | `autumn-modules` | 组装、Session 富化 |
| `ApiAuthSupport` / `ApiTokenLoginSupport` | `autumn-modules` | 抽头、OAuth 解析 |
| `AuthContextArgumentResolver` | `autumn-modules` | 参数注入 |
| `UserContextService` | `autumn-modules` | 新解析器绑定 |
| `UserInfoResolver` | minclouds-basic | 遗留解析器绑定 |

索引：`docs/AI_INDEX.md` → 本页。相关选型见 `AI_AUTH_LOGIN_MODES.md`（勿与本页混淆：那是登录体系，本页是**请求元数据**）。

---

## 7. 边界

- 只增元数据，不改 `-10000` / 鉴权成败。  
- 绑定失败不抛、不阻断业务。  
- debug 日志只打 `tokenPrefix`。  
- accessKey 权威顺序：`TokenStore.clientId` → Account 反查 → 影子 `username`。
