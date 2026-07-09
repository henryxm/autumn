# 多域名部署与授权登录（AI_MULTI_DOMAIN）

同一 Autumn 实例绑定多个域名（含 **独立 TLD** 与 **同一根域子域** 混合）时的会话隔离、扫码 URL 与授权页 Tab 行为说明。

> 授权登录总览见 [`AI_AUTH_LOGIN_MODES.md`](AI_AUTH_LOGIN_MODES.md)；扫码 API 见 [`AI_QRC_API.md`](AI_QRC_API.md)。

---

## 1. 场景与目标

| 场景 | 行为 |
|------|------|
| `a.com` 与 `b.com` 同时访问 | 各域名 **独立 Shiro 会话**（host-only Cookie `autumnid`），可同时登录不同账号 |
| `auth.x.com` 与 `app.x.com` | 配置 `CLUSTER_ROOT_DOMAIN=x.com` 时，子域共享 `.x.com` Cookie（需 Redis Session） |
| OAuth 授权页 `/oauth2/authorize` | 登录成功后 **保留登录方式 Tab**（账号 / 扫码 / 手机），在对应 Tab 展示用户信息 |

---

## 2. 配置项

| 键 | 说明 |
|----|------|
| `SITE_DOMAIN` | 逗号分隔的站点域名，如 `a.com,b.com,auth.x.com` |
| `BIND_DOMAIN` | 防火墙放行域名，建议与 `SITE_DOMAIN` 同步 |
| `CLUSTER_ROOT_DOMAIN` | 子域集群根域；**勿**用于无关 TLD 的强制 Cookie 共享 |
| `SITE_SSL` | 是否按 HTTPS 生成 canonical URL |
| `autumn.redis.open` + `autumn.shiro.redis` | 分布式 Session（子域共享或集群部署建议开启） |

### 混合域名推荐

```
SITE_DOMAIN=a.com,b.com,auth.x.com
BIND_DOMAIN=a.com,b.com,auth.x.com
CLUSTER_ROOT_DOMAIN=x.com
```

- 访问 `a.com` / `b.com`：`HostSessionCookieFilter` 使用 **host-only** Cookie。
- 访问 `auth.x.com`：若 Host 为 `x.com` 子域，Cookie domain 为 `.x.com`。

---

## 3. 实现要点

### 3.1 请求级 Cookie（`HostSessionCookieFilter` + `HostAwareSessionIdCookie`）

每个请求在 **ThreadLocal** 中解析 Cookie name/domain，由 `HostAwareSessionIdCookie` 在读写 Set-Cookie 时读取，**不 mutating** 全局 Shiro Cookie 单例，避免多 Host 并发互相覆盖。

### 3.2 扫码 QR URL（`ScanTicketService.buildQrUrl(request, uuid)`）

授权页预建票据时，QR 链接使用 **当前请求 Host**（`WebPathUtils.absoluteBaseUrl`），不再固定 `getBaseUrl()` 首域。

### 3.3 授权页 Tab 状态（`AuthAuthorizeLoginSupport`）

| 登录方式 | Session / URL 参数 |
|----------|-------------------|
| 账号 | `loginTab=account` |
| 手机 | `loginTab=phone` |
| 扫码 | `loginTab=qr` |

登出授权页（`/oauth2/authorize/logout`、`/open/oauth2/authorize/logout`）会清除 Tab 状态。

---

## 4. 反向代理

`WebPathUtils.absoluteBaseUrl` 读取：

- `X-Forwarded-Proto`（scheme）
- `X-Forwarded-Host` 或 `Host`（主机名）

请确保网关正确转发上述头，否则 QR 链接 scheme/Host 可能错误。

---

## 5. 相关代码

| 类 | 路径 |
|----|------|
| `HostSessionCookieSupport` | `autumn-modules/.../sys/shiro/` |
| `HostSessionCookieContext` | `autumn-modules/.../sys/shiro/` |
| `HostAwareSessionIdCookie` | `autumn-modules/.../sys/shiro/` |
| `AuthAuthorizeLoginSupport` | `autumn-modules/.../oauth/oauth2/support/` |
| `login.html` / `login.js` | 授权页 UI |

---

## 6. 回归检查

- [ ] 两无关域名各开浏览器 Tab，分别登录，会话互不影响
- [ ] 在 `b.com` 打开授权页，二维码 Host 为 `b.com`
- [ ] 扫码 / 账号 / 手机登录后，左侧停留在对应 Tab 并显示用户信息
- [ ] 右侧「确认授权」在已登录且勾选协议后可点击
