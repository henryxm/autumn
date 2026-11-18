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
- 响应：`autumn=true`、`protocolVersion=1`、`opsCert`=`none|pending|ready`、`adminIpEnable`、`enrollAvailable`、`siteId`、`serverFingerprint`
- 无密钥材料；错误 Token → 403

### 3.1 客户端探测结果矩阵

| 结果 | 判定 | 用户文案 |
|------|------|----------|
| TCP/TLS/超时/DNS | 网络失败 | 无法连接站点，检查地址与网络 |
| HTTP 404 / 非 JSON / 无 `data.autumn==true` | **不兼容** | 未部署支持运维证书的 Autumn，或为旧版本，不支持本方案 |
| 403 | Token/网关 | 探测被拒绝，检查网关是否剥离自定义头 |
| `pending` + `enrollAvailable` | 可入驻 | 从该站控制台复制 `OpsEnroll secret=` |
| `ready` + 本地有证 | 可管理 | 进入该站管理页 |
| `enrollAvailable=false` 且本地无证 | 需重置 | wall 重置或 DB 清理后再入驻 |

## 4. API 一览

| Action | 鉴权 | 说明 |
|--------|------|------|
| `POST /ops/api/v1/probe` | Probe Token | 能力与入驻态 |
| `POST /ops/api/v1/enroll` | body.`secret`=OpsEnroll | body.`clientPublicKey`；返回 `certId`/`siteId`/`serverPublicKey`；烧毁共享 enroll hash；**不下发私钥** |
| `POST /ops/api/v1/challenge` | body.`certId` | `nonce`（TTL≈120s） |
| `POST /ops/api/v1/session` | 签名 + AES 包裹 | 返回 `sessionId` |
| `POST /ops/api/v1/adminip/list\|add\|remove\|setForbidden` | `X-Ops-Session` | 管理 IP（`wall_ip_admin_white`）；见下表 |
| `POST /ops/api/v1/currentIp` | session | 当前访问 IP |
| `POST /ops/api/v1/ipwhite/list\|add\|remove\|setForbidden` | session | 用户防火墙白名单（`wall_ip_white`）；见下表 |
| `POST /ops/api/v1/ipblack/list\|add\|remove\|setAvailable` | session | 用户防火墙黑名单（`wall_ip_black`）；见下表 |
| `POST /ops/api/v1/ipblack/firewallCount/get\|set` | session | 自动拉黑阈值（存于策略行 `0.0.0.0` 的 `count`；`set` body.`count`，下限 500） |

### 4.1 三类 IP 列表与禁用

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
| `POST /ops/api/v1/runtime/snapshot` | session | 运行快照：`system`/`jvm`/`disk`/`database`/`firewall`/`traffic`/`redis`/`paths`（分段独立失败不影响整包） |
| `POST /ops/api/v1/gate/get\|setAdminIpEnable\|verifyClearToken\|setClearToken` | session；关校验 / 改令牌须原 clearToken | `get` 含 `adminIpEnable` + `clearTokenConfigured`；未配置时 App 可 `setClearToken` 首次写入；已配置须 `oldClearToken`；空令牌时危险操作拒绝；`enroll`/`claim` 自动开管理IP |
| `POST /ops/api/v1/cert/list\|revoke` | session | 多设备列表/吊销；`revoke` 响应含 `lastCert`（吊销后是否已无有效证） |
| `POST /ops/api/v1/cert/issue` | session | 已登录设备签发一次性认领码（明文仅返回一次；落库 salt+hash，TTL≈30min） |
| `POST /ops/api/v1/cert/claim` | body.`claimSecret` | 新设备本地生成 RSA → 提交公钥认领；返回同 enroll（**不下发私钥**） |
| `POST /ops/api/v1/reset` | 超管登录 + `clearToken` | 清客户端、清 pendingClaims、清服务端钥、重发 OpsEnroll（RSA 延后到下次 enroll） |

Shiro：`/ops/api/v1/**` = `anon`。`AdminAccessGuard` 豁免 `OpsApiController`。  
Wall：`GET /wall/api/opscert/status`、`POST /wall/api/opscert/reset`。

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

## 6. 错误与安全

| 场景 | 码 |
|------|-----|
| Probe/入驻/签名失败 | 403 |
| 缺会话 / 会话过期 | 401 |
| 参数缺失 | 400 |

- 会话只授权运维白名单 / 用户防火墙名单 / 门禁开关 / 证书吊销，**不做**任意 gen CRUD。
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
- [ ] OpsEnroll 仅成功一次；响应无私钥
- [ ] 双节点同一 OpsEnroll 均可 enroll；fingerprint/clients 一致
- [ ] session → claimCurrent → wall 可见管理 IP
- [ ] App 两站点证书互不覆盖
- [ ] `cert/issue` → 他机 `cert/claim` 得独立 certId；认领码不可复用
- [ ] 吊销最后一张有效证响应 `lastCert=true`
- [ ] `/rsa/api/v1` 与账号密码登录回归
- [ ] wall bootstrap / clearToken 解锁仍可用
