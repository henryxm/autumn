# Autumn 3.0.0 安全加固兼容性与回滚手册

> **适用范围**：分支 `3.0.0` / 工件 `cn.org.autumn:*:3.0.0` 上「安全全量加固（1B + P0→P2）」改动。  
> **目标**：业务仓与历史部署在升级框架时，能按本手册做**回归清单**、**开关兼容**与**出问题回滚**。  
> **关联**：加固动机见对话审计；传输加密见 `docs/AI_CRYPTO.md`；会话守卫见 `docs/AI_SESSION_GUARD.md`；HTTP 代理见 `docs/HTTP_PROXY.md`；攻防演练见 `docs/AI_SECURITY.md`。

---

## 0. 兼容总原则

| 原则 | 说明 |
|------|------|
| API 形状优先 | 对外路径、Cookie 名（`rememberMe` / `autumnid`）、Token 头名、OAuth/Bot 路径尽量不变 |
| 数据双读 / 惰性升级 | 口令、用户 API Token 等：**旧数据可读**，新写入用更强算法，避免强制全表迁移 |
| 行为收紧用配置回退 | 多数破坏性默认值可通过配置恢复「旧宽松行为」，便于灰度与回滚 |
| 密钥轮换不可逆于 Cookie | RememberMe 密钥一旦更换，**旧 RememberMe Cookie 必然失效**（设计可接受） |
| 回滚代码 ≠ 回滚数据 | 口令/Token 若已惰性写成新格式，**只回退 JAR 而不回退库**可能导致登录/鉴权失败 |

---

## 1. 变更总表（按兼容影响分级）

### 1.1 破坏性 / 需主动适配（回归必测）

| ID | 变更 | 历史行为 | 现行为 | 受影响方 | 兼容开关 / 缓解 |
|----|------|----------|--------|----------|-----------------|
| **C-01** | HTTP 代理出站白名单 | 任意 `target`/`X-Target-URL` 可转发 | 白名单为空则**全部拒绝**；拒私网/元数据 | AI 代理客户端、网关转发 | SysConfig `SECURITY_HTTP_PROXY_CONFIG` |
| **C-02** | RememberMe AES 密钥 | 固定公开钥 `Z3Vucw…` | SysConfig / Redis SETNX / `{node.home}/security/remember-me.key`（旧 `config/` 路径自动迁移） | 已勾选「记住我」的用户 | 无兼容旧 Cookie；用户重新登录即可；集群须同一密钥源 |
| **C-03** | `userUuid` hint-only | 无 Token 时可凭 hint 冒充解析身份 | 默认**拒绝** | 依赖网关只传 `userUuid`、无 Token 的内部调用 | SysConfig `SECURITY_AUTH_CONFIG.allowUserUuidHint=true` |
| **C-04** | 默认管理员口令 | 未配置时 `admin`/`admin` | 未配置时**随机强口令**并打 info 日志（英文）+ 落盘恢复文件 | 首次安装/自动化装机脚本 | 配置 `systemPassword`（EnvBean） |
| **C-05** | Dev 空密登录 | 空用户名/密码回填 `admin` | 空则报错 | 本地习惯空 POST 登录的脚本 | 显式传账号密码 |
| **C-06** | `/plugin` / `/plugin.html` | Shiro anon + 无角色校验 | 走 `spm`；方法内超管 + **管理 IP 白名单**（`wall_ip_admin_white`） | 自动化插件加载 | 先登录超管 Session，且客户端 IP 已在管理白名单 |
| **C-20** | 后台门禁 / gen CRUD / Bean 执行 / wall 加白 | 有菜单即可调任意 gen 全表；`/wall/api/ipwhite/add` 无鉴权；exec 默认可调 | **`GenAdminAccessAspect`** 织入 `*..modules..controller..*` / `*..controller.gen..*`（不限 `cn.org.autumn`）；强制超管；**管理 IP** 受 `SECURITY_OPS_CONFIG.adminIpEnable` 控制（**默认关**，wall 可开；关须 clearToken）；`GEN_MODULES`+SPI `managedModules()`；`selfGenModules` 豁免；异形包见 **`docs/AI_ADMIN_IP_BIZ.md`**；**矩阵**见 **`docs/AI_ADMIN_ACCESS_MATRIX.md`**；install 不强制 IP；wall 临时密码认领 | 非超管调管理 API；`adminIpEnable=true` 时非白名单 IP | 业务仓按 AI_ADMIN_IP_BIZ 模式 A/B/C；遇风险开启管理 IP；`/sys/node/*` 须超管（+可选管理 IP） |
| **C-22** | `@ServerEndpoint` + 管理切面 | 无宽织入，JSR-356 端点通常不被 CGLIB | **`GenAdminAccessAspect` 曾织入 `modules..controller..*`**，对同包 `@ServerEndpoint`（如 bugshut）打 CGLIB 代理 → `ServerEndpointExporter` 注册代理类失败，启动崩溃 | 依赖 bugshut / 自研 `@ServerEndpoint` 且类在 `controller` 包下的壳工程 | 框架已 **`!@within(ServerEndpoint)`** 排除；判定侧亦豁免；业务端点勿用 `@RestController`，Exporter 注册前 `ClassUtils.getUserClass`；矩阵见 **AI_ADMIN_ACCESS_MATRIX** |
| **C-23** | gen 子类公开/集群 API | 子类扩展方法仅靠 Shiro anon/业务鉴权，不被 gen 模板门禁 | 曾对「继承 `*ControllerGen`」**整类**强制超管+管理 IP，误拦 `AppleCluster#notify/nodes`、`LanguageApi`、`LanguageController#put`、xsignw 公开读等 | 签名集群 Feign、语言注册 Feign、公开 `*Api`、业务 Controller 子类扩展接口 | 切面按**方法声明类**判定：gen CRUD / `@RequiresPermissions` / 框架 `GEN_MODULES` 子类运维口仍强制；业务模块子类无注解方法与 `*Api` 不连坐；`LanguageController#put` 方法豁免；对照表见 **[`AI_ADMIN_ACCESS_MATRIX.md`](AI_ADMIN_ACCESS_MATRIX.md)** |
| **C-21** | WebShell（`/webshell` `/shell` `/sftp`） | 常默认可连、握手弱鉴权 | **默认关闭**（`SECURITY_OPS_CONFIG.webShellEnabled=false` + `autumn.webshell.enabled=false`）；开启后须超管（`adminIpEnable=true` 时另须管理 IP）+主机白名单 | 依赖 `web-shell` 的现网升级后终端不可用 | 显式双开开关并配置 `allowed-hosts`；禁止 Shiro anon |
| **C-07** | `/actuator/**` | Shiro anon | 走 `spm`（需登录） | 监控探活若直打 actuator | 改用网关鉴权或登录；health 暴露见 `management.endpoints` |
| **C-08** | clear/reinit 头认证 | 固定头 `application/client-clear` 等 | **必须**在 SysConfig 配置高熵 ops Token，未配置则拒绝 | 运维脚本 | `SECURITY_OPS_CONFIG.clearToken` / `reinitToken` |
| **C-09** | CORS 反射 Origin | 任意 Origin + Credentials | 仅同源或 SysConfig CORS 白名单 | 跨域带 Cookie 的前端 | `SECURITY_AUTH_CONFIG.corsAllowedOrigins` |
| **C-10** | DataFilter | 切面实际未写入过滤条件 | 非超管写入 `sql_filter` | 依赖「非超管看全表」的后台列表 | 无开关；属安全正确行为，需数据权限回归 |
| **C-11** | `rbt_` Shiro 授权 | 机器人 Subject 继承**主人**菜单权限 | 仅 `bot:api`，不继承主人 | 误用 `rbt_` 调管理端 `@RequiresPermissions` | 管理端改用用户 Token/Session |
| **C-12** | 超级密码 | 配置 ≥20 位即可登录任意账号 | 默认 **`SECURITY_AUTH_CONFIG.superPasswordEnabled=false` 忽略** | 运维万能密码流程 | 开关 true 且 `SUPER_PASSWORD` ≥20 位 |
| **C-13** | OAuth Access Token 生成 | `MD5Generator` | CSPRNG 不透明串 | 若客户端校验 token「像 MD5」 | 一般无影响；勿假设 token 格式 |
| **C-14** | 上传/表单体积 | `-1` 不限制 | 默认 **100MB** | 超大文件上传业务 | 调大 `spring.servlet.multipart.*` / Tomcat 限制 |
| **C-15** | FreeMarker HTML 自动转义 | 默认不 auto-escape；存量多用 `?html` | **已回退全局强制**：不设全局 `output_format=HTML`（与 `?html` 互斥会 ParseException） | 若单页要自动转义 | 该模板 `<#ftl output_format="HTML">` 且去掉 `?html`；或继续手写 `?html`（推荐存量） |
| **C-16** | CSRF（生产默认） | 无全局 CSRF | `SECURITY_AUTH_CONFIG.csrfEnforce`（prod 首次种子 true） | 管理端 Cookie 会话的 POST/PUT/DELETE | 带 `X-CSRF-Token`；或后台关 enforce |
| **C-17** | Cookie Secure（生产） | 未强制 Secure | prod：`remember-me`/`session` Secure=true | 纯 HTTP 访问生产 | HTTPS 或临时关 Secure |
| **C-18** | docker-compose | 弱口令、映射 3306/6379、profile=dev | 强制定义密码、默认不暴露 DB/Redis、profile=prod | 本地 compose 一键起 | 设置环境变量；调试可自行改 compose |
| **C-19** | 口令升级 vs 会话比对 | Session/`AuthorizationInterceptor` 用 Principal.password 与库值全等比对，不等则踢下线 | 以 **DB** 为准经 `SessionPasswordAlignSupport` 分类：SHA→`pbkdf2$` **同步**并 `forcePersist` Session；真改密仍踢；升级后 **`invalidateUserCacheCluster`**；登录先升级再 RememberMe；`preHandle` 全请求对齐（含 JSON） | 历史 SHA 用户首次登录后立刻「无效登录」掉线；多节点 cache 陈旧误踢 | 框架侧已修；业务勿再自行比对 password 列与 Session |

