# Autumn 运维双边 RSA 证书通道（`/ops/api/v1`）

> 与业务传输加密 [`AI_CRYPTO.md`](AI_CRYPTO.md) / `/rsa/api/v1` **完全隔离**（URL、会话、存储、密钥）。  
> 管理 IP 门禁关系见 [`AI_ADMIN_IP_BIZ.md`](AI_ADMIN_IP_BIZ.md)；加固清单旁注见 [`AI_SECURITY_HARDENING_3_0_COMPAT.md`](AI_SECURITY_HARDENING_3_0_COMPAT.md) **C-20**。

## 1. 目标与独立性

- **双边 RSA**：服务端与客户端各自持有密钥对；**私钥永不经过网络**。
- **与账号密码登录独立**：证书通道只负责管理 IP 白名单与 `adminIpEnable` 等高敏感运维开关；日常后台菜单/gen CRUD 仍须用户名密码（且在授权 IP 下）。证书 = 进门（IP），账号 = 进房间（身份）。
- **与业务传输加密独立**：禁止复用 `/rsa/api/v1` 会话与 EncryptKey。
- **单机与集群同一份秘钥**：服务端 RSA、已入驻客户端公钥、OpsEnroll 校验材料均在共享 `SECURITY_OPS_CERT_CONFIG`（sys_config）；禁止各节点各执一份。
- 入驻一次性：日志 `OpsEnroll secret=`（与 `AdminIpBootstrap secret=` **独立**）；明文不落库。
- 客户端：**多站点列表**（多个 Autumn 后台域名各自探测/换证/管理）。

## 2. 密钥与存储

| 材料 | 真源 | 说明 |
|------|------|------|
| 服务端 RSA、`siteId`、指纹、`clients[]` | `SECURITY_OPS_CERT_CONFIG` | 集群各节点只读同一配置；**不在启动时生成**，在 `enroll` 校验 OpsEnroll secret **成功后**「空则写」 |
| OpsEnroll 校验 | 同配置 `enrollSaltB64` + `enrollHashB64` | 启动（或 reset）写入 hash；明文仅日志 `OpsEnroll secret=`；**无明文落库** |
| 多设备认领码 | 同配置 `pendingClaims[]` | `issue` 写入 salt+hash；明文 `claimSecret` 仅 API 返回一次；TTL≈30min、单次消费 |
| challenge / ops session | Redis 短 TTL（有 Redis）；否则本机内存 | 键：`{ns}:autumn:ops:challenge|session:*` |
| 客户端私钥 | 仅 App 本机按站点分目录 | 永不上传 |

网络上可出现：服务端公钥、nonce、客户端公钥（仅入驻）、签名、AES 包裹密钥。一期管理 API 以 `X-Ops-Session` 鉴权为主，body 可为明文 JSON。

## 3. 探测约定

- Header：`X-Autumn-Ops-Probe: autumn-ops-probe`（`AutumnOpsProbe.TOKEN`）
- `POST /ops/api/v1/probe`
- 响应：`autumn=true`、`protocolVersion=1`、`opsCert`=`none|pending|ready`、`adminIpEnable`、`enrollAvailable`、`discoverEnabled`、`siteId`、`serverFingerprint`
- 无密钥材料；错误 Token → 403

### 3.0 发现 URL（分享 / 扫码）与公开落地页

- 规范形态：`{baseUrl}/ops/discover`（由站点 baseUrl **动态拼接**，**不含** OpsEnroll / 分享口令 / 私钥）
- `GET /ops/discover`：匿名 **超然信公开落地页**（受 `SECURITY_OPS_CERT_CONFIG.discoverEnabled` 控制，**默认 true**）
  - **开启**：模板 `templates/ops/discover.html` — 双轨并重（站点运维 / 超然信下载）、双二维码、可复制本页链接
  - **关闭**：对外 **404**（如同页面不存在，避免被探测）；**不**渲染关闭说明页、**不**输出站点/运维字段
  - 证书 **enroll / cert/claim 成功后自动** `discoverEnabled=false`；`reset` 后恢复 `true`；App 可经会话再开关
  - 后台登录首页 `main`：管理员已登录，**始终**完整落地页（版面不受开关控制）；模板注入 `publicDiscoverEnabled` 真值，公网已关时提示「勿对外分发运维码」
  - **注意**：`discoverEnabled` **不削弱** T0 `probe`（持公开 Probe Token 仍可探测）；仅控制浏览器 HTML 发现页是否存在
  - 可选商店链与官网 URL 见 `SITE_PORTAL_CONFIG.chaoranxin`（`docs/AI_SITE_PORTAL.md`）
