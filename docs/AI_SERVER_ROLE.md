# 服务器角色（Server Role）

> 与 `AI_NODE_PROFILE.md`、`AI_CLUSTER_JOB_ORCHESTRATION.md`、**`AI_CLUSTER_NODE.md`（总览用法）** 配套。强调 **100% 前向兼容**：未配置 roles 时行为与历史全开一致。

## 1. 目标

- 一套本机角色 SSOT：`Profile.roles`（字符串列表，**非枚举**）。
- 框架提供可动态注册的角色元数据与能力门禁；业务可 `ServerRoleRegistry.register` 扩展。
- 默认空 roles / `ALL` → 网页、API、Job、后台**全部允许**。
- 预置含 **LOCAL**（本机/边缘）：与 `JobDuty.LOCAL` 配套，见 `AI_CLUSTER_JOB_ORCHESTRATION.md`。

## 2. 核心类型

| 类型 | 包 | 职责 |
|------|-----|------|
| `ServerRole` | `cn.org.autumn.node.role` | code / name / description / capabilities |
| `ServerRoleRegistry` | 同上 | 全局注册与列表 |
| `ServerRoleGroups` | 同上 | 角色组与写入规范化（含 ALL 互斥） |
| `ServerRoleGate` | 同上 | `isUnrestricted` / `hasCapability` / `allowsAll` / `isLocalScoped` / `allowsClusterJobDuty` |
| `BuiltinServerRoles` | 同上 | Must@5 预置 ALL/WEB/API/WORKER/LOCAL/JOB/MONITOR |
| `ServerRolePathClassifier` | `autumn-modules` | 路径 → 所需能力 |
| `ServerRoleInterceptor` | `autumn-modules` | HTTP 403 门禁 |

### 预置角色

| code | 能力 | 含义 |
|------|------|------|
| ALL | `*` | 全开（默认） |
| WEB | WEB_UI | 网站页 |
| API | API_HTTP, FILE_DOWNLOAD | API / 下载 |
| WORKER | BACKGROUND | 后台静默 |
| LOCAL | LOCAL_JOB | 本机/边缘专岗（本地任务；宽松跳过集群互斥 JobDuty） |
| JOB | SCHEDULED_JOB | 定时任务 |
| MONITOR | MONITOR | 监视 |

多角色可共存（如 `WEB+API`、`LOCAL+WEB+API`）；写入含 `ALL` 时规范化为仅 `["ALL"]`。

LOCAL 专岗（含 LOCAL、不含 JOB、非 unrestricted）对 LoopJob：`SINGLETON`/`SEQUENTIAL` 自动跳过；`ALL`/`LOCAL` 仍可跑。详见 `AI_CLUSTER_JOB_ORCHESTRATION.md` §1.0.0。

## 3. 兼容铁律

- `roles` 空或仅空白 → `isUnrestricted()==true`。
- `roles` 含 `ALL` → 全开。
- LoopJob `@JobMeta(roles=...)` 经 `ServerRoleGate.allowsAll`：空 required / 本机 unrestricted → 允许。
- LOCAL 专岗另经 `allowsClusterJobDuty` 宽松跳过 SINGLETON/SEQUENTIAL（见编排文档）。
- **BuiltinServerRoles 不写 Profile**，避免启动误开闸。

## 4. HTTP 限制

`ServerRoleInterceptor`：unrestricted 直接放行；否则按路径所需能力检查。

可配：

- `autumn.node.role.api-path-prefixes`
- `autumn.node.role.web-path-prefixes`
- `autumn.node.role.download-path-prefixes`

（逗号/分号/空白分隔；缺省见 `ServerRolePathClassifier`。）

## 5. 业务扩展

```java
registry.register(ServerRole.of("AGENT", "Agent", "Agent WS 建连", Set.of("AGENT_WS")));
```

扩展角色能力字符串由业务约定；Gate 对已注册角色读 capabilities，未知 code 仅按同名能力兜底。

## 6. 配置本机角色

- 管理页：运维监控 → **集群节点**（`cluster.html`）：本机编辑、单节点 / 批量远程 assign
- `PUT /sys/node/profile` body `roles`
- `PUT /sys/node/registry/assign` / `assign-batch`
- `ProfileService.roles(...)` / Registry 远程 assign
- 业务在写库后同步本机 Profile（见 BigHub Web Pod）