### 1.2 双读兼容（旧数据可用，新数据新格式）— 回滚需注意库状态

| ID | 变更 | 旧格式 | 新格式 | 登录/校验兼容 | 回滚风险 |
|----|------|--------|--------|---------------|----------|
| **D-01** | 登录口令哈希 | SHA-256 × 16 + `salt` 列 | `pbkdf2$iterations$saltB64$hashB64` | 旧口令可登录；成功后**惰性升级**为 PBKDF2；须防 C-19（Session 旧哈希误踢） | 已升级用户：回退旧代码后仍可走 SHA 路径失败 → **必须**保留双读代码或回写口令 |
| **D-02** | 用户 API Token | `usr_user_token.token` 明文 | `sha256:` + hex（pepper 固定） | `getToken`：先按哈希查，再按明文查 | 新签发 Token 仅库中有哈希；回退到「只查明文」的旧代码 → **新 Token 全部失效** |

### 1.3 行为增强但路径兼容（建议回归，通常无需改业务代码）

| ID | 变更 | 说明 |
|----|------|------|
| **B-01** | 登录 Session Fixation | `subject.login` 后换发 SessionId，Cookie 名不变 |
| **B-02** | QRC Webhook SSRF | 拒私网/元数据；公网 URL 仍可用 |
| **B-03** | nonce 防重放 | `putIfAbsent` + 可选 Redis；签名协议不变 |
| **B-04** | `@Login` 拦截器 | 校验失败改为抛错，不再吞异常放行 |
| **B-05** | `sidx` ORDER BY | 非法排序列被丢弃（列表默认序），不再注入 |
| **B-06** | Druid | prod 默认关控制台；dev 需登录 |
| **B-07** | `org.json` 版本 | 强制 ≥20240303，覆盖 Oltu 传递依赖 |

---

## 2. 密码与凭证（重点）

### 2.1 登录口令（sys_user.password）

**写入路径（新）**

- `SysUserService.save` / `updateNoRole`（改密）/ `changePassword` / `resetPassword` → `ShiroUtils.hashPassword` → `PasswordHashSupport.hashNew`（PBKDF2-HMAC-SHA256，约 12 万次迭代）。

**校验路径（双读）**

- `NamedHashedCredentialsMatcher`：若存储串以 `pbkdf2$` 开头 → PBKDF2 校验；否则 → 原 Shiro SHA-256×16 + `salt`。
- `ShiroUtils.passwordsMatch` / 支付重置 `LoginPasswordPayResetVerifier` 同步双读。