- Shiro：`/ops/discover` = `anon`；管理 IP 门禁豁免 `OpsDiscoverController`；关闭发现页**不**禁止 `/ops/api/v1/probe`
- 后台菜单：运维监控 →「公开落地页」新窗口打开 `/ops/discover`（用于查看**公网真实态**：关闭时即为 404；完整落地预览请看登录后首页 `main`）
- App：拦截该 path（或 `crchat://vc/autumnops?base=`）→ 剥离 baseUrl → 本地 `POST …/probe`；**不依赖落地页文案**
- 等价标记（客户端）：`?autumn_ops=1` / `#autumn_ops`（可选）

### 3.1 客户端探测结果矩阵

| 结果 | 判定 | 用户文案 |
|------|------|----------|
| TCP/TLS/超时/DNS | 网络失败 | 无法连接站点，检查地址与网络 |
| HTTP 404 / 非 JSON / 无 `data.autumn==true` | **不兼容** | 未部署支持运维证书的 Autumn，或为旧版本，不支持本方案 |
| 403 | Token/网关 | 探测被拒绝，检查网关是否剥离自定义头 |
| `pending` + `enrollAvailable` | 可入驻 | 从该站控制台复制 `OpsEnroll secret=` |
| `ready` + 本地有证 | 可管理 | 进入该站管理页 |
| `enrollAvailable=false` 且本地无证 | 需重置 | wall 重置或 DB 清理后再入驻 |

## 4. 鉴权分层与 API 矩阵

### 4.0 威胁模型与横切

| 层 | 机制 | 说明 |
|----|------|------|
| Shiro | `/ops/api/v1/**` = **`anon`** | 框架不拦；**真实鉴权在 `OpsApiController` / 服务层** |
| 管理 IP 门禁 | `AdminAccessGuard` **豁免** `OpsApiController` | 门禁开启时 App 仍可靠证书会话加白（设计如此） |
| Wall / Shield | **`/ops/api/v1` 未**列入 `isBypassUri` | 黑名单或攻击模式强校验仍可能拦住 ops（与「证书自救」冲突）→ 见 §4.5 残留风险 |
| 传输 | **须 TLS** | 一期管理 API body 多为明文 JSON；`X-Ops-Session` 鉴权；开会话时协商 AES，**管理读写未强制 body 加密** |
| CORS | Allow-Headers 默认**未**含 Probe/Session 头 | 原生 App 通常无感；Web 壳需网关补头 |

**分层一览**

| 层级 | 含义 | 允许泄露面 |
|------|------|------------|
| **T0** 发现 | 固定公开 Probe Token（源码/文档可知） | 协议能力位、`opsCert` 态、`siteId`/指纹摘要；**无私钥 / IP 名单 / runtime** |
| **T1** 入驻 | 一次性 OpsEnroll / claimSecret | 服务端公钥与 `certId`；**永不下发任何私钥** |
| **T2** 握手 | 有效证 + 私钥签名 | `nonce` / `sessionId` |
| **T3** 管理 | `X-Ops-Session`（危险写 + clearToken） | IP 名单、门禁、证书列表、runtime 等 |
| **T4** 破窗 | 超管登录 + clearToken（或 wall 同源） | 清证重置 |

```text
probe(T0) → enroll|claim(T1) → challenge → session(T2) → 管理 API(T3)
                                                      ↘ 危险写 + clearToken
reset / wall opscert/reset (T4，不依赖 ops session)
```

### 4.1 完整 API 鉴权矩阵（`POST /ops/api/v1/…`，共 29）

列说明：**鉴权** = Controller/Service 实际校验；**附加** = clearToken 等；**敏感响应** = 须防经弱鉴权泄露；**结论** = 安全评审口径。

