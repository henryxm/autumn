# Autumn

Autumn 是一套可扩展的后台基础框架：在 Spring Boot 之上提供权限与系统管理、代码生成、两级缓存、分布式队列/锁、HTTP 混合加解密与字段存储加密、定时任务、机器人开放 API、支付安全凭证、扫码/OAuth/开放平台，以及 **管理 IP 门禁 + 运维双边 RSA 证书通道**（配合 **超然信** App）。配套 `docs/` 约束文档与 Cursor Skill，支持传统开发与 AI 协作并行。

> **本文按当前检出分支 `3.0.0`（JDK 17+）叙述。** 使用 `master` / artifact `2.0.0` 时请对照下方版本表，并以该分支的 `pom.xml` 为准。

**导读**：运维上手 → [启动密钥一览](#首次启动控制台密钥一览) · [超然信](#超然信客户端) · [登录分层](#管理员登录与访问分层) · [管理 IP](#管理-ip) · [运维证书](#运维证书)；研发上手 → [两条路径](#两条上手路径) · [菜单导览](#后台功能与扩展菜单) · [文档入口](#文档入口)。

---

## 版本与分支

| 线 | 分支 / artifact | 运行时 | 命名空间 | Cursor Skill |
|----|-----------------|--------|----------|--------------|
| **2.x** | `master` · `2.0.0` | JDK **8** · Spring Boot **2.7.18** · MyBatis-Plus **2.x** | `javax.*` | `.cursor/skills/autumn-framework-2x/SKILL.md` |
| **3.x（本文默认）** | **`3.0.0`** · **`3.0.0`** | JDK **17+** · Spring Boot **3.5.x** · MyBatis-Plus **3.x** | `jakarta.*` | `.cursor/skills/autumn-framework-3x/SKILL.md` |

业务工程请在 `AGENTS.md` 或首轮对话中**写明依赖的 Autumn 主版本**，避免 2.x / 3.x 规范与 Skill 混用。具体版本以根 `pom.xml` 为准。

---

## 两条上手路径

### 路径 A — 跑通本仓演示应用 `web`

1. 准备 MySQL（库名如 `autumn`，utf8mb4）与可选 Redis。
2. 修改 `web/src/main/resources/application-dev.yml` 数据源。
3. `mvn clean package -DskipTests`，运行 `cn.org.autumn.Web` 或 `web/target/web.jar`。
4. **查看启动日志**，按 [密钥一览](#首次启动控制台密钥一览) 保存管理员引导口令、`OpsEnroll`、`AdminIpBootstrap`（**全新库不再默认 `admin/admin`**）。
5. 用日志中的用户名/口令登录后台，登录后尽快改密；再用超然信完成运维证书入驻（见下文）。

### 路径 B — 业务仓引入 `autumn-starter`

1. 在业务工程 `pom` 依赖 `autumn-starter`（版本与分支对齐）。
2. 首轮 AI/人工开发阅读 [`docs/AI_BOOT.md`](docs/AI_BOOT.md) → [`docs/AI_INDEX.md`](docs/AI_INDEX.md) → [`docs/AI_STANDARDS.md`](docs/AI_STANDARDS.md)。
3. 新模块走代码生成三步（见 [`docs/AI_CODEGEN.md`](docs/AI_CODEGEN.md)）；菜单/页面用 `*Menu` / `*Site` + `@PageAware`。
4. 管理 API / gen 对齐管理 IP 门禁时读 [`docs/AI_ADMIN_IP_BIZ.md`](docs/AI_ADMIN_IP_BIZ.md)。

---

## 超然信客户端

运维证书的官方客户端是 **超然信**（移动端）。用它探测站点、粘贴入驻口令、管理 IP 与门禁开关，并在会话内分享运维密钥。

| 项 | 说明 |
|----|------|
| **官网** | [https://chaoran.xin/](https://chaoran.xin/) |
| **应用市场** | 在 App Store、华为应用市场、应用宝等搜索 **「超然信」** 安装 |
| **公开落地页** | `{站点 baseUrl}/autumn/ops/discover`（双轨：站点运维 + 超然信下载；受 `discoverEnabled` 控制，默认开；入驻成功后常自动关） |
| **客户端源码** | [`autumn-app`](https://github.com/henryxm/autumn-app.git)（`autumn_ops` / `autumn_ops_ui`；iOS Pod `AutumnOpsCore` / `AutumnOpsUI`） |
| **后台配置** | **系统管理 → 站点门户**：`SITE_PORTAL_CONFIG.chaoranxin`（`officialUrl` 默认官网、`installQrUrl`、`storeLinks[]`） |

关闭发现页只影响浏览器 HTML（对外可呈 404），**不禁止** App `probe`。登录后首页 `main` 始终可预览完整落地版面。

深入：[`docs/AI_SITE_PORTAL.md`](docs/AI_SITE_PORTAL.md) §2.3 · [`docs/AI_OPS_CERT.md`](docs/AI_OPS_CERT.md) §3.0 / §7。

---

## 首次启动：控制台密钥一览

应用初始化结束前后，控制台常见三类**一次性明文**（级别可能为 `INFO` / `WARN`）。**全部视为机密**：勿提交仓库、勿贴公开工单。

示例（与真实启动日志对齐，口令值每次不同）：

```text
INFO  ... AdminBootstrapPasswordSupport : Admin bootstrap password generated. Save it now.
      file=.../.autumn/security/admin-bootstrap.credentials username=admin password=******** Change it after login.
WARN  ... OpsCertService       : OpsEnroll secret=********
WARN  ... IpAdminWhiteService  : AdminIpBootstrap secret=********
```

| 日志关键字 | 配置 / 落盘 | 用途 | 何时出现 |
|------------|-------------|------|----------|
| **`Admin bootstrap password generated`** | 文件 `{autumn.node.home}/security/admin-bootstrap.credentials`；库中只存哈希 | **后台超管登录口令**（用户名多为 `admin`） | **全新库**且未配置 `system.password` / Env `systemPassword` 时，首次创建 admin |
| **`OpsEnroll secret=`** | 仅内存 hash → `SECURITY_OPS_CERT_CONFIG` | 超然信 **运维证书入驻** | 待入驻 / 证书 reset 后 |
| **`AdminIpBootstrap secret=`** | 仅内存 salt+hash | **认领当前管理 IP**（wall unlock / bootstrap API） | **管理白名单为空**时（与是否已开 `adminIpEnable` 无关；开启校验后才真正挡门禁） |

**管理员口令：新库 vs 老库**

| 场景 | 行为 |
|------|------|
| **全新部署**（库中尚无 admin 用户） | 随机 **20 位**口令；日志打印一次 + 写入 `admin-bootstrap.credentials`；登录后请改密（或横幅「已知晓并保留」） |
| **已配置** `system.password` / Env | 使用配置口令；**不**写恢复文件、**不**置引导待办 |
| **老库已有 admin** | **不会**再生成随机口令；沿用库中哈希。历史若仍为旧口令（含曾默认的 `admin`），登录校验**双读兼容**（旧 SHA-256×16 可验并惰性升级 PBKDF2）；框架**不再**把新装默认设回 `admin/admin` |

丢失找回、运维 Token `reveal`、多节点挂载：见 [`docs/AI_SECURITY_HARDENING_3_0_COMPAT.md`](docs/AI_SECURITY_HARDENING_3_0_COMPAT.md) §2.2.1。

---

## 管理员登录与访问分层

**运维证书不能代替后台登录。** 证书只负责「进门」（管理 IP / 门禁开关）；日常菜单与 gen CRUD 仍须 **用户名密码**（且在已授权的管理 IP 下）。

| 层 | 含义 |
|----|------|
| **L0** | Shiro 登录会话 |
| **L1** | `@RequiresPermissions` 等菜单/按钮权限 |
| **L2** | 系统管理员 +（当 `adminIpEnable=true` 时）客户端 IP ∈ 管理 IP 白名单 |
| **L3** | 运维页 `gateView` / `@PageAware(admin=true)` 等页面门禁 |

| 项 | 新装默认 | 生产建议 |
|----|----------|----------|
| 账号 | 用户名默认 `admin`；**口令见启动日志 / credentials 文件** | 登录后立刻改密；按角色拆分账号 |
| 管理 IP | `adminIpEnable` 默认关 | 证书入驻后会自动开启；或在 wall 手动开启 |
| clearToken | `SECURITY_OPS_CONFIG` 未配置则危险写全拒 | 由 **Web 超管**在安全配置首次写入（≥16）；超然信仅可轮换 |

深入：[`docs/AI_ADMIN_ACCESS_MATRIX.md`](docs/AI_ADMIN_ACCESS_MATRIX.md) · [`docs/AI_SECURITY_HARDENING_3_0_COMPAT.md`](docs/AI_SECURITY_HARDENING_3_0_COMPAT.md)（3.x）。

---

## 管理 IP

| 项 | 说明 |
|----|------|
| **开关真源** | `sys_config` → **`SECURITY_OPS_CONFIG.adminIpEnable`**（**默认 false**；与证书配置 `SECURITY_OPS_CERT_CONFIG` **不是同一份**） |
| **开启后** | gen / 管理类 API 须 **超管** 且 IP ∈ **`wall_ip_admin_white`**（与用户防拉黑表 `wall_ip_white` **分表**） |
| **开启时** | 自动认领**当前访问 IP** 进入管理白名单 |
| **关闭时** | 必须提供 **`SECURITY_OPS_CONFIG.clearToken`** |
| **操作入口** | 超然信运维 Gate，或后台 **运维监控 → 防火墙**（`wall.html`）「管理IP校验」 |
| **wall 锁页** | `adminIpEnable=true` 且当前 IP 未加白时：输入 **`AdminIpBootstrap secret=`**（名单仍空且引导未消费）或 **clearToken** → 认领当前 IP 后再操作 |
| **禁止** | 用用户白名单开关 `WallDefault.ipWhiteEnable` 顶替管理 IP 开关 |

业务仓非标准包接入：[`docs/AI_ADMIN_IP_BIZ.md`](docs/AI_ADMIN_IP_BIZ.md)（模式 A/B/C）。矩阵：[`docs/AI_ADMIN_ACCESS_MATRIX.md`](docs/AI_ADMIN_ACCESS_MATRIX.md)。

### Nginx / 多域名运维检查（必做）

| 项 | 说明 |
|----|------|
| **可信代理** | 配置 `autumn.client-ip.trusted-proxies`（或环境变量 `AUTUMN_CLIENT_IP_TRUSTED_PROXIES`）为 **紧挨应用的 Nginx/LB 对端**（单 IP 或窄 CIDR）。未配置时应用只用 `remoteAddr`，忽略 XFF。 |
| **覆盖 XFF** | Nginx：`proxy_set_header X-Forwarded-For $remote_addr;`（**覆盖**，勿用 `$proxy_add_x_forwarded_for`）。见 [`docs/HTTP_PROXY.md`](docs/HTTP_PROXY.md) §5。 |
| **验证** | 开启管理 IP 后认领的应是**真实客户端出口**，而非 `10.x` / `127.0.0.1` / LB 地址。 |
| **多域名 CORS** | 同站多个 Origin（子域/多绑定）须写入 `SECURITY_AUTH_CONFIG.corsAllowedOrigins`；Cookie 写请求带 `X-CSRF-Token`（`csrfEnforce` 默认开）。 |
| **clearToken** | 须由 **Web 超管**在安全配置首次写入（≥16）；运维 App 仅可轮换，不可首次铸造。 |
| **证书与超管隔离** | `/ops/api/v1` 管理面只认 `X-Ops-Session`；超管 Cookie alone → 401。见 [`docs/AI_OPS_CERT.md`](docs/AI_OPS_CERT.md) §4。 |

---

## 运维证书

通道前缀 **`/ops/api/v1`**（Shiro `anon`，但**真实鉴权在 Ops 服务层**；并**豁免**管理 IP 切面，以便 App 自救加白）。与业务传输加密 `/rsa/api/v1` **完全隔离**。集群各节点共用一份 **`SECURITY_OPS_CERT_CONFIG`**。公网部署须 **TLS**（管理 API body 多为明文 JSON）。

### 概念

- **双边 RSA**：服务端与客户端各自密钥对；**私钥永不上传**。
- **证书 = 进门（IP）**；**账号 = 进房间（身份）**——入驻成功后，浏览器日常管理仍用**用户名密码**登录。
- 入驻口令明文**只出现在服务器日志**，不落库、不进 API 响应。
- 服务端 RSA **不在启动时生成**；仅在 OpsEnroll 校验成功后「空则写」。

### 获取 Ops 秘钥（入驻口令）

1. 服务端**首次具备入驻条件**或**证书重置**后，在控制台 / 日志中查找 **`OpsEnroll secret=`**（粘贴到超然信）。见 [密钥一览](#首次启动控制台密钥一览)。
2. **勿与** `Admin bootstrap password`（后台登录）或 `AdminIpBootstrap secret=`（认领管理 IP）混淆。
3. App `probe`：`opsCert=pending` 且 `enrollAvailable=true` 时可入驻；站点不兼容会提示「未部署支持运维证书的 Autumn」。
4. 口令**一次性**：成功 `enroll` 后 hash 烧毁；需要新口令须走「重置证书」。

### 首次入驻（超然信）

1. 安装超然信 → 打开运维 Gate（扫码 `{base}/autumn/ops/discover`、粘贴站点 Base URL，或会话内 `AutumnOps` 消息）。
2. 粘贴 `OpsEnroll secret=` → 本地生成 RSA → `enroll` → 私钥按站点分目录存放。
3. 成功后服务端自动（幂等）：
   - `adminIpEnable=true`
   - 认领**当前访问 IP** 到管理白名单
   - **关闭**公开发现页（`discoverEnabled=false`；**不削弱** `probe`，仅浏览器 HTML 对外 404）
4. `challenge` → `session` 后可管理名单 / 门禁 / 用户黑白名单等。
5. 在该管理 IP 上用**账号密码**打开后台做日常配置。

### 添加管理 IP

- **超然信**（持证会话）：`/adminip/add`，或认领当前 IP。
- **后台**：运维监控 → 防火墙（须已在管理名单或先 unlock）。
- 删除**正在访问的当前 IP**（管理白名单或用户白名单）须 **clearToken**。

### 开关管理 IP 校验

- **开启**：App `/gate/setAdminIpEnable` 或 wall「管理IP校验」；开启时认领当前 IP。
- **关闭**：必须 **clearToken**（`enable=false`）。

### 清除令牌（clearToken）

存于 **`SECURITY_OPS_CONFIG.clearToken`**（与 `adminIpEnable` 同配置族）。

| 用途 | 说明 |
|------|------|
| 关管理 IP 校验 | 必须 |
| 删除「当前访问」管理/用户白名单 IP | 必须 |
| T4 重置运维证书 | **超管已登录** + clearToken；**不要**带 ops session |
| 设置 | 首次可直接设；已配置须 `oldClearToken`（或兼容字段） |
| 安全 | API 只返回 `clearTokenConfigured`；**永不下发明文**；未配置则一切依赖 clearToken 的危险写一律拒绝 |

### 重置证书

| 场景 | 做法 |
|------|------|
| **还能进 wall** | `POST /wall/api/opscert/reset`（超管），或超管 + clearToken 调 `POST /ops/api/v1/reset` → 日志**新** `OpsEnroll secret=` → 超然信对该站**重新入驻** |
| **IP 锁死且证书丢失** | DB 清空证书配置后**必须重启**（见下）→ 日志新 OpsEnroll → 再 enroll |

```sql
-- 按实际库表调整；仅重启不够：库中仍有 clients 时不会重打入驻口令
UPDATE sys_config SET param_value = '{}' WHERE param_key = 'SECURITY_OPS_CERT_CONFIG';
```

`reset` 会**重开**公开发现页。新口令**只进日志，不进 API body**。禁止用 `/rsa/api/v1` 或普通用户登录「补丁」恢复。

### 多设备

1. **推荐（独立密钥）**：已持证设备 `cert/issue` → 一次性 `claimSecret` → 新机本地 RSA → `cert/claim`（**各自 `certId`，可单独吊销**）。
2. **同证共享**：超然信会话发 **`AutumnOps`**（口令包裹同一客户端私钥；分享口令不进消息正文）。对端 Gate 导入则**共享同一 `certId`**。协议见并列仓 `../im/docs/IM_CLIENT_AUTUMN_OPS.md`。
3. 唯一设备丢失 / 全部吊销：走「重置证书」。

### 联调自检（摘录）

- [ ] 新库：日志有 `Admin bootstrap password generated`；能用该口令登录（非 `admin/admin`）
- [ ] 老库：已有 admin 时不重打引导口令；历史口令仍可登录并惰性升级哈希
- [ ] `OpsEnroll` / `AdminIpBootstrap` / 管理员引导口令三者不混用
- [ ] enroll 仅成功一次；响应**无私钥**；双节点 fingerprint 一致
- [ ] 入驻后 `adminIpEnable=true` 且当前 IP 在 `wall_ip_admin_white`
- [ ] 关管理 IP / 删当前 IP / reset 均须 clearToken
- [ ] `/autumn/ops/discover` 入驻后公网 404；登录后 `main` 仍可完整预览
- [ ] `cert/issue` → 他机 `claim` 得独立 `certId`；认领码不可复用
- [ ] `/rsa/api/v1` 与账号密码登录不受影响

完整协议与 40 条 API 矩阵：[`docs/AI_OPS_CERT.md`](docs/AI_OPS_CERT.md)。

---

## 后台功能与扩展菜单

以下为框架**默认种子菜单**（业务仓可增删改）。注册方式：`SysMenuService` 硬编码顶层 + 各模块 `*Menu`（`@Menu`）/ 页面 `*Site`（`@PageAware`）。规范见 [`docs/AI_STANDARDS.md`](docs/AI_STANDARDS.md) §14 · [`docs/AI_MAP.md`](docs/AI_MAP.md) §2.8A。

### 系统管理

| 页面（代表） | 说明 |
|--------------|------|
| 管理员 / 角色 / 菜单 / 部门 / 字典 | RBAC 与组织 |
| 参数管理 | `sys_config` |
| 站点门户 | 登录页品牌、备案、法律链接、**超然信公开页**配置（`siteportal.html`） |
| 系统日志 / 文件上传（OSS） | 审计与对象存储 |
| SQL 监控 | Druid |

### 运维监控

| 页面 | 说明 | 深入 |
|------|------|------|
| 缓存 / 队列 / 函数队列 / 线程池 | 运行时运维 | [`AI_MAP`](docs/AI_MAP.md) · [`AI_FUNCTION_QUEUE`](docs/AI_FUNCTION_QUEUE.md) |
| 数据库管理 / 全表查询 / 库备份入口 | DB 运维与只读开关 | [`AI_DATABASE_READ_ONLY`](docs/AI_DATABASE_READ_ONLY.md) |
| 定时任务（LoopJob） | 周期任务与分配 | [`AI_CLUSTER_JOB_ORCHESTRATION`](docs/AI_CLUSTER_JOB_ORCHESTRATION.md) |
| 集群节点 | 节点注册与角色 | [`AI_CLUSTER_NODE`](docs/AI_CLUSTER_NODE.md) |
| 防火墙（`wall.html`） | 管理 IP、clearToken、运维证书重置、用户黑白名单 | [`AI_OPS_CERT`](docs/AI_OPS_CERT.md) · [`AI_ADMIN_IP_BIZ`](docs/AI_ADMIN_IP_BIZ.md) |
| 会话 / Redis / 登录日志 / 日志级别 | 会话与中间件 | [`AI_SESSION_GUARD`](docs/AI_SESSION_GUARD.md) · [`REDIS_STANDALONE`](docs/REDIS_STANDALONE.md) |
| 字段加密 / 配置安全 | at-rest 加密与 `SECURITY_*` JSON | [`AI_FIELD_ENCRYPT`](docs/AI_FIELD_ENCRYPT.md) · [`加固 3.0`](docs/AI_SECURITY_HARDENING_3_0_COMPAT.md) |
| 人机检测 / Bean 执行器 / 重建表 / 版本更新 | 防护与运维工具（Bean/WebShell 默认关，见加固 C-21） | [`加固 3.0`](docs/AI_SECURITY_HARDENING_3_0_COMPAT.md) |
| **公开落地页** | 新窗口打开 `/autumn/ops/discover`（公网真实态；关闭时即为 404） | [`AI_OPS_CERT`](docs/AI_OPS_CERT.md) §3.0 |

### 授权与开放

| 菜单组 | 代表页 | 文档 |
|--------|--------|------|
| 授权登录 | 授权管理、授权范围 | [`AI_AUTH_LOGIN_MODES`](docs/AI_AUTH_LOGIN_MODES.md) · [`AI_OAUTH_INTEGRATION`](docs/AI_OAUTH_INTEGRATION.md) · [`AI_AUTH_SCOPE`](docs/AI_AUTH_SCOPE.md) |
| 客户端 | 接入管理（RP） | [`AI_OAUTH_INTEGRATION`](docs/AI_OAUTH_INTEGRATION.md) |
| 开放平台（OPL） | 统一管理 | [`AI_OPL_INTEGRATION`](docs/AI_OPL_INTEGRATION.md) · [`AI_OPL_SPI`](docs/AI_OPL_SPI.md) |
| 开放接入（OPC） | 统一管理 | [`AI_OPC_INTEGRATION`](docs/AI_OPC_INTEGRATION.md) |
| 功能授权 | Scope 定义 | [`AI_AUTH_SCOPE`](docs/AI_AUTH_SCOPE.md) |
| 扫码登录 | 授权页 / 票据 | [`AI_SCAN_LOGIN_STANDARD`](docs/AI_SCAN_LOGIN_STANDARD.md) · [`AI_QRC`](docs/AI_QRC.md) |

### 业务扩展模块（默认）

| 菜单组 | 模块包 | 文档 |
|--------|--------|------|
| 用户管理 | `usr` | 资料 / OpenId / 登录日志 |
| 防火墙（模块树） | `wall` | 与运维监控「防火墙」页配合；详见上表 |
| 机器人 | `bot` | [`AI_ROBOT`](docs/AI_ROBOT.md) · [`AI_ROBOT_API`](docs/AI_ROBOT_API.md) |
| 支付安全 | `safe` | [`AI_SAFE_CREDENTIAL`](docs/AI_SAFE_CREDENTIAL.md) |
| 任务管理 | `job` | cron / 分配 |
| 数据备份 | `db` | 策略与备份任务 |
| 代码生成 | `gen` | [`AI_CODEGEN`](docs/AI_CODEGEN.md) · [`AI_TEMPLATES`](docs/AI_TEMPLATES.md) |
| 国家语言 | `lan` | 多语言 |
| SPM | `spm` | 超级位置模型 / 访问日志 |

### 如何扩展后台菜单与页面

1. 页面：在 `*Site`（或生成 `*Pages`）字段上标 `@PageAware`；自定义逻辑写在 `*Site`，勿改 gen 页。
2. 菜单：`@Menu` + 继承 `ModuleMenu`，`init()` 写入 `SysMenuService`。
3. 管理页若需超管 + 管理 IP：`admin=true` / 纳入访问矩阵（见 `AI_ADMIN_ACCESS_MATRIX`）。

---

## 工程结构

```text
autumn/
├── autumn-handler/     # Handler 扩展接口与默认实现
├── autumn-lib/         # 缓存、队列、加解密、RuntimeSql、PageAware、FieldEncrypt 等
├── autumn-modules/     # 内置业务模块与后台 Controller / 模板
├── autumn-starter/     # 一站式依赖聚合
├── web/                # 启动入口（主类 cn.org.autumn.Web）
├── docs/               # AI_*.md 约束与专项文档
├── scripts/            # constraints-scan、dependency-scan 等
└── .cursor/skills/     # autumn-framework-2x / 3x
```

`autumn-modules` 主要包：`sys` · `gen` · `job` · `db` · `oauth` · `auth` · `client` · `usr` · `oss` · `lan` · `spm` · `wall` · `bot` · `safe` · `qrc` · `opl` · `opc` · `install` · `node` · `ops` · `support`。

运维客户端独立仓：[`autumn-app`](https://github.com/henryxm/autumn-app.git)。

---

## 核心能力一览

| 能力 | 说明 | 深入阅读 |
|------|------|----------|
| **模块与权限** | `sys / gen / job / db / oauth / usr / oss / lan / spm / wall / bot / safe / qrc / opl / opc …` | `docs/AI_MAP.md` |
| **管理 IP / 访问矩阵** | `SECURITY_OPS_CONFIG.adminIpEnable` + `wall_ip_admin_white`；L0–L3 | [`AI_ADMIN_ACCESS_MATRIX`](docs/AI_ADMIN_ACCESS_MATRIX.md) · [`AI_ADMIN_IP_BIZ`](docs/AI_ADMIN_IP_BIZ.md) |
| **运维证书** | `/ops/api/v1` · `SECURITY_OPS_CERT_CONFIG` · 超然信 | [`AI_OPS_CERT`](docs/AI_OPS_CERT.md) |
| **站点门户** | 登录品牌 / 备案 / 超然信下载配置 | [`AI_SITE_PORTAL`](docs/AI_SITE_PORTAL.md) |
| **集群节点** | 注册、角色、LoopJob 职责 | [`AI_CLUSTER_NODE`](docs/AI_CLUSTER_NODE.md) |
| **Service 继承链** | `ModuleService` → 缓存 / 队列 / 方言 / 自动 uuid | [`AI_STANDARDS`](docs/AI_STANDARDS.md) §2.1 |
| **两级缓存 / 队列 / 锁** | EhCache+Redis；Memory/Redis 队列；`withLock*` | [`AI_MAP`](docs/AI_MAP.md) · [`AI_DISTRIBUTED_LOCK`](docs/AI_DISTRIBUTED_LOCK.md) |
| **HTTP 混合加解密** | RSA 握手 + AES 会话（≠ 运维证） | [`AI_CRYPTO`](docs/AI_CRYPTO.md) |
| **字段存储加密** | `@FieldEncrypt` · `EncryptModuleService` | [`AI_FIELD_ENCRYPT`](docs/AI_FIELD_ENCRYPT.md) |
| **定时任务** | `LoopJob` + cron | [`AI_MAP`](docs/AI_MAP.md) §2.5 |
| **代码生成** | 可替换产物；业务写 Service | [`AI_STANDARDS`](docs/AI_STANDARDS.md) §11 · [`AI_CODEGEN`](docs/AI_CODEGEN.md) |
| **多数据库** | `RuntimeSql` / Provider | [`AI_DATABASE`](docs/AI_DATABASE.md) |
| **机器人 / 支付安全 / 扫码** | `/robot` · `/safe` · QRC | [`AI_ROBOT`](docs/AI_ROBOT.md) · [`AI_SAFE_CREDENTIAL`](docs/AI_SAFE_CREDENTIAL.md) · [`AI_SCAN_LOGIN_STANDARD`](docs/AI_SCAN_LOGIN_STANDARD.md) |
| **账号认证 / 登录跳转** | `ACCOUNT_AUTH_CONFIG` · `postLoginRedirect` | [`AI_ACCOUNT_AUTH_CONFIG`](docs/AI_ACCOUNT_AUTH_CONFIG.md) |
| **Handler 扩展** | `autumn-handler` 条件注入 | 在线文档 `handler` 章节 |

**三套密钥互不混用**：传输层 `AI_CRYPTO`（`/rsa`）· 字段 at-rest `AI_FIELD_ENCRYPT` · 运维证 `AI_OPS_CERT`（`/ops`）。门禁开关与 clearToken 在 **`SECURITY_OPS_CONFIG`**；证书材料在 **`SECURITY_OPS_CERT_CONFIG`**。

---

## 字段存储加密（摘要）

面向库内 `String` 字段 at-rest 加密、业务层仍用明文的场景。

1. 实体字段标注 `@FieldEncrypt`（可选 `searchable` + `{field}Hash` 列）。
2. Service 继承 **`EncryptModuleService`**（无加密字段继续用 `ModuleService`）。
3. 配置 `autumn.crypto.field.*`（`enabled` / `key` / `hash-key` / `prefix`）；管理页 **运维监控 → 字段加密**（`fieldencrypt.html`）。

手写 `baseMapper` SQL 须自行 `onWrite` / `afterRead*`。完整路径、缓存钩子与迁移：[`docs/AI_FIELD_ENCRYPT.md`](docs/AI_FIELD_ENCRYPT.md)。

---

## Service 默认继承链

```text
ModuleService
  → BaseService          # 分页、getCondition、菜单
  → DistributedService   # withLock*
  → ShareCacheService    # 跨模块共享缓存
  → BaseCacheService     # @Cache 回源与失效
  → BaseQueueService     # 队列注册与发送
  → AutoIdService        # UuidBased / SnowBased 第二主键
  → CompatibleService    # MP2 风格 select* 兼容层
  → DialectService       # RuntimeSql / columnInWrapper（MP3 ServiceImpl）
```

含 `@FieldEncrypt` 的实体：将 `ModuleService` 换为 **`EncryptModuleService`**，其余链不变。

---

## 快速启动

步骤与 [路径 A](#路径-a--跑通本仓演示应用-web) 相同，默认值如下：

| 项 | 默认值 |
|----|--------|
| 端口 | `80` |
| 管理后台 | http://localhost/ |
| 在线文档 | http://localhost/modules/docs/index |
| OpenAPI（SpringDoc） | http://localhost/swagger-ui.html |
| 超管用户名 | 默认 `admin`（可用 Env 改） |
| 超管口令 | **新库**：启动日志 / `admin-bootstrap.credentials` 中的随机口令；**老库**：沿用库内已有哈希；**已配** `system.password` 则以配置为准 |

```bash
mvn clean package -DskipTests
# IDE 运行 web/src/main/java/cn/org/autumn/Web.java
# 或 java -jar web/target/web.jar
```

**安全提示**

- 启动后立刻保存三类日志密钥（见 [密钥一览](#首次启动控制台密钥一览)）；**勿**默认尝试 `admin/admin`（新装无效）。
- 开发可先直登；**生产 profile 勿叠 `dev`/`h2`/`sqlite`**（`Config.isDev()` 会短路管理 IP 校验）。
- `OpsEnroll` / `AdminIpBootstrap` / 引导口令 / clearToken **勿**写入仓库或长期工单。
- 生产建议：`autumn.redis.open=true`、`autumn.shiro.redis=true`、**HTTPS**；改超管密码；超然信入驻证书；配置 **clearToken**；确认公网 `/autumn/ops/discover` 是否仍需开放。
- 多节点：共享或统一 `system.password`；引导口令文件在**创建 admin 的那台** `node.home`（见加固 §2.2.1）。
- 多节点定时任务配合 `LoopJob` 的 `assignTag` / `server.tag`。

---

## 文档入口

### 应用内在线文档

| 入口 | 路径 |
|------|------|
| 文档首页 | `/modules/docs/index` |
| 推荐顺序 | quickstart → architecture → ai-collab → handler → sys → cache / queue / job / oauth / hybrid-crypto |
| 模板源码 | `autumn-modules/src/main/resources/templates/modules/docs/` |

### 仓库 `docs/`（AI 与研发约束）

**所有 `AI_*.md` 均在 `docs/` 目录。**

| 第一步 | 文件 |
|--------|------|
| 索引 | [`docs/AI_INDEX.md`](docs/AI_INDEX.md) |
| 最小上下文 | [`docs/AI_BOOT.md`](docs/AI_BOOT.md) |
| 能力地图 | [`docs/AI_MAP.md`](docs/AI_MAP.md) |
| 强制规范 | [`docs/AI_STANDARDS.md`](docs/AI_STANDARDS.md) |

**按场景追加**（完整矩阵见 `AI_INDEX.md` §2）：

| 场景 | 文档 |
|------|------|
| **运维证书 / 超然信 / clearToken / 重置** | [`AI_OPS_CERT`](docs/AI_OPS_CERT.md) · [`AI_ADMIN_ACCESS_MATRIX`](docs/AI_ADMIN_ACCESS_MATRIX.md) · [`AI_ADMIN_IP_BIZ`](docs/AI_ADMIN_IP_BIZ.md) · [`AI_SITE_PORTAL`](docs/AI_SITE_PORTAL.md) |
| **管理员引导口令 / 新装随机密码** | README「密钥一览」+ [`加固 §2.2.1`](docs/AI_SECURITY_HARDENING_3_0_COMPAT.md) |
| 管理 IP / 业务仓门禁 | 同上矩阵 + [`加固 C-20`](docs/AI_SECURITY_HARDENING_3_0_COMPAT.md) |
| 站点门户 / 超然信下载配置 | [`AI_SITE_PORTAL`](docs/AI_SITE_PORTAL.md) |
| 集群节点 | [`AI_CLUSTER_NODE`](docs/AI_CLUSTER_NODE.md) |
| 字段存储加密 | [`AI_FIELD_ENCRYPT`](docs/AI_FIELD_ENCRYPT.md) |
| HTTP 加解密 | [`AI_CRYPTO`](docs/AI_CRYPTO.md) |
| 新模块 / 代码生成 | [`AI_CODEGEN`](docs/AI_CODEGEN.md) · [`AI_TEMPLATES`](docs/AI_TEMPLATES.md) |
| 机器人 / 支付安全 / 扫码 | [`AI_ROBOT`](docs/AI_ROBOT.md) · [`AI_SAFE_CREDENTIAL`](docs/AI_SAFE_CREDENTIAL.md) · [`AI_SCAN_LOGIN_STANDARD`](docs/AI_SCAN_LOGIN_STANDARD.md) |
| 升级 autumn | [`AI_UPGRADE`](docs/AI_UPGRADE.md) |
| 3.x 安全加固兼容 | [`AI_SECURITY_HARDENING_3_0_COMPAT`](docs/AI_SECURITY_HARDENING_3_0_COMPAT.md) |

**Cursor / 业务仓引用**：本仓库 `@docs/AI_INDEX.md`；并列业务仓 `@../autumn/docs/AI_BOOT.md`。

---

## AI 协作与 Cursor Skill

| Skill | 适用 | 路径 |
|-------|------|------|
| **autumn-framework-2x** | JDK 8 · Boot 2.7 · MP2 · master | `.cursor/skills/autumn-framework-2x/SKILL.md` |
| **autumn-framework-3x** | JDK 17+ · Boot 3.5 · MP3 · **`3.0.0`** | `.cursor/skills/autumn-framework-3x/SKILL.md` |

约束扫描（按需）：`bash scripts/constraints-scan`；PR 硬门禁：`bash scripts/check-java-fqn`（见 `AI_CODE_STYLE.md` §7.1）。

---

## 环境与构建

| 项 | 要求（**3.0.0 分支**） |
|----|------------------------|
| JDK | **17+** |
| Maven | 3.8+ |
| MySQL | 5.7+（建议 8.x） |
| Redis | 建议开启（缓存、Shiro 会话、队列、集群字段加密开关等） |

父 POM 在 **`validate`** 阶段启用 **`dependencyConvergence`**；升级依赖后若 enforcer 报错，请在 **`dependencyManagement`** 中对齐版本。构建命令见「快速启动」。

---

## 开发纪律（摘要）

完整条文见 **`docs/AI_STANDARDS.md`**。高频项：

- **分层**：Controller 禁止注入 Dao；Service 通过 `baseMapper` 访问本实体；跨域走对方 Service。
- **SQL**：Dao 仅 **Provider**（`*DaoSql extends RuntimeSql`）；条件列用 **`columnInWrapper`**。
- **代码生成**：`controller/gen`、`Pages/*.html/js` **勿手改**；业务写在非 gen 层。
- **双主键**：关联与对外标识用 **`uuid`**（`docs/AI_DUAL_KEY.md`）。
- **定时任务**：固定周期优先 **`LoopJob.*`**。
- **字段加密**：`@FieldEncrypt` → **`EncryptModuleService`**；手写 SQL → **`afterRead*`**。
- **管理访问**：新装从日志取引导口令（勿假定 `admin/admin`）；生产启用管理 IP；危险写配合 clearToken；证书通道勿与 `/rsa` 混用。

---

## 维护脚本

| 脚本 | 用途 |
|------|------|
| `scripts/constraints-scan` | 规范分组扫描（按需） |
| `scripts/autumn-dependency-scan.sh` | 依赖方升级 autumn 时的只读体检（见 `AI_UPGRADE.md` §4） |

---

## 链接

- 项目站点：http://autumn.org.cn
- 超然信官网：https://chaoran.xin/
- 运维客户端：https://github.com/henryxm/autumn-app.git
- 文档总索引：[`docs/AI_INDEX.md`](docs/AI_INDEX.md)