**惰性升级（原子、防污染）**

- 开关：SysConfig `SECURITY_AUTH_CONFIG.passwordUpgradeOnLogin`（默认 `true`）。
- 仅 `login(username, password, …)` 且**未**走超级密码 / `SuperPasswordToken` 时触发。
- 明文来源：登录表单参数 `userSubmittedPassword`（锁定变量），**禁止**用 Token 内空串或其它字段。
- 门禁：从 DB **重新加载**行 → `PasswordHashSupport.matchesLegacySha256(用户明文, 库password, salt)` 必须通过 → 用**同一明文**生成 PBKDF2 → **自检** `matchesPbkdf2` → `UPDATE … SET password=新 WHERE uuid=? AND password=旧`（CAS，影响行数须为 1）。
- 失败（校验不过 / CAS 未命中 / 自检失败）：**不改库**，登录仍成功（仍用旧 SHA）。
- 成功后（及 CAS 未命中但库已是 PBKDF2 时）以库为准写回 Subject；**`invalidateUserCacheCluster`**（非仅本机 `clear(uuid)`）；**`RedisShiroSessionDAO#forcePersist`** 立即回写 Session。
- **升级须在 `applySessionRememberMe` 之前**，避免 Redis/RememberMe 仍序列化旧 SHA。
- 防御（**C-19**）：`SysUserService#alignSessionPasswordWithStore` + `SessionPasswordAlignSupport`；`AuthorizationInterceptor#preHandle` 对已登录请求对齐；两侧同为 PBKDF2 却不等 → 真改密踢下线。
- 非口令建会话（`login(AuthenticationToken)` / OAuth / 扫码等）**不**触发惰性升级（无用户明文）；遗留 SHA 可长期保留直至下次密码登录。
- 实现：`LegacyPasswordUpgradeSupport.tryUpgradeAtomically`。

**对业务仓的影响**

- 登录 API（`/sys/login` 等）请求/响应形状**不变**。
- **禁止**业务代码自行 `ShiroUtils.sha256(plain, salt)` 后与 `user.password` 做「仅 SHA」比较；应改用 `ShiroUtils.passwordsMatch` 或走统一登录。
- 支付密码 / 手势等（`PayCredentialSupport`）**仍用** `ShiroUtils.sha256`，与登录口令算法**独立**，本轮未改存储格式。

**回归用例**

1. 历史用户（库中仍为 SHA hex）用正确密码登录 → 成功，且再次查库 password 变为 `pbkdf2$…`。  
2. 新注册/重置密码用户 → 库中即为 `pbkdf2$…`，登录成功。  
3. 错误密码 → 失败信息与历史一致（不泄露算法）。  
4. 支付密码重置用登录密码校验 → 遗留 SHA 与已升级 PBKDF2 用户均可通过。  
5. 历史 SHA 用户登录成功后**连续访问管理页 / JSON API** → 不得因 C-19 被踢。  
6. **双节点**：节点 A 密码登录完成升级 → 节点 B 立刻打开管理页 → 不得因进程 cache 旧 SHA 误踢。  
7. 管理员重置密码后，旧 Session 仍应被踢（确认未误伤真改密）。

### 2.1.0 密码登录失败排查（account Web / App）

**典型文案区分**

| 来源 | 文案 | 含义 |
|------|------|------|
| account AJAX | `登录失败，请重试！` | 兜底；优先查日志 `AJAX登录*` / RSA / Redis；修复后应更常看到真实 `账号或密码不正确` 等 |
| account App | `密码错误` | App 口令校验失败（须双读 `ShiroUtils.passwordsMatch`，禁 SHA 单读） |
| 框架 Shiro | `账号或密码不正确` / `账户验证失败` | `AuthorizationController.loginPost` |
| 拦截器 | `权限验证失败` | `AuthorizationInterceptor` 非 `AException` 路径；日志含 `type=` 根因 |

**与「重部署后短暂可登录」相关**

1. AJAX 曾只读 `model.error`，真实错误在 redirect `?error=` → 误报「请重试」（已修：写 model + 解析 redirect）。
2. `SysUserService.sync` 曾用 Session 主体 `password` 回写库（hashCode 含口令）→ 旧 SHA 盖掉 `pbkdf2$`（已修：update 前保留库中 password/salt）。
3. 登录后 SessionFixation/Redis 抛错被 AJAX 吞掉（已修：认证成功后 regenerate 失败只 warn；AJAX 无 error 且已登录视为成功）。
4. 验证码/第三方勿再用 `getSuperPassword()` 走用户哈希比对（已修：`loginTrusted` + `SuperPasswordToken`）。
5. App API 须 `passwordsMatch` 双读。

**运维检查**：`sys_user.password` 列长 ≥100；RSA Redis `account:uniform:rsa:priv:*`；集群 Session 与 DB 口令是否一致。

### 2.1.1 登录口令变更审计（sys_password_change_log）

**目的**：回答「谁、何时、从哪改了谁的口令」；支持「新口令是否用过」校验。不替代 `sys_log`，也不记录登录成败。

**表与字段（实体驱动建表）**

| 字段 | 说明 |
|------|------|
| `targetUserUuid` | 被改口令用户 |
| `operatorUserUuid` | 操作者（自助=本人；管理端=当前登录；匿名/系统可空） |
| `action` | `CHANGE` / `RESET` / `ADMIN_SET` / `CREATE` / `CLEAR` / `UPGRADE` |
| `channel` | `SELF` / `ADMIN` / `FORGOT` / `REGISTER` / `BOOTSTRAP` / `OAUTH_PROVISION` / `SYSTEM` |
| `previousPasswordHash` | **变更前** `sys_user.password`（SHA hex 或 `pbkdf2$…`）；`CREATE` 可空 |
| `previousSalt` | 变更前 `sys_user.salt`（遗留 SHA 校验用；PBKDF2 盐在哈希串内，可空） |
| `success` / `ip` / `userAgent` / `remark` / `createTime` | 结果与客户端元数据 |

**统一入口**：`PasswordChangeLogService#record`；审计失败仅 `warn`，**不回滚**改密主流程。写库路径挂钩：`changePassword` / `resetPassword` / `updateNoRole`（实际改密）/ `save`（建号设口令）/ `cancel`（清空）/ 登录惰性升级 CAS 成功。

**明文禁忌与接口门禁**

