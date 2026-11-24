# 多域名部署与授权登录（AI_MULTI_DOMAIN）

同一 Autumn 实例绑定多个域名（含 **独立 TLD** 与 **同一根域子域** 混合）时的会话隔离、扫码 URL 与授权页 Tab 行为说明。

> 授权登录总览见 [`AI_AUTH_LOGIN_MODES.md`](AI_AUTH_LOGIN_MODES.md)；扫码 API 见 [`AI_QRC_API.md`](AI_QRC_API.md)。

---

## 1. 场景与目标

| 场景 | 行为 |
|------|------|
| `a.com` 与 `b.com` 同时访问 | 各域名 **独立 Shiro 会话**（host-only Cookie `autumnid`），可同时登录不同账号 |
| `auth.x.com` 与 `app.x.com` | 配置 `CLUSTER_ROOT_DOMAIN=x.com` 时，子域共享 Cookie Domain=`x.com`（Rfc6265 **无前导点**；需 Redis Session） |
| OAuth 授权页 `/oauth2/authorize` | 登录成功后 **保留登录方式 Tab**（账号 / 扫码 / 手机），在对应 Tab 展示用户信息 |

---

## 2. 配置项

| 键 | 说明 |
|----|------|
| `SITE_DOMAIN` | 逗号分隔的站点域名，如 `a.com,b.com,auth.x.com` |
| `BIND_DOMAIN` | 防火墙放行域名，建议与 `SITE_DOMAIN` 同步 |
| `CLUSTER_ROOT_DOMAIN` | 子域集群根域：写 **`bighub.cn` / `x.com`**；**禁止**写成 **`.bighub.cn`**（Tomcat `Rfc6265CookieProcessor` 会拒 `Domain=.…`） |
| `SITE_SSL` | 是否按 HTTPS 生成 canonical URL |
| `autumn.redis.open` + `autumn.shiro.redis` | 分布式 Session（子域共享或集群部署建议开启） |

### 混合域名推荐

```
SITE_DOMAIN=a.com,b.com,auth.x.com
BIND_DOMAIN=a.com,b.com,auth.x.com
CLUSTER_ROOT_DOMAIN=x.com
```

- 访问 `a.com` / `b.com`：`HostSessionCookieFilter` 使用 **host-only** Cookie。
- 访问 `auth.x.com`：Host 为集群根子域时，Cookie Domain=`x.com`（覆盖 `*.x.com`，含 apex 行为依浏览器）。
- 扫码绑定 Cookie 与会话 Cookie **共用** `HostSessionCookieSupport.resolveCookieDomain`（见 §3.2）。

---

## 3. 实现要点

### 3.1 请求级会话 Cookie（`HostSessionCookieFilter` + `HostAwareSessionIdCookie`）

每个请求在 **ThreadLocal** 中解析 Cookie name/domain，由 `HostAwareSessionIdCookie` 在读写 Set-Cookie 时读取，**不 mutating** 全局 Shiro Cookie 单例，避免多 Host 并发互相覆盖。

`resolveCookieDomain` 返回值 **永远无前导点**（与 Netscape 旧写法 `Domain=.x.com` 不同）。`normalizeHost` 会剥掉配置里误写的前导 `.`。

### 3.2 扫码浏览器绑定 Cookie（`ScanBrowserBindSupport`）

登录/授权页 `POST /qrc/scanticket/web/ticket/create`（及 RP `/client/oauth2/qrc/web/*`）建票时下发：

| Cookie | 作用 |
|--------|------|
| `autumn_qrc_bind_{uuid}` | 主绑定（多标签互不覆盖） |
| `autumn_qrc_bind` | 兼容旧单 Cookie（`uuid.secret`） |

后续 `status` / `stream` / `exchange` 须带匹配 Cookie，防仅凭 uuid 跨浏览器抢登。

