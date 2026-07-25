# 本机节点画像（Node Profile）

> 适用：`autumn-lib` / `autumn-modules`。通用基础能力，供任意业务项目注入使用。  
> 相关：`docs/AI_CLUSTER_JOB_ORCHESTRATION.md`（LoopJob `JobDuty`；多接口 Bean 见 §1.3）；`docs/AI_SERVER_ROLE.md`（服务器角色与能力门禁）；**`docs/AI_CLUSTER_NODE.md`**（集群节点总览 / Registry / 管理页）。

## 1. 作用

- 为本 JVM 进程生成并持久化稳定节点身份 **`uuid`**（32 位小写 hex）。
- 文件：`{home}/node-profile.json`；未配置时默认 **`{user.home}/.autumn`**（绝对路径，避免 Docker 下 `user.dir=/` 落到 `/.autumn`）。
- 业务侧注入 `NodeProfile` / `ProfileService`：`uuid()` 作为服务节点主键。
- **推荐扩展**：业务键进 **`labels`**（框架不解释键含义）。若子项目在 JSON **顶层**另写扩展字段，框架写盘（`roles` / `patch` / `ensure` 回写等）**只覆盖** `uuid`/`version`/`create`/`update`/`roles`/`labels`，其它顶层键**保留**。
- `roles` 为**服务器角色**列表（见 `AI_SERVER_ROLE.md`）：空或 `ALL` = 全开；亦供 LoopJob `@JobMeta(roles)` 门禁。边缘节点可设 `["LOCAL","WEB","API"]`：跑本机/`ALL` 任务，宽松跳过 `SINGLETON`/`SEQUENTIAL`（见 `AI_CLUSTER_JOB_ORCHESTRATION.md` §1.0.0）。
- HTTP `PUT /profile` 的 `labels` 为**合并**（传入键覆盖/新增，值为 `null` 删除该键），不整表清空未提及的 labels。

## 2. 配置

| 键 | 含义 | 默认 |
|----|------|------|
| `autumn.node.home` | 画像目录（绝对或相对；相对路径相对 `user.home`） | `{user.home}/.autumn` |
| `autumn.node.salt` | 同机多实例区分盐 | 空 |
| `autumn.node.profile.cache-ttl-ms` | 内存缓存 TTL；≤0 每次读盘 | `60000` |
| `autumn.node.registry` | 集群登记心跳 | **未配置时跟随 `autumn.redis.open`**；显式 `true`/`false` 可覆盖。配置开启但 Redis/Redisson 不可用时**不生效、不阻断启动**，打一次 info 日志 |
| `autumn.node.namespace` | 登记 Redis 命名空间 | `default` |

示例：

```yaml
autumn:
  redis:
    open: true          # Registry 未显式配置时随之开启
  node:
    # registry: true    # 可选：显式覆盖；false 可在 Redis 开启时仍关闭登记
    namespace: default
    home: ${user.home}/.myapp
```

## 3. JSON 字段

```json
{
  "uuid": "a1b2c3d4e5f6789012345678abcdef01",
  "version": 1,
  "create": "2026-07-20T15:00:00Z",
  "update": "2026-07-20T15:00:00Z",
  "roles": [],
  "labels": {}
}
```

- 启动 `ensure()`：保证 `uuid`；`roles` 为空。
- **`roles` 非空**才参与 LoopJob 角色门禁。
- 写盘只更新上表框架字段；子项目额外顶层键（如 `"biz": {...}`）在 `roles()` / 集群 assign / `patch` 后仍保留。

## 4. 加载与刷新

| 方式 | 说明 |
|------|------|
| 启动 | `LoadFactory.Must` → `ensure()` |
| API 写 | `save` / `patch` / HTTP PUT → 立即刷新缓存 |
| TTL | 读路径惰性失效后读盘（默认 1 分钟） |
| 显式 | `reload()` / `POST /sys/node/profile/reload` |

## 5. 指纹 Snapshot

`Fingerprint.collect()` / `FingerprintProvider.collect()` 返回：

| 字段 | 含义 |
|------|------|
| `macAddresses` | 物理网卡 MAC |
| `machineId` | `/etc/machine-id` 等 |
| `hostname` / `os` / `arch` | 主机与 OS |
| `hash32` | SHA-256 前 32 hex → 节点 uuid |
| `advertiseHostCandidate` | 建议通告 IPv4 |

`DefaultFingerprintProvider` 为默认 Bean；可自定义 `FingerprintProvider` 替换。

## 6. ProfileCustomizer

```java
@Component
@Order(100)
public class MyCustomizer implements ProfileCustomizer {
  public void onCreate(Profile profile, Fingerprint.Snapshot snap) {
    // 首次创建：写入 labels
  }
  public boolean onLoad(Profile profile, Fingerprint.Snapshot snap) {
    // 已有文件：labels 缺失时补全并 return true → 框架回写磁盘
    return false;
  }
}
```

- `onCreate`：仅首次创建落盘前。
- `onLoad`：读盘命中已有 uuid 时；返回 true 表示已改 Profile，框架落盘。

`home` 未显式 `home(dir)` 时，每次按配置键解析（避免 Bean 构造早于 Spring Environment）。

## 7. 生命周期（消费方扩展）

1. `LoadFactory.Must`：`ProfileService.ensure()` → 本地 uuid 就绪（含 onLoad 补全）。  
2. 消费方 `InitFactory.Init`（建表后）：`profile.uuid()` + `lastSnapshot()`/labels → DB upsert 等。

## 8. Java API

注入 `NodeProfile` 或 `ProfileService`：

| 方法 | 说明 |
|------|------|
| `uuid()` / `peekUuid()` / `profile()` / `get()` | 读身份；`peekUuid` 不 ensure |
| `ensure()` / `reload()` / `patch` / `roles` / `label` | 写与刷新；已有文件仅 `lastSnapshot==null` 时采指纹 |
| `home(dir)` | 显式切换目录（之后固定覆盖配置） |
| `lastSnapshot()` | 最近一次采集快照 |

组件：`Profile`、`Fingerprint`、`FingerprintProvider`、`ProfileStore`、`ProfileService`、`ProfileCustomizer`、`NodeProfile`、`Registry`。

## 9. HTTP

前缀 `/sys/node`：

| Method | Path | 说明 |
|--------|------|------|
| GET/PUT | `/profile` | 读/补丁本机画像（含 `roles`） |
| PUT | `/profile/home` | 切换 home |
| POST | `/profile/reload` | 强制读盘 |
| POST | `/profile/reset-uuid` | 重算 uuid |
| GET | `/roles` | 已注册 ServerRole 目录（管理页勾选） |
| GET | `/registry` | `enabled` / `configured` / `redisOpen` / `followsRedis` / `namespace` / `staleMs` / `online` / `nodes` / **`members`** |
| POST | `/registry/beat` | 立即上报本机心跳（改本机 roles 后刷新成员表） |
| PUT | `/registry/assign` | 单节点远程改 roles（目标为本机时同步落盘） |
| PUT | `/registry/assign-batch` | `{ uuids, roles }` 批量远程指派 |

管理页：运维监控 → **集群节点**（`cluster.html`）。Registry 默认跟随 `autumn.redis.open`；显式 `autumn.node.registry` 可覆盖。