- **禁止**写入或 API 返回口令明文；亦不存「新口令」明文（新口令只在 `sys_user` 哈希中）。
- `GET /sys/passwordlog/list`：**不对普通用户开放**；门禁为**系统管理员 + 管理 IP 白名单**（`AdminAccessGuard.requireOpsAdmin`），不挂可分配菜单权限，避免角色误授权。响应为 `PasswordChangeLogView`，**剥离** `previousPasswordHash` / `previousSalt`（仅元数据：操作者/目标/动作/渠道/IP/UA/时间）。
- 无 gen CRUD / 无自助查询接口；防重用匹配仅在服务端 `matchesHistory` 使用哈希，不对外暴露。

**历史口令 / 防重用**

- 配置：`SECURITY_AUTH_CONFIG.passwordReuseAllowed`（默认 **`true`**，允许复用当前/历史口令）。管理端安全认证 JSON 可改；种子亦可读 Env `autumn.password.reuse-allowed`。存量库 JSON 缺字段时 Gson 反序列化仍回落字段默认 `true`；升级后建议 `refreshJson` 合并新键。
- `matchesHistory(userUuid, plain, limit)`：按时间倒序取最近 N 条**含 previousHash** 的记录（方言安全 `page`），用 `PasswordHashSupport` 双读校验（不可从哈希还原明文）。
- 作用范围：自助 `changePassword` 与 `resetPassword`（含忘记密码）在 **`passwordReuseAllowed=false`** 时 `assertNotRecentPassword`（当前口令 + 最近 **5** 条历史）；审计服务缺失则 **fail-closed**。管理端 `updateNoRole`（`ADMIN_SET`）**不受**此约束。默认允许复用时跳过校验；审计仍始终记录 `previousPasswordHash`。
- `resetPassword` 默认 channel=`SYSTEM`；忘记密码入口经 `PasswordChangeAuditContext` 设为 `FORGOT`。审计行仅在写库成功后落库。

### 2.2 超级密码（紧急通道）

- 配置项：`SUPER_PASSWORD` / EnvBean `supperPassword` 仍存在。  
- **新增**：`SECURITY_AUTH_CONFIG.superPasswordEnabled`（默认 `false`）。  
- 启用条件：开关 true **且** `SUPER_PASSWORD` 长度 ≥20。  
- **用途**：紧急登录任意账号（含忘记 admin 口令后改密）；**不是**日常管理员初始口令替代。  
- **收紧**：未配置或过短时 `getSuperPassword()` 返回空，**不再**回落进程内临时 uuid（避免误以为存在可用超级密码）。  
- 设置建议：先在后台写入 ≥20 位超级密码，再打开开关；清空或关开关后不可用直至重新配置。  
- 与引导态：用超级密码登录**不会**自动清除 `adminBootstrapPending`。

### 2.2.1 管理员引导口令（随机初始口令）

| 概念 | 说明 |
|------|------|
| 引导态口令 | 未配置 `system.password` / Env `systemPassword` 时，首次建 admin 生成的随机口令 |
| 配置口令 | Env/配置显式给定 → **不**写恢复文件、**不**置改密待办 |
| 改密待办 | `SECURITY_AUTH_CONFIG.adminBootstrapPending`；驱动管理端提示条 |

**生命周期**

1. **生成**：`AdminBootstrapPasswordSupport` 生成 20 位随机口令；哈希写入 admin；明文落盘 `{autumn.node.home}/security/admin-bootstrap.credentials`（权限尽力 `0600`）；`info` 英文日志打印路径与明文一次；`adminBootstrapPending=true`。  
2. **提示（软强制）**：登录成功 / `GET /sys/session/self` / `self/ping` 返回 `passwordReminder`；管理端壳顶部横幅「去修改 / 稍后提醒 / 已知晓并保留」。**不**因未改密 403。  
3. **稍后提醒**：会话属性 snooze 24h，横幅降为弱提示。  
4. **已知晓并保留**：清 pending、删恢复文件、记 `bootstrapAcknowledgedAt`（审计日志）。  
5. **改密/重置 admin**：同上清 pending 与文件，并踢其他会话。  

**丢失找回**

| 场景 | 处理 |
|------|------|
| 文件仍在、未改密 | 读 `admin-bootstrap.credentials` 或首次启动 info 日志 |
| 无 SSH | 运维 Token：`GET /sys/ops/admin-bootstrap/status` / `reveal`（头 `x-bootstrap-authentication` = clear 或 reinit Token；仅 pending 时 reveal 明文） |
| 文件与明文均失 | 无法从哈希还原 → 超管改密 / 开超级密码后改密 / 文档化重置 |
| 已配 `system.password` | 以 Env 为准，无引导文件 |
| 多节点 | 文件在**创建 admin 的那台** `node.home`；集群应共享卷或统一 `system.password` |

**明确不做**：明文长期进 DB/SysConfig；强制改密才能进系统；恢复 `admin/admin`；未开超级密码时用 temp uuid 冒充超级密码。

### 2.3 用户 API Token（usr_user_token）

- 创建：`createToken` / `createApiToken` 返回实体上的 `token` 字段仍为**明文一次**；落库为 `sha256:…`。  
- 查询：`getToken` / `queryByToken` 支持明文入参（先哈希再查，再回落明文行）。  
- OAuth `saveToken` 写入访问令牌时亦哈希化。  
- **回归**：旧明文行仍可鉴权；新 Token 创建后立刻用返回明文调 API；重启后再用同一明文仍可（库中哈希）。

### 2.4 机器人 `rbt_`

- 存储哈希逻辑未改算法；**授权**改为不继承主人菜单权限（C-11）。  
- `/bot/api/v1` 业务鉴权路径应不受影响；勿用 `rbt_` 调后台管理权限接口。

### 2.5 RememberMe

- Cookie 名仍为 `rememberMe`。  
- 密钥解析优先级（`RememberMeCipherKeySupport`）：
  1. SysConfig `SECURITY_SESSION_CONFIG.rememberMeCipherKey`（**集群推荐写此**）
  2. Redis 键 `{namespace}:system:shiro:remember-me:cipher-key`（SETNX，无 TTL）
  3. 本地文件默认 **`{autumn.node.home}/security/remember-me.key`**；旧 `config/remember-me.key` **自动迁移**
  4. 进程内临时密钥（仅兜底）