**Domain 规则**：与 §3.1 相同。若 `addCookie` 仍因非法 Domain 抛 `IllegalArgumentException`，框架降级为 **host-only**（不设 Domain），保证建票 HTTP 200 仍能出码。

### 3.3 扫码 QR URL（`ScanTicketService.buildQrUrl`）

授权页预建票据时，QR 链接使用 **当前请求 Host**（`WebPathUtils.absoluteBaseUrl`），不再固定 `getBaseUrl()` 首域。

### 3.4 CSRF 与登录扫码（勿与 Domain 问题混淆）

| 现象 | 是否挡住出码 | 说明 |
|------|--------------|------|
| `GET /sys/session/self/csrf` → **401** | **否** | 未登录访问属预期（Shiro `spm`）；`login.html` 建票不依赖该 token |
| 已登录用户打开 `/login` 再扫码 | 已规避 | `CsrfProtectFilter` 豁免 `/qrc/scanticket/web/**`、`/client/oauth2/qrc/web/**` |
| 二维码区文案 `An invalid domain [.bighub.cn]…` | **是（已修）** | 旧逻辑 `Domain=.bighub.cn` → Tomcat 拒写 Cookie → `webCreate` 把异常 msg 回给前端 |

### 3.5 授权页 Tab 状态（`AuthAuthorizeLoginSupport`）

| 登录方式 | Session / URL 参数 |
|----------|-------------------|
| 账号 | `loginTab=account` |
| 手机 | `loginTab=phone` |
| 扫码 | `loginTab=qr` |

登出授权页（`/oauth2/authorize/logout`、`/open/oauth2/authorize/logout`）会清除 Tab 状态。

---

## 4. 反向代理

`WebPathUtils.absoluteBaseUrl` / Cookie Host 解析读取：

- `X-Forwarded-Proto`（scheme）
- `Host`（优先；扫码绑定 Domain 也依此）

请确保网关 **`Host`（或等价）与浏览器地址栏一致**，否则 Domain 可能与站点不符。

---

## 5. 排障：登录二维码空白

1. Network → `POST .../ticket/create`：若 `code!=0` 且 `msg` 含 `invalid domain` → 检查 `CLUSTER_ROOT_DOMAIN` 是否带前导 `.`，并确认已部署含 Rfc6265 Domain 修复的版本。  
2. Response `Set-Cookie`：应见 `autumn_qrc_bind_*`；`Domain=` 若存在须为 `bighub.cn` 而非 `.bighub.cn`。  
3. Application → Cookies：刷新后应有绑定 Cookie。  
4. `GET /sys/session/self/csrf` 401：可忽略，**不是**出码失败原因。

---

## 6. 相关代码

| 类 | 路径 |
|----|------|
| `HostSessionCookieSupport` | `autumn-modules/.../sys/shiro/` |
| `HostAwareSessionIdCookie` | `autumn-modules/.../sys/shiro/` |
| `ScanBrowserBindSupport` | `autumn-modules/.../qrc/support/` |
| `CsrfProtectFilter` | `autumn-modules/.../sys/filter/` |
| `AuthAuthorizeLoginSupport` | `autumn-modules/.../oauth/oauth2/support/` |
| `login.html` / `autumn-qrc-core.js` | 登录扫码 UI |

---

## 7. 回归检查

- [ ] 两无关域名各开浏览器 Tab，分别登录，会话互不影响
- [ ] 在 `b.com` 打开授权页，二维码 Host 为 `b.com`
- [ ] `https://{apex}/login` 扫码 Tab 能出码；`ticket/create` 200；无 `invalid domain` 文案
- [ ] `CLUSTER_ROOT_DOMAIN` 误配为 `.例.com` 时，`normalizeHost` 仍可出码（或降级 host-only）
- [ ] 扫码 / 账号 / 手机登录后，左侧停留在对应 Tab 并显示用户信息
- [ ] 右侧「确认授权」在已登录且勾选协议后可点击