| # | 路径 | 层级 | 鉴权（Helper） | 附加 | 敏感响应（摘要） | 风险 | 结论 |
|---|------|------|----------------|------|------------------|------|------|
| 1 | `/probe` | T0 | `assertProbe`（头 `X-Autumn-Ops-Probe: autumn-ops-probe`） | — | `autumn`/`protocolVersion`/`opsCert`/`adminIpEnable`/`enrollAvailable`/`discoverEnabled`/`siteId`/`serverFingerprint` | 指纹可被扫 | **允许「匿名」发现**（持公开 Probe Token）；**禁止**扩返回名单/路径/JVM/clients |
| 2 | `/enroll` | T1 | body.`secret`=OpsEnroll + `clientPublicKey`（服务层） | 成功烧毁 enroll hash；自动开管理 IP + 认领当前 IP；**关发现页** | `certId`/`siteId`/`serverPublicKey`/`serverFingerprint`/`adminIpEnable`/`discoverEnabled` | secret 泄露=可入驻 | **非匿名**；依赖日志口令熵；**不下发私钥** |
| 3 | `/challenge` | T2 | 有效 `certId` | — | `nonce`/`expireSeconds` | 可探测证是否存在 | **持证前置**；certId 宜不可猜测；见 §4.5 |
| 4 | `/session` | T2 | certId + nonce + 客户端私钥签名 + AES 经服务端公钥包裹 | — | `sessionId`/`expireAt` | 低（密码学绑定） | **证明持证**；换取会话头 |
| 5 | `/adminip/list` | T3 | `requireOpsSession` | — | 管理 IP 分页列表 | 高 | **必须会话** |
| 6 | `/adminip/add` | T3 | session | 可 `claimCurrent` | `ip`/成功标志 | 高 | **必须会话** |
| 7 | `/adminip/remove` | T3 | session | 删**当前访问 IP** 时 `assertClearToken` | `ip` | 高 | 会话 + 条件 clearToken |
| 8 | `/adminip/setForbidden` | T3 | session | — | 状态 | 高 | **必须会话** |
| 9 | `/currentIp` | T3 | session | — | 当前访问 IP | 低 | **必须会话** |
| 10 | `/ipwhite/list` | T3 | session | — | 用户白名单列表 | 中高 | **必须会话** |
| 11 | `/ipwhite/add` | T3 | session | 可 `claimCurrent` | `ip` | 中高 | **必须会话** |
| 12 | `/ipwhite/remove` | T3 | session | 删当前 IP 时 clearToken | `ip` | 中高 | 会话 + 条件 clearToken |
| 13 | `/ipwhite/setForbidden` | T3 | session | — | 状态 | 中高 | **必须会话** |
| 14 | `/ipblack/list` | T3 | session | — | 黑名单列表（不含 `0.0.0.0` 哨兵） | 中高 | **必须会话** |
| 15 | `/ipblack/add` | T3 | session | 拒 `0.0.0.0` | `ip` | 中高 | **必须会话** |
| 16 | `/ipblack/remove` | T3 | session | 拒删哨兵 | `ip` | 中高 | **必须会话** |
| 17 | `/ipblack/setAvailable` | T3 | session | 拒哨兵 | `available`（0=生效） | 中高 | **必须会话** |
| 18 | `/ipblack/firewallCount/get` | T3 | session | — | `firewallCount` | 中 | **必须会话** |
| 19 | `/ipblack/firewallCount/set` | T3 | session | 下限 500 | `firewallCount` | 中 | **必须会话** |
| 20 | `/gate/get` | T3 | session | — | `adminIpEnable`/`discoverEnabled`/`clearTokenConfigured`（布尔，**非明文**） | 低中 | **必须会话**；禁止下发 clearToken 明文 |
| 21 | `/runtime/snapshot` | T3 | session | — | 主机/磁盘/库/路径/防火墙样本/Redis/访问量等 | **高** | **必须会话**；禁止降到 Probe |
| 22 | `/gate/verifyClearToken` | T3 | session + `assertClearToken` | — | `valid` | 低（可爆破） | 会话 + 正确令牌 |
| 23 | `/gate/setClearToken` | T3 | session | 已配置须 `oldClearToken`；首次可直接设 | `clearTokenConfigured`/`changed` | 中（首次抢注） | **必须会话**；见 §4.5 |
| 24 | `/gate/setAdminIpEnable` | T3 | session | `enable=false` 须 clearToken | `adminIpEnable` | 高 | 关校验 = 会话 + clearToken |
| 24b | `/gate/setDiscoverEnabled` | T3 | session | body.`enable`；**不要求** clearToken | `discoverEnabled` | 低中 | 会话即可开关公开发现页 |
| 25 | `/cert/list` | T3 | session | — | 设备 `certId`/label/revoked/时间/`self`/`activeCount` | 中 | **必须会话** |
| 26 | `/cert/issue` | T3 | session | — | **`claimSecret` 明文一次**、claimId、expiresAt | 高 | **必须会话**；仅展示一次；须 TLS |
| 27 | `/cert/claim` | T1 | body.`claimSecret` + `clientPublicKey` | 成功同 enroll：开管理 IP + **关发现页** | 同 enroll + `discoverEnabled` | 高（码泄露） | **非匿名**；单次消费 |
| 28 | `/cert/revoke` | T3 | session | — | `success`/`lastCert` | 高 | **必须会话**；吊销不自动重开发现页 |
| 29 | `/reset` | T4 | `assertClearToken` + `requireSystemAdmin` | **不要** ops session | siteId/fingerprint/`enrollAvailable`/`discoverEnabled`/hint | 关键 | **超管登录 + clearToken**；reset **重开**发现页 |