- **自愈**：删除本地 key 文件后重启 → 有 Redis 则从 Redis 回写文件；无 Redis 则重新生成（旧 Cookie 失效属预期）。
- **为何不按节点 uuid 分子目录**：uuid 写在 `{home}/node-profile.json` 内；RememberMe 密钥在集群内须按 **namespace 共享**，按 uuid 分叉会导致平行节点无法互解密。挂载整个 `autumn.node.home` 即可同时保留画像与密钥。
- 从公开默认钥升级后：旧 RememberMe Cookie **不兼容**，用户重登一次即可。
- **禁止**使用历史公开默认钥 `Z3VucwAAAAAAAAAAAAAAAA==`。

### 2.6 Redis / 单机 / Docker 多场景兼容性

#### 2.6.1 RememberMe 场景矩阵

| 场景 | Session | 密钥权威源 | 本地文件作用 | Docker 重启 | 删本地 key 后 |
|------|---------|------------|--------------|-------------|---------------|
| A. 单机，未开 Redis | 进程内/DB | **本地文件** | 权威 | 须挂载 `node.home`，否则丢钥 | 重生新钥，旧记住我失效 |
| B. 单机，开 Redis + shiro.redis | Redis | Redis SETNX + 本地缓存 | 引导/备份；冲突以 Redis 为准 | 挂载 home 或依赖 Redis | Redis 回写文件（自愈） |
| C. 集群多节点 + Redis | 共享 Redis | **Env 相同** 或 Redis SETNX | 每节点副本，须与 Redis 一致 | 各挂载各自 home；钥靠 Redis/Env | 从 Redis 回写 |
| D. 集群多节点但无 Redis | 粘滞/不一致 | 必须 **SysConfig 固定同一 cipher-key** | 无统一密钥时各写各的 → **不兼容** | 后台统一密钥 | 有 SysConfig 则回写；无则危险 |
| E. 仅清 Redis、保留本地文件 | — | 本地可再 SETNX 成为权威 | 恢复集群钥 | — | — |
| F. Redis 与本地都删且无 Env | — | 新随机钥 | 新文件 | — | 全部旧 Cookie 失效 |

#### 2.6.2 路径与挂载

| 项 | 值 |
|----|-----|
| 节点 home 默认 | `{user.home}/.autumn`（`autumn.node.home` / `AUTUMN_NODE_HOME`） |
| 画像文件 | `{home}/node-profile.json`（内含节点 uuid） |
| RememberMe 文件 | `{home}/security/remember-me.key` |
| 管理员引导口令文件 | `{home}/security/admin-bootstrap.credentials`（改密/确认后删除） |
| compose 示例 | `./.storage/web/autumn:/root/.autumn` + `AUTUMN_NODE_HOME=/root/.autumn` |
| 旧路径 | `config/remember-me.key`（相对 cwd，容器易丢）→ 启动时迁移到 home |

#### 2.6.3 其它加固项（集群视角）

| 加固项 | 开 Redis | 未开 Redis |
|--------|----------|------------|
| Shiro Session | ✅ 多节点共享 | ⚠️ 粘滞或不跨节点 |
| ForceLogout + RememberMe | ✅ | ⚠️ 标记不可跨节点共享 |
| 口令 PBKDF2 CAS / API Token | ✅ MySQL | ✅ MySQL |
| OAuth nonce | ✅ Redis | ⚠️ JVM 本地，多实例不共享 |
| CSRF（随 Session） | ✅ | ⚠️ 随 Session 局限 |
| CORS / 代理 SSRF / DataFilter / sidx | ✅ 配置一致即可 | ✅ |

**RememberMe 跨节点流程（场景 C）**

1. 节点 A 记住我登录 → 共享 AES 钥写 Cookie。  
2. 请求到节点 B → 同钥解密。  
3. 强制下线 → Redis force-logout，全集群拒绝 RememberMe。  
4. 仅本地文件分叉且未进 Redis/Env → B 解密失败（安全失败，非串号）。

---

## 3. 配置权威 vs 历史数据兼容

### 3.0 两层含义

| 层 | 策略 |
|----|------|
| **配置** | 运行时**只认**后台 SysConfig 四组 `json_type`；不依赖 Env/yml 运维 |
| **历史数据/协议** | 口令 SHA 双读+惰性升级、API Token 明文行双读、RememberMe Redis/文件自愈等**保留** |

### 3.1 SysConfig 四键（分类「安全加固」）

| 键 | 模型 | 主要内容 |
|----|------|----------|
| `SECURITY_AUTH_CONFIG` | `SecurityAuthConfig` | 超级密码开关、引导口令待办、口令升级、历史口令复用、hint、CSRF、CORS |
| `SECURITY_HTTP_PROXY_CONFIG` | `SecurityHttpProxyConfig` | 代理开关、DNS、白名单（空=拒全部） |
| `SECURITY_OPS_CONFIG` | `SecurityOpsConfig` | clear/reinit Token；Bean/WebShell/`adminIpEnable` |
| `SECURITY_SESSION_CONFIG` | `SecuritySessionConfig` | Cookie Secure/SameSite、RememberMe AES 密钥 |

首次 `put` 若库无行：可一次性吸收进程内仍存在的历史 `autumn.*` / 旧 Env（便于老部署升级不丢白名单）；**之后只改后台**。`prod` profile 下 CSRF 与 Cookie Secure 默认种子为 true。

**启动顺序**：`getSecurity*Config()` 在库尚无行时**回落同一套 seed**（非空 Java 默认），故 Shiro Cookie/RememberMe Bean 在 `InitFactory.put` 之前创建也能拿到 prod Secure / 历史 Env 密钥，与入库后一致；**改库中 Secure/SameSite/密钥后仍须滚动重启**（Cookie Bean 不热更新）。

### 3.2 仍留 yml 的节点/基础设施

`autumn.redis.open`、`autumn.shiro.redis`、`autumn.node.home`、`autumn.shiro.remember-me.key-file`、`CLUSTER_NAMESPACE` / `SUPER_PASSWORD`（后两者本就是 SysConfig 字符串项）。

### 3.3 集群原则

1. 安全开关与 ops Token：**只改 SysConfig**，禁止多节点各改文件。  
2. RememberMe 密钥：SysConfig → Redis SETNX → 本地文件；与 Redis 冲突时以 **Redis** 为准；改钥后清 Redis 键并全量重启。  
3. Cookie Secure/SameSite：改 SysConfig 后**滚动重启**。  
4. 无 Redis 多节点须粘滞或补 Redis。

