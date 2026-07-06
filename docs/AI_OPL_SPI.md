# OPL 开放平台扩展标准（Service / Extension）

> **适用对象**：在 Autumn 应用内扩展 OPL 能力的业务模块（无需 fork `cn.org.autumn.modules.opl`）  
> **契约包**：`cn.org.autumn.opl.*`（**autumn-lib**，业务仓仅依赖 lib 即可编写扩展）  
> **默认实现**：`cn.org.autumn.modules.opl`（**autumn-modules**）  
> **HTTP 对接**：第三方开发者仍见 [`AI_OPL_INTEGRATION.md`](AI_OPL_INTEGRATION.md)

---

## 1. 架构分层

```
┌─────────────────────────────────────────────────────────┐
│  业务项目（你的模块）                                        │
│  @Component 实现 OpenPlatformExtension                    │
│  或注入 OpenPlatformService 编程式调用                      │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  autumn-lib  cn.org.autumn.opl                           │
│  OplConstants · model.* · spi.*                          │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  autumn-modules  cn.org.autumn.modules.opl               │
│  OpenPlatformServiceImpl · OplExtensionService · OAuth/API │
└─────────────────────────────────────────────────────────┘
```

**原则**：

- 跨项目复用的**类型、常量、SPI 接口**放 **autumn-lib**
- 表实体、Dao、Controller、默认 Service 放 **autumn-modules**
- 业务扩展只新增 Spring Bean，通过 `@Order` 控制调用顺序

**对外契约**：

| 接口 | 用途 |
|------|------|
| `OpenPlatformService` | 编程式调用（Account / App / Identity / OAuth） |
| `OpenPlatformExtension` | 同步扩展 hook（校验、授权生命周期、身份算法、userInfo） |
| `OpenPlatformSubscriber` | 领域事件异步订阅 |

---

## 2. 标准常量与路径

引用 `cn.org.autumn.opl.OplConstants`（事件名见内部类 `OplConstants.Event`）：

| 常量 | 值 | 说明 |
|------|-----|------|
| `OAUTH2_BASE` | `/opl/oauth2` | authorize / token / userInfo |
| `API_V1_BASE` | `/opl/api/v1` | 开发者 Open API |
| `ADMIN_BASE` | `/opl/admin` | 后台管理 API |
| `MANAGE_PAGE` | `oplmanage.html` | 统一管理页（模板 `opl/oplmanage.html`） |
| `PARAM_APP_ID` / `PARAM_APP_SECRET` | `app_id` / `app_secret` | OAuth 参数名 |
| `DEFAULT_SCOPE` | `basic` | 默认授权 scope |
| `AUTH_CODE_TTL_SECONDS` | 300 | 授权码 TTL |
| `ACCESS_TOKEN_TTL_SECONDS` | 86400 | access_token TTL |
| `REFRESH_TOKEN_TTL_SECONDS` | 604800 | refresh_token TTL |
| `STATUS_ACTIVE` / `STATUS_DISABLED` | 1 / 0 | 账号与应用状态 |

OPC 侧常量见 `cn.org.autumn.opc.OpcConstants`（`ADMIN_BASE`、`MANAGE_PAGE`、`CONFIG_AUTO_REGISTER`）。

应用类型枚举：`cn.org.autumn.opl.model.OpenAppType`（与 DB/API 一致）。

---

## 3. 程序化服务 `OpenPlatformService`

业务模块注入服务接口，避免直接依赖 Entity/Dao：

```java
@Autowired
private OpenPlatformService openPlatformService;

public void onUserLogin(String appId, String userUuid) {
    OpenIdentitySnapshot identity = openPlatformService.resolveIdentity(appId, userUuid);
    // identity.getOpenId() / getUnionId()
}
```

### Account

| 方法 | 说明 |
|------|------|
| `getOrCreateAccount(userUuid, name)` | 按平台用户获取或创建开发者账号 |
| `getAccountByUser(userUuid)` | 查询开发者账号 |
| `requireActiveAccount(accountUuid)` | 校验并返回账号 |

### App

| 方法 | 说明 |
|------|------|
| `getApp` / `requireActiveApp` | 查询应用 |
| `listAppsByAccount(accountUuid)` | 账号下应用列表 |
| `registerApp(...)` | 注册（返回 `OpenAppRegisterOutcome`，含一次性明文 secret） |
| `updateApp(...)` | 更新名称、回调、scope、类型 |
| `updateAppStatus(appId, status)` | 启用/禁用 |
| `resetAppSecret(accountUuid, appId)` | 重置密钥 |
| `validateAppSecret` / `validateRedirectUri` | OAuth 参数校验 |

### Identity / OAuth

| 方法 | 说明 |
|------|------|
| `resolveIdentity` / `getIdentity` / `getIdentityByOpenId` | 身份查询 |
| `buildUserInfo(accessToken)` | 组装 userInfo（含 Extension enrichment） |
| `issueTokenFromCode` / `refreshToken` | 令牌交换 |

默认实现：`OpenPlatformServiceImpl`。业务仓可用 `@Primary` 装饰或覆盖。

---

## 4. 扩展点 `OpenPlatformExtension`