**浏览器 wall 旁路（非 App 通道，对照用）**

| 路径 | 鉴权 | 说明 |
|------|------|------|
| `GET /wall/api/opscert/status` | `requireOps`（超管 + 可选管理 IP） | 证书态摘要 |
| `POST /wall/api/opscert/reset` | 超管 | 等同清证重生（日志新 OpsEnroll） |

### 4.2 三类 IP 列表与禁用

| 表 | 接口前缀 | 状态字段 | 语义 | 危险操作 |
|----|----------|----------|------|----------|
| 管理级 IP 白名单 | `/adminip` | `forbidden` | `0` 启用 / `1` 禁用 | 删除**当前访问 IP** 须 `clearToken` |
| 用户白名单 | `/ipwhite` | `forbidden` | `0` 启用 / `1` 禁用 | 删除**当前访问 IP** 须 `clearToken` |
| 用户黑名单 | `/ipblack` | `available` | **`0` 生效拦截** / **`1` 停用**（与 `load→getIps(0)` 一致） | 无 clearToken 要求 |

**黑名单策略哨兵 `0.0.0.0`**：不是真实客户端地址，而是把「刷新周期内访问次数阈值」写在该行的 `count` 字段（`available=1` 使其不进拦截缓存）。列表接口**排除**该行；禁止 `add`/`remove`/`setAvailable`；改阈值用 `firewallCount/get|set`（wall 页「黑名单阈值」同源）。

列表约定（三表相同）：

- 请求：`page`（从 1）、`limit`（默认 20，最大 100）；`add` 可传 `claimCurrent=true` 认领当前访问 IP
- 响应：`count`、`page`、`limit`、`hasMore`、`list[]`（`ip` / `tag` / `description` / 状态字段 / `createTime` 毫秒时间戳）
- `setForbidden` body：`ip` + `forbidden`∈{0,1}
- `setAvailable` body：`ip` + `available`∈{0,1}（黑名单：**0=生效**）

### 4.3 敏感数据与禁止项

| 通道 | **允许**返回 | **禁止**返回 / 行为 |
|------|--------------|---------------------|
| T0 `probe` | 能力位、入驻态、`siteId`、指纹摘要、`adminIpEnable` | 私钥、`clients[]` 明细、任一类 IP 名单、clearToken 明文、runtime/JDBC 密码、磁盘路径细节 |
| T1 enroll/claim | 服务端**公钥**、`certId`、指纹、`adminIpEnable` | 服务端/客户端**私钥**；不得在错误响应中区分「口令格式」与「口令正确但其它失败」到可利用程度以外的额外材料 |
| T2 challenge/session | nonce、sessionId、过期 | 名单 / runtime |
| T3 session 管理 | 名单、门禁布尔、证书元数据、runtime 分段、一次性 `claimSecret` | clearToken **明文**；不得把 session API 改成仅 Probe Token |
| T4 reset | 重置后 siteId/指纹/enroll 可用提示 | 新 OpsEnroll **明文只进服务器日志**，不进 API body |

部署：**禁止**对公网以明文 HTTP 暴露 `/ops/api/v1`。会话只授权运维名单 / 门禁 / 证书 / runtime，**不做**任意 gen CRUD。

### 4.4 错误码（鉴权相关）

| 场景 | 码 |
|------|-----|
| Probe Token / 入驻口令 / 签名 / 认领码失败 | 403 |
| 缺 `X-Ops-Session` / 会话过期 | 401 |
| 参数缺失 | 400 |

### 4.5 残留风险（接受或后续加固）

| 风险 | 现状 | 处理 |
|------|------|------|
| 固定公开 Probe Token | 文档/源码可知 | **接受**（发现必需）；严格限制 probe 响应面（§4.3） |
| `challenge` 可探测 certId 是否存在 | 无效证 → 403 | **接受**（依赖 UUID 熵）；后续可统一错误文案 / 限速 |
| Wall/Shield 仍拦 ops | 未 bypass | **文档标明**；黑名单自救可能失败 → 后续评估将 `/ops/api/v1/**` 加入旁路或对有效 Session 放行 |
| 无 Redis 多节点会话 | 内存会话 | 需 LB 粘性；入驻验密不依赖粘性 |
| 持 session 设备失窃 | 可改名单/发认领码/拉 runtime | 吊销证、短会话 TTL；运维设备须保护 |
| 首次 `setClearToken` 可抢注 | 未配置时可直接写入 | 审计；后续可加 enroll 后短窗 |
| 一期 body 明文 | 依赖 TLS | **接受**于内网/HTTPS；公网必须 TLS |
## 5. 完整交互时序