---

## 4. 回归测试清单（建议按环境勾选）

### 4.1 账号与会话

- [ ] 遗留 SHA 用户登录成功并触发口令升级  
- [ ] PBKDF2 用户登录成功  
- [ ] 改密 / 重置密码 / 注册  
- [ ] 无 `system.password` 全新安装：引导文件 + info 日志；登录见横幅；改密后文件删除  
- [ ] 「已知晓并保留」关闭横幅并删文件；已配 `system.password` 无引导态  
- [ ] 超级密码关/短不可登；开且 ≥20 可紧急登  
- [ ] 记住我：升级后需重登；Secure/SameSite 在 HTTPS 下正常  
- [ ] 强制下线 + RememberMe 阻断（见 `AI_SESSION_GUARD.md`）  
- [ ] OAuth/OPL 回调建会话（`establishSession`）后 Cookie 可用  
- [ ] 扫码登录交换令牌登录  

### 4.2 开放 API

- [ ] 用户 API Token：旧明文行、新哈希行  
- [ ] Bot：`rbt_` 调 `/bot/api/v1`；确认管理端权限接口拒绝机器人 Subject  
- [ ] 无 Token 仅 `userUuid`：默认 401/空上下文；SysConfig `allowUserUuidHint=true` 时恢复  

### 4.3 代理与出站

- [ ] 未配白名单：代理返回 403  
- [ ] 配白名单：允许主机成功；`127.0.0.1` / `169.254.169.254` 拒绝  
- [ ] QRC Webhook 私网 URL 被跳过  

### 4.4 后台与运维

- [ ] 非超管列表数据范围（DataFilter）  
- [ ] 列表非法 `sidx` 不报 500、不注入  
- [ ] 插件 load/unload 无超管 → 403（`/plugin` 不再 anon；须超管 + 管理 IP 白名单）  
- [ ] actuator 未登录不可随意访问  
- [ ] clear/reinit：配置 Token 后成功；未配置失败  
- [ ] prod CSRF：管理 POST 带 `X-CSRF-Token`  
- [ ] **`SECURITY_AUTH_CONFIG.adminLockdown`**：默认 `false`（模式 A）。运维页与框架 GEN_MODULES 管理页无论开关均仅系统管理员；**任意业务 gen CRUD** 亦仅超管（见下）；自助 API（`/sys/session/self/**`、usr/safe/bot/qrc gen）始终不锁。`true` 时子项目自定义 `modules/**` 与非 gen 定制管理页也仅超管  
- [ ] **`SECURITY_OPS_CONFIG.beanExecuteEnabled`**：默认 `false`；开启后仍须超管，且 `adminIpEnable=true` 时须管理 IP 白名单，审计日志记账号/IP/Bean/方法  
- [ ] **`SECURITY_OPS_CONFIG.webShellEnabled`**：默认 `false`（与 `autumn.webshell.enabled` 取更严）；开启后仍须超管（+可选管理 IP）+ 主机白名单（C-21）  
- [ ] **`SECURITY_OPS_CONFIG.adminIpEnable`**：默认 `false`；`true` 时运维/gen/`checkAdmin` 验 `wall_ip_admin_white`；wall「管理IP校验」可开关；**关闭须 clearToken**；开启自动认领当前 IP  

- [ ] **`POST /wall/api/ipwhite/add`**（用户防拉黑名单）：未登录/非超管（及管理 IP 校验开启且未加白）→ 不写库；删当前 IP 须头 `x-clear-authentication` = clearToken  
- [ ] **管理 IP**：`wall_ip_admin_white` 与 `wall_ip_white` 表隔离；运维/gen/`requireOpsAdmin` 走 `IpAdminWhiteService.checkAdmin`（受 `adminIpEnable` 控制），不跟随 `WallDefault.ipWhiteEnable`；**不从用户白名单迁移/回退**，空表初始化；用户 `isWhite` **逻辑继承**管理名单（管理 IP 默认免防火墙拉黑）  
- [ ] **管理 IP 引导**：`adminIpEnable=true` 且空表时启动打印 `AdminIpBootstrap secret=`（明文一次）；内存只存 hash；`POST /wall/api/ipadminwhite/bootstrap` 认领当前 IP 后立即清 hash；二次同一密钥失败；名单非空后 bootstrap 拒绝；客户端 IP 解析与 `IPUtils.getIp` 统一（XFF 首 hop + trim）  
- [ ] **`wall.html`**：超管可进页；仅 `adminIpEnable=true` 且 IP 未加白时**不直接展示内容**，居中输入临时安全密码（引导态=启动密钥，否则=`clearToken`）→ `POST /wall/api/ipadminwhite/unlock` 认领当前 IP 后再显示；POST 带 CSRF；认领后增删管理 IP 须已在管理名单；用户白名单区块文案区分「防拉黑」
- [ ] **`/sys/node/*`（`ProfileController`）**：未登录/非超管（及管理 IP 校验开启且未加白）→ 403（`GEN_MODULES` 含 `node`，切面强制）
- [ ] **install 向导**：`autumn.install.mode=true` 时可无管理 IP；切面织入 install 包但不强制 IP（首装无白名单）  
- [ ] **`firewall.html`**：非超管不得改 `WallDefault`；`white=false` 须 clearToken  
- [ ] gen CRUD：非超管调 `*.controller.gen.*` → 403；`adminIpEnable=true` 时管理 IP 未加白亦 403；**usr/safe/bot/qrc**（及 SPI `selfGenModules`）不强制超管  
- [ ] 子项目 `modules/{biz}/**` **页面**：默认非超管可开（登录由 Spm）；`adminLockdown=true` 或 `@PageAware(admin=true)` 时仅超管  
- [ ] 普通用户自助：`/sys/session/self/list`、`self/ping`、`/sys/user/info|password` 仍 200（本人数据）  
- [ ] `/plugin/load|unload` 与 HTML `/plugin` 一致：超管（+可选管理 IP）  

### 4.4.1 依赖仓兼容矩阵（im / video / bighub / xsign-www）

> 扫描时点：业务仓依赖 Autumn **3.0.0**。框架侧 gen 切点已为 `*..controller.gen..*`（含 `cn.bighub`）；下表「框架已拦」与「业务仓待改」分开。