由 `OplExtensionService` 按 **`@Order` 升序** 调度；`supports(OpenAppType)` 默认 `true`，可按应用类型过滤。

| 回调 | 时机 |
|------|------|
| `validateRegister` / `validateRedirectUri` / `validateScope` | 注册与 OAuth 参数校验（抛异常即拒绝） |
| `beforeAuthorizePage` / `beforeApprove` | 授权页展示 / 用户确认前 |
| `afterCodeIssued` / `afterTokenIssued` | 授权码 / access_token 签发后 |
| `enrichUserInfo` | userInfo 默认字段填充后 |
| `relaxedRedirectMatch` | 是否跳过 redirectUri 精确匹配 |
| `generateOpenId` / `generateUnionId` | 覆盖默认 ID 算法（返回 null 则用框架默认） |

```java
@Component
@Order(100)
public class MyOplExtension implements OpenPlatformExtension {
    @Override
    public boolean supports(OpenAppType appType) {
        return appType == OpenAppType.MiniProgram;
    }

    @Override
    public void beforeApprove(OpenAppSnapshot app, OpenAuthorizationRequest request) throws Exception {
        // 例如：校验用户是否完成实名
    }

    @Override
    public void enrichUserInfo(OpenAppSnapshot app, OpenIdentitySnapshot identity, OpenUserInfoSnapshot userInfo) {
        userInfo.setNickname(userInfo.getNickname() + " (VIP)");
    }

    @Override
    public boolean relaxedRedirectMatch(OpenAppSnapshot app) {
        return true;
    }
}
```

### 4.1 领域事件 `OpenPlatformSubscriber`

事件名见 `cn.org.autumn.opl.OplConstants.Event`：

| 事件 | 常量 |
|------|------|
| App 注册 | `Event.APP_REGISTERED` |
| 密钥重置 | `Event.APP_SECRET_RESET` |
| 授权码签发 | `Event.CODE_ISSUED` |
| Token 签发 | `Event.TOKEN_ISSUED` |
| 身份解析 | `Event.IDENTITY_RESOLVED` |
| Union 创建 | `Event.UNION_CREATED` |

```java
@Component
@Order(200)
public class OplAuditSubscriber implements OpenPlatformSubscriber {
    @Override
    public String events() {
        return "opl.app.registered,opl.oauth.token_issued";
    }
    @Override
    public void onEvent(OpenPlatformEvent event) {
        // 审计日志、MQ、Webhook...
    }
}
```

---

## 5. 标准数据模型（lib）

| 类 | 用途 |
|----|------|
| `OpenAccountSnapshot` | 开发者账号只读视图 |
| `OpenAppSnapshot` | App 只读视图（无 secret） |
| `OpenAppRegisterOutcome` | 注册/重置密钥结果（含一次性明文 secret） |
| `OpenIdentitySnapshot` | openId + unionId + appId + user |
| `OpenUserInfoSnapshot` | userInfo 响应体 |
| `OpenTokenSnapshot` | token 交换结果 |
| `OpenAuthorizationRequest` | 授权上下文 |
| `OpenPlatformEvent` | 事件载荷 |

模块内 Entity **不得** 作为跨模块 API 暴露；对外统一 Snapshot / Outcome。

---

## 6. 与 OPC 模块的关系

- **OPL**：开放平台提供方（appId 体系、OAuth2 `/opl/oauth2/*`）
- **OPC**：接入方 RP（见 [`AI_OPC_INTEGRATION.md`](AI_OPC_INTEGRATION.md)）

业务项目若同时部署两者：在 OPC 回调中注入 `OpenPlatformService` 做本地用户绑定，或通过 `OpenPlatformSubscriber` 监听 `opl.oauth.token_issued` 异步关联。

---

## 7. 扩展清单（上线前）

- [ ] 跨模块调用 → 注入 `OpenPlatformService`，不直接调 `OpenAppService`
- [ ] HTTPS / 回调域白名单 → `OpenPlatformExtension#validateRegister` / `validateRedirectUri`
- [ ] 小程序/公众号 → `supports(MiniProgram)` + `relaxedRedirectMatch`
- [ ] userInfo 额外字段 → `enrichUserInfo`
- [ ] 审计/同步 → `OpenPlatformSubscriber`
- [ ] 常量与路径 → 引用 `OplConstants`，避免硬编码

---

## 8. 相关文档

| 文档 | 用途 |
|------|------|
| [`AI_OPL_INTEGRATION.md`](AI_OPL_INTEGRATION.md) | 第三方 HTTP 对接 |
| [`AI_OPC_INTEGRATION.md`](AI_OPC_INTEGRATION.md) | 本系统作为 RP 接入 |
| [`AI_OAUTH_INTEGRATION.md`](AI_OAUTH_INTEGRATION.md) | 平台 OAuth client_id 体系（与 OPL appId **并行**） |
| [`AI_MAP.md`](AI_MAP.md) §2.6 | Handler 扩展机制总述 |

---

*维护：OPL SPI 变更时同步更新本文、`AI_OPL_INTEGRATION.md` 与 `autumn-framework-2x` Skill。*