1. App 多站点列表 → 添加标题 + Base URL → **自动 probe**。
2. 不兼容则提示并留在添加页；可返回列表。
3. `pending`：粘贴控制台 `OpsEnroll secret=` → 本地生成 RSA → `enroll` → 分站目录存私钥。  
   **`enroll` / `cert/claim` 成功后服务端自动：`adminIpEnable=true` + 认领当前访问 IP 到管理白名单**（默认关闭，对接成功即开启；幂等）。
4. `challenge` → `session` → 可选继续管理 `ipwhite` / `ipblack`；门禁开关亦可在 App 概览手动关（须 clearToken）。
5. 浏览器在该管理 IP 用**账号密码**登录后台做日常管理；用户白名单只管防火墙防拉黑，与管理 IP **分表**。
6. 返回列表可添加下一 Autumn 系统；各站证书文件隔离。

### 5.1 丢钥恢复

1. **能登录 wall**：重置证书 → 日志新 `OpsEnroll` → App 对该站再入驻。
2. **IP 锁死且证书丢失**：DB 将 `SECURITY_OPS_CERT_CONFIG` 置为 `{}`（或清空 `param_value`）→ **重启** → 日志出现新 `OpsEnroll`（此时尚无服务端 RSA）→ App 再 enroll（此时才生成服务端钥）。  
   **仅重启不够**：库中仍有 `clients` 时不会重打临时密码。
3. 禁止用 `/rsa/api/v1` 或普通用户登录补丁恢复。

示例（MySQL，按实际表名调整）：

```sql
UPDATE sys_config SET param_value = '{}' WHERE param_key = 'SECURITY_OPS_CERT_CONFIG';
```

### 5.2 多设备

1. **推荐（独立密钥）**：已登录设备 `POST /ops/api/v1/cert/issue` → 将 `claimSecret` 分享给新设备 → 新设备本地生成 RSA → `POST /ops/api/v1/cert/claim`。每台设备各自 `certId`，可单独吊销。
2. **同证备份/迁移**：App 口令导出**同一**客户端私钥包并在另一设备导入（共享同一 `certId`；适合备份，不适合作为「给同事单独一台机」的主路径）。
3. **唯一设备丢失 / 全部吊销**：走 §5.1 wall 重置或 DB 清空后再 `OpsEnroll` 入驻。

## 6. 错误与安全（摘要）

鉴权分层、完整矩阵、敏感禁止项与残留风险见 **§4**（以 §4.1 / §4.3 / §4.5 为准）。

- 错误码见 §4.4。
- 关 `adminIpEnable`、删除「当前访问 IP」的管理/用户白名单均须 `clearToken`（与 wall 对齐）。
- 无 Redis 的多节点：会话阶段需 LB 粘性；**入驻验密不依赖粘性**（共享 hash）。
- 服务端 RSA **不在启动时生成**；仅在 OpsEnroll secret 校验成功后「空则写」。

## 7. 客户端

- **真源**：独立仓 [`autumn-app`](https://git.minclouds.com/henryxm/autumn-app.git)（本地常并列于 `../autumn-app`）
  - `packages/autumn_ops` — 协议 lib（probe/enroll/session/adminip/gate/ipwhite/ipblack）
  - `packages/autumn_ops_ui` — Material 多站点列表/添加/管理
  - `shell` — 独立 Flutter 启动壳（打包运行）
- 超然信 / XSign / XSVideo **暂不内嵌**运维客户端；运维使用独立壳。
- 协议字段以本文为准。

## 8. 联调清单

- [ ] probe → pending/ready；错误站「不兼容」
- [ ] **无** Probe 头调 probe → 403；**仅** Probe 头调 `adminip/list` / `runtime/snapshot` → **401**（缺会话）
- [ ] OpsEnroll 仅成功一次；响应无私钥
- [ ] 双节点同一 OpsEnroll 均可 enroll；fingerprint/clients 一致
- [ ] session → claimCurrent → wall 可见管理 IP
- [ ] App 两站点证书互不覆盖
- [ ] `cert/issue` → 他机 `cert/claim` 得独立 certId；认领码不可复用
- [ ] 吊销最后一张有效证响应 `lastCert=true`
- [ ] `/rsa/api/v1` 与账号密码登录回归
- [ ] wall bootstrap / clearToken 解锁仍可用
- [ ] （现状记录）客户端 IP 在用户黑名单时：`/ops/api/v1/probe` 与 session 是否被 Wall 拦截（见 §4.5）