| 仓 | 首页 / 公开页 | 业务管理页 | gen CRUD API | 框架结论 |
|----|---------------|------------|--------------|----------|
| **xsign-www**（`xsignw`） | 公开 Site / `XSignWwwController`；证书与职位等自定义 anon | `login=true` 管理页默认兼容 | `xsignw` gen → 仅超管（切面） | 首页可用；gen 全表已锁 |
| **im**（`im/file/asr/…`） | 部分 `login=false`；客户端 `/im/api/v1` | 大量 `login=true`、无 `admin=true` | `im/file/asr/…` gen → 仅超管 | 客户端 API 不受 gen 切面影响 |
| **video**（`vshort`） | `/vshort/api/v1/**` Shiro anon + `@Authenticated` | `VshortPages` 等默认兼容 | `vshort` gen → 仅超管 | 同上 |
| **bighub**（`cn.bighub.modules.*`） | 门户 SPM；`/admin` 自拦超管 | 控制台默认兼容；**全库无** `admin=true` | `cn.bighub...controller.gen` → 超管+管理 IP（`*..modules..controller..*` 织入） | 手写管理模块用 SPI `managedModules()`；异形包见 **AI_ADMIN_IP_BIZ** |

**适配要点（框架约定）**

1. **公开首页 / 匿名 API**：`@PageAware(login=false)` 或可维护 Controller + Shiro anon；**禁止**把公开能力写进 `controller.gen`（否则非超管/匿名会 403）。  
2. **本人数据**：usr/safe/bot/qrc，或 `AdminAccessModuleHandler#selfGenModules()`（仍须权限与行级隔离）。  
3. **业务全表**：仅超管+管理 IP 用 gen；运营部分表 → 可维护 Controller + 数据范围。  
4. **管理页**：全表/运维向 Site 字段加 **`@PageAware(login = true, admin = true)`**。  
5. **非标准包前缀**：按 **`docs/AI_ADMIN_IP_BIZ.md`** 模式 A（标准包）/ B（`managedModules`）/ C（薄切面委托 `enforceAdminIpApiIfRequired`）。  
6. **整站高危**：可开 `adminLockdown=true`（会收紧全部未知 `modules/**`，含用户控制台页，慎用）。

**业务仓残留风险（框架不替代，升级后仍须业务侧整改）**

| 仓 | 残留点 | 建议 |
|----|--------|------|
| **xsign-www** | FilterChain 将 `/xsignw/xsignhomepage/**`、`/xsignw/xsignjobvacancypublish/**` 标 anon；Universal Link **写**接口、证书上传/兑换等落在公开面 | anon 仅保留真正公开读方法；写操作改登录/签名校验；**勿**对整个 gen 前缀 anon（gen CRUD 虽被切面拦，子类扩展方法不会） |
| **im** | 管理页无 `admin=true`；部分运维 REST（如 Zookeeper/Nacos/RobotChannel）未 `isSystemAdmin` | 管理页补 `admin=true`；运维 REST 对齐 `requireOpsAdmin` 或至少超管 |
| **video** | `/vshort/api/v1/**` 整段 Shiro anon，身份仅靠 `@Authenticated`（含 `notNull=false`） | 审漏注；敏感写接口禁止匿名 Subject |
| **bighub** | 大量 bare `@PageAware`（默认 `login=false`）含运维向 helps；管理页仅 `login=true` | helps 显式 `login=true`；管理页 `admin=true`；自助继续走 `usercenter` / `*/user/*` |

### 4.4.2 安全告警日志（`SecurityAlert`）

门禁拒绝与高危运维审计统一前缀，便于 SIEM / `grep` 发现提权探测：

| 类型 | 日志形态 | 说明 |
|------|----------|------|
| **拒绝** | `WARN … SecurityAlert denied reason=…, user=…, login=…, ip=…, uri=…, target=…, detail=…, ua=…` | 未授权 / 非超管 / IP 未加白 / Bean 执行关闭等 |
| **审计** | `INFO … SecurityAlert audit action=…, user=…, …` | 已通过门禁的高危动作（如 Bean 执行） |

**常用 `reason`（拒绝）**

| reason | 触发点 |
|--------|--------|
| `ops_admin_unauthenticated` / `ops_admin_forbidden` | `requireOpsAdmin`（exec/plugin/queue/wall API 等） |
| `system_admin_unauthenticated` / `system_admin_forbidden` | `requireSystemAdmin`；gen CRUD 的 `target=gen:…#method` |
| `admin_page_view` / `ops_view` / `gen_module_view` | `gateView` 页面门禁 |
| `ops_ip_white` / `admin_ip_white` | 运维页 / `IpAdminWhiteService.checkAdmin` 管理 IP 未加白 |
| `ip_white` | 用户防火墙 `IpWhiteService.check`（若仍被调用）IP 未加白 |
| `admin_ip_bootstrap` | 管理 IP 引导密钥无效/错误（hash 保留可重试） |
| `admin_lockdown` / `custom_manage_*` | lockdown / 定制管理页无菜单 |
| `admin_shell_unauthenticated` / `admin_shell_forbidden` | `AdminShellAccess.denyUnlessSystemAdmin` |
| `bean_execute_disabled` | exec Bean 执行器关闭后仍被调用 |
| `data_filter_unauthenticated` | `@DataFilter` 未登录 |
| `webshell_disabled` / `webshell_forbidden` / `webshell_host_denied` / `webshell_session_mismatch` | WebShell 关闭、非超管、主机拒绝、凭据缓存用户不匹配（见 **C-21**） |

成功访问另有 **`SecurityAlert audit action=webshell_*`**（`webshell_access` / `webshell_sftp` / `webshell_ws_*` / `webshell_ssh` 等），凡通过门禁必记。

**运维建议**：对 `SecurityAlert denied` 按 `ip`/`user` 聚合告警；短时大量 `ops_*` / `gen:` / `admin_ip_white` / `admin_ip_bootstrap` / `ip_white` / `webshell_*` 视为扫描或提权探测。

- [ ] **C-21 WebShell**：默认 `/webshell`、`/shell`、`/sftp` 不可用；开启后非超管/未加白 403；未在 `allowed-hosts` 的主机拒绝；检索 `SecurityAlert denied reason=webshell_`

### 4.5 前端 / 模板

- [ ] 登录页、管理页渲染正常（全局未强制 HTML auto-escape，存量 `?html` 可用）  
- [ ] 若某页自行 `<#ftl output_format="HTML">`，确认已去掉 `?html`、富文本处用 `?no_esc`  

---

## 5. 回滚策略

### 5.1 推荐顺序

1. **配置回退（优先）**：用 §3 开关恢复业务必需的旧行为（代理白名单、hint、CSRF、超级密码等），无需回退 JAR。  
2. **代码回退**：回退至加固前 Git 提交 / 上一版本工件。  
3. **数据回退（按需）**：仅当已发生大规模口令/Token 格式升级且必须跑「只认旧格式」的极旧代码时。

### 5.2 口令（D-01）回滚细则

| 场景 | 做法 |
|------|------|
| 回退框架但仍保留双读 matcher | **推荐**：无需动库 |
| 回退到**无** PBKDF2 双读的历史版本 | 已升级用户无法登录 → 需批量重置密码，或从备份恢复 `password`/`salt` |
| 仅部分用户升级 | 重置这些用户密码，或 SQL 从备份恢复其行 |

**禁止**：把公开默认 RememberMe 钥写回生产。

### 5.3 API Token（D-02）回滚细则

| 场景 | 做法 |
|------|------|
| 回退到只查明文的旧 `UserTokenService` | 所有 `sha256:` 行失效 → 用户重新签发 Token；或把哈希行作废并重新 `createApiToken` |
| 保留双读实现 | 无需动库 |

### 5.4 RememberMe（C-02）

- 回退代码并继续使用新 `remember-me.key` / Redis / Env 同一钥：已用新钥加密的 Cookie 仍可用。  
- 删除本地文件、清空 Redis 共享键或更换 SysConfig RememberMe 密钥：所有 RememberMe 失效，用户重新登录。  
- 集群回滚时须保证**所有节点**仍指向同一密钥源，避免「部分节点仍认旧钥」。  

### 5.5 运维检查点（回滚后）

- [ ] 登录、改密、OAuth、Bot Token、代理、CORS 前端  
- [ ] 确认未重新启用公开 RememberMe 钥  
- [ ] 确认 `allowed-hosts` / CSRF / ops Token 符合当前环境预期  

---

## 6. 关键代码与配置索引（便于追溯）

| 主题 | 路径 |
|------|------|
| 出站 SSRF | `OutboundUrlSafety`、`BaseHttpProxyService`、`SECURITY_HTTP_PROXY_CONFIG` |
| 安全加固 SysConfig | `SecurityAuthConfig` / `SecurityHttpProxyConfig` / `SecurityOpsConfig` / `SecuritySessionConfig` |
| 口令双读 | `PasswordHashSupport`、`NamedHashedCredentialsMatcher`、`ShiroUtils`、`SysUserService#login` / `#alignSessionPasswordWithStore`、`SessionPasswordAlignSupport`、`AuthorizationInterceptor`（C-19）、`RedisShiroSessionDAO#forcePersist` |
| 管理员引导口令 | `AdminBootstrapPasswordSupport`、`AdminBootstrapService`、`SecurityAuthConfig.adminBootstrapPending`、`AdminBootstrapController`、管理端 `index.html` 横幅 |
| API Token 哈希 | `UserTokenService` |
| RememberMe 钥 | `RememberMeCipherKeySupport`、`ShiroConfig`、`RedisKeys#getRememberMeCipherKey` |
| hint-only | `UserContextService#resolve` |
| DataFilter | `DataFilterAspect` |
| sidx | `WrapperColumns#orderByColumnExpression` |
| CORS | `SessionFiter`、`CorsOriginSupport` |
| CSRF | `CsrfProtectFilter`、`CsrfTokenSupport`、`GET /sys/session/self/csrf` |
| 机器人授权 | `UserRealm#doGetAuthorizationInfo` |
| 代理文档 | `docs/HTTP_PROXY.md` §2.4 |
| 默认配置 | `web/src/main/resources/application.yml`（及 `-dev`/`-prod`） |

---

## 7. 业务仓升级检查清单（依赖 autumn 3.0.0 加固版）

1. 阅读本手册 §1～§2，标出本业务是否使用：HTTP 代理、hint-only、超级密码、插件热加载、跨域 Cookie、超大上传、管理端纯 Cookie 写操作。  
2. 在 `application-*.yml` / 环境变量补齐：`allowed-hosts`、ops Token、CORS、CSRF（若 prod）、RememberMe 共享钥（多节点）。  
3. 搜索业务代码：`ShiroUtils.sha256(` 与 `user.getPassword()` 的直接相等比较 → 改为 `passwordsMatch`。  
4. 搜索：`userUuid` 无 Token 调开放 API → 改 Token 或临时开 hint。  
5. 跑 §4 回归清单后再上线；保留本手册版本与 Git tag，便于事故回滚对照。

---

## 8. 文档修订记录

| 日期 | 版本说明 |
|------|----------|
| 2026-11-14 | 初版：对应 3.0.0 安全全量加固（1B 代理 + P0～P2），含密码/Token 双读与回滚说明 |
| 2026-11-14 | §2.5～2.6：RememberMe Redis/Env 共享密钥；全量加固项集群平行部署兼容矩阵 |
| 2026-11-14 | §2.5～2.6：密钥落 `node.home/security/`、Docker 挂载与删文件自愈；Redis/单机场景矩阵 |
| 2026-11-14 | 再评审：补 **C-19**（升级后 Session 旧 SHA 被拦截器误踢）；登录先升级再 RememberMe；C-02 路径与 §2.5 对齐 |
| 2026-11-14 | C-19 深化：集群 `invalidateUserCacheCluster`、CAS 未命中仍对齐、`forcePersist`、`SessionPasswordAlignSupport`、preHandle 全请求对齐、改密/重置走集群失效 |
| 2026-11-14 | 配置评审：§3 集群一致性列；RememberMe Env≠Redis 以 Redis 为准；CORS 改 `CorsProperties` 正确绑定 YAML 列表 |
| 2026-11-14 | 安全开关拆为四组 SysConfig json；运行时无 Env 双轨；历史口令/Token 双读保留；首次 put 可吸收旧 autumn.* |
| 2026-11-14 | 评审优化：库空 getter 回落 seed（修 Shiro 早于 put）；`SecurityHttpProxySettings` 替反射；文档标明 Cookie 改后须重启 |
| 2026-11-15 | §2.2 / §2.2.1：管理员引导口令落盘与软提示；超级密码去掉 temp uuid 回落；ops reveal |
