# 机器人开放 API 参考手册

> 配套指南：**`docs/AI_ROBOT.md`**（对接流程、验签、示例、清单）。  
> 基础路径：`{ORIGIN}/robot/api/v1`（`ORIGIN` 为业务系统部署的 Autumn 应用根 URL，无尾斜杠）。

## 0. 通用约定

### 0.1 HTTP

| 项 | 值 |
|----|-----|
| Method | 全部为 **`POST`** |
| Content-Type | `application/json;charset=UTF-8` |
| 字符编码 | UTF-8 |

### 0.2 请求包装 `Request<T>`

除特别说明外，请求体为 JSON 对象，业务字段放在 **`data`** 中：

```json
{
  "data": { }
}
```

`Request` 可含框架级时间戳等字段（`TimestampClient`），业务对接通常只需关心 **`data`**。

### 0.3 响应包装 `Response<T>`

```json
{
  "code": 0,
  "msg": "success",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | **`0` 表示成功**；非 0 为失败 |
| `msg` | string | 提示信息；成功常为 `success` |
| `data` | T | 业务数据；部分接口成功时 `data` 为字符串 `"success"` |

失败时 `data` 可能为 `null`。业务代码应 **先判断 `code === 0`**，再解析 `data`。

### 0.4 鉴权请求头

**管理 API**（用户令牌，来自 `usr` 模块，非 `rbt_`）：

```http
Token: <用户访问令牌>
```

或使用（优先级见 `ApiAuthSupport`）：

```http
X-Robot-Token: <仅当传 rbt_ 时会被识别为机器人令牌，管理 API 会拒绝>
Authorization: Bearer <用户令牌>
```

**入站消息 API**（机器人令牌）：

```http
X-Robot-Token: rbt_xxxxxxxxxxxxxxxx
```

推荐机器人侧固定使用 `X-Robot-Token`，避免与用户令牌混淆。

### 0.5 常见错误码

| code | 典型 msg | 场景 |
|------|----------|------|
| `0` | success | 成功 |
| `-10000` | 请登录 / 请使用用户令牌 / 请使用机器人访问令牌 | 未带令牌、令牌无效、令牌类型与接口不匹配 |
| 非 0 | 业务文案 | `CodeException` 文案，如「已达机器人数量上限」「消息推送过于频繁」 |

业务侧应对 `-10000` 做重新登录/换令牌处理；对其余非 0 展示 `msg` 或映射为业务错误码。

---

## 1. 管理 API（用户令牌）

以下接口 **禁止** 使用 `rbt_` 机器人令牌。

### 1.1 机器人列表

**`POST /robot/api/v1/list`**

| 项 | 值 |
|----|-----|
| 请求 `data` | 可省略或 `{}` |
| 权限 | 当前登录用户（令牌对应用户） |

**响应 `data`（`RobotListResult`）**

```json
{
  "list": [
    {
      "uuid": "32位机器人uuid",
      "nickname": "展示名",
      "icon": "头像URL",
      "status": 1,
      "robot": true
    }
  ]
}
```

说明：列表为 **`status >= 0`** 的机器人（含停用）；不含软删（-1）与已销毁（-2）。

---

### 1.2 创建机器人

**`POST /robot/api/v1/create`**

**请求 `data`（`RobotCreateRequest`）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 展示名称 |
| `description` | string | 否 | 描述 |
| `icon` | string | 否 | 头像 URL |
| `tokenExpireDays` | int | 否 | 首个令牌有效天数；空则默认 **365** |

**响应 `data`（`RobotCreateResult`）**

| 字段 | 类型 | 说明 |
|------|------|------|
| `robot` | User | 机器人概要（`robot=true`） |
| `token` | string | **首个 `rbt_` 明文令牌，仅返回一次** |

**副作用**：触发 Hook 事件 `robot.created`（若已配置 Hook）。

---

### 1.3 停用 / 启用 / 删除 / 销毁机器人

**`POST /robot/api/v1/disable`**  
**`POST /robot/api/v1/enable`**  
**`POST /robot/api/v1/delete`**  
**`POST /robot/api/v1/destroy`**

**请求 `data`（`RobotUuidRequest`）**

| 字段 | 类型 | 必填 |
|------|------|------|
| `uuid` | string | 是，机器人 uuid |

**响应**：成功时 `data` 常为 `"success"`。

| 操作 | 机器人 status | 令牌 | Hook |
|------|---------------|------|------|
| disable | 0 | 全部作废 | 保留 |
| enable | 1 | 不自动恢复 | 保留 |
| delete | -1 软删（**仅主人**） | 作废 | **删除全部 Hook 行** |
| destroy | -2 硬销毁（**仅系统管理员**） | 作废 | **删除全部 Hook 行** |

- **delete**：用户侧唯一删除入口；记录 `delete_time`，从列表隐藏，**立即释放创建配额**（`usedRobots` / `assertRobotQuota` 不再计入）；**不可 enable 恢复**；后台保留 `deletedRetentionDays` 天供审计/管理员 `destroy` 或定时硬销毁。
- **停用**：`status=0` 仍占用配额，可 `enable` 恢复。
- **destroy**：要求机器人已为软删（`status=-1`）；非管理员调用将拒绝。
- **定时任务**：软删除超过 **`RobotQuotaConfig.deletedRetentionDays`（默认 30）** 的机器人由 **`RobotService`** 每日任务（`LoopJob.OneDay`）自动硬销毁。
- **软删门禁**：超过 **`RobotQuotaConfig.maxSoftDeletePending`（默认 5）** 禁止 `create`，降至该值及以下可恢复。

Hook 事件：`robot.disabled` / `robot.enabled` / `robot.deleted` / `robot.destroyed`。

---

### 1.4 Hook 列表

**`POST /robot/api/v1/hook/list`**

**请求 `data`（`RobotUuidRequest`）**

| 字段 | 必填 | 说明 |
|------|------|------|
| `uuid` | 是 | **机器人** uuid（非 Hook uuid） |

**响应 `data`（`RobotHookListResult`）**

```json
{
  "hooks": [
    {
      "uuid": "hook业务uuid",
      "robot": "机器人uuid",
      "owner": "用户uuid",
      "name": "名称",
      "callbackUrl": "https://example.com/hook",
      "events": "order.paid,*",
      "description": "",
      "status": 1,
      "createTime": "2025-01-01T00:00:00+08:00",
      "updateTime": "...",
      "lastInvokeTime": "..."
    }
  ]
}
```

**不返回 `secret` 明文。**

---

### 1.5 创建 Hook

**`POST /robot/api/v1/hook/create`**

**请求 `data`（`RobotHookCreateRequest`）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `robot` | string | 是 | 机器人 uuid |
| `name` | string | 是 | Hook 名称 |
| `callbackUrl` | string | 是 | 公网 `http`/`https`；禁止内网/本机 |
| `secret` | string | 否 | 签名密钥；**强烈建议传入并自行保存** |
| `events` | string | 否 | 订阅事件 CSV 或 `*`；默认 `*` |
| `description` | string | 否 | 说明 |

**响应 `data`**：`RobotHookView`（不含 `secret`）。

> 若 `secret` 留空，服务端会自动生成随机密钥，但 **响应不返回明文**。对接方无法验签除非自行在请求中传入 `secret`。

---

### 1.6 更新 Hook

**`POST /robot/api/v1/hook/update`**

**请求 `data`（`RobotHookUpdateRequest`）**

| 字段 | 必填 | 说明 |
|------|------|------|
| `uuid` | 是 | Hook uuid |
| `name` / `callbackUrl` / `secret` / `events` / `description` | 否 | 非空字段才更新 |

---

### 1.7 删除 / 停用 / 启用 Hook

**`POST /robot/api/v1/hook/delete`**  
**`POST /robot/api/v1/hook/disable`**  
**`POST /robot/api/v1/hook/enable`**

**请求 `data`（`RobotHookUuidRequest`）**

| 字段 | 必填 |
|------|------|
| `uuid` | Hook uuid |

---

### 1.8 令牌列表

**`POST /robot/api/v1/token/list`**

**请求 `data`（`RobotUuidRequest`）**：机器人 uuid。

**响应 `data`（`RobotTokenListResult`）**

| 字段 | 说明 |
|------|------|
| `tokens` | `RobotTokenItemView[]`（仅元数据，无明文） |
| `usedRows` | 当前 **有效** 令牌数（status=1） |
| `maxRows` | 有效令牌上限 |

`RobotTokenItemView` 字段：`uuid`, `robot`, `tokenPrefix`（`rbt_`+前 12 位）, `status`, `expireTime`, `updateTime`, `lastUsedTime`。

---

### 1.9 创建 / 作废 / 轮换令牌

**`POST /robot/api/v1/token/create`**

**请求 `data`（`RobotTokenCreateRequest`）**

| 字段 | 必填 | 说明 |
|------|------|------|
| `uuid` | 是 | 机器人 uuid |
| `tokenExpireDays` | 否 | 有效天数 |

**响应 `data`（`RobotTokenResult`）**：`{ "token": "rbt_..." }` 明文仅返回一次。

**`POST /robot/api/v1/token/revoke`**

**请求 `data`（`RobotTokenUuidRequest`）**：`uuid` = **令牌行** uuid（非机器人 uuid）。物理删除行。

**`POST /robot/api/v1/token/rotate`**

**请求 `data`（`RobotRotateTokenRequest`）**：同 create（`uuid` 为机器人 uuid）。

满额时仅删除 **已作废** 令牌行；仍满则失败，**不会删除仍在使用的有效令牌**。

---

### 1.10 配额查询与保存

**`POST /robot/api/v1/config/get`**

**请求 `data`（`RobotConfigUserRequest`，可空）**

| 字段 | 说明 |
|------|------|
| `uuid` | 目标用户 uuid；空则查当前操作者 |

非本人且非系统管理员 → 失败。

**响应 `data`（`RobotConfigResult`）**

| 字段 | 说明 |
|------|------|
| `uuid` | 用户 uuid |
| `maxRobots` / `maxTokens` / `maxHooks` | 配置值，**-1** 表示继承全局 |
| `usedRobots` | 占用配额的机器人数（**含停用，不含软删/销毁**） |
| `pendingSoftDeleted` | 软删中（`status=-1`）机器人数 |
| `softDeleteCreateBlocked` | 为 `true` 时不可 `create`（软删数 > `effectiveMaxSoftDeletePending`） |
| `effectiveMaxSoftDeletePending` | 全局软删门禁上限（默认 5） |
| `effectiveDeletedRetentionDays` | 全局软删保留天数（默认 30） |
| `effectiveMaxRobots` / `effectiveMaxTokens` / `effectiveMaxHooks` | 合并全局后的实际上限 |

**`POST /robot/api/v1/config/save`**

**权限**：**系统管理员**（`SysUserRoleService.isSystemAdministrator`）。

**请求 `data`（`RobotConfigSaveRequest`）**

| 字段 | 说明 |
|------|------|
| `uuid` | 目标用户 uuid |
| `maxRobots` / `maxTokens` / `maxHooks` | 至少填一项；**-1** 继承；**>0** 为具体上限 |

---

## 2. 入站 API（机器人令牌）

### 2.1 推送消息

**`POST /robot/api/v1/message/push`**

| 项 | 值 |
|----|-----|
| 鉴权 | **必须** `rbt_` 机器人令牌 |
| 幂等头 | 可选 `X-Robot-Message-Id: <key>` |
| 权限 | 机器人 `scopes` 非空时需含 `message.push` 或 `*` |

**请求 `data`（`RobotMessagePushRequest`）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | string | 是 | 消息类型，正则 `^[a-z][a-z0-9._-]{0,63}$` |
| `data` | object 或 string | 否 | 业务 JSON；可传对象或 JSON 字符串 |
| `messageId` | string | 否 | 幂等键 `^[a-zA-Z0-9._-]{8,64}$`；若提供则作为对外 `messageId` |

**请求示例**

```json
{
  "data": {
    "type": "order.paid",
    "messageId": "idem-20250526-0001",
    "data": {
      "orderId": "O10001",
      "amount": 199.0
    }
  }
}
```

**响应 `data`（`RobotMessagePushResult`）**

| 字段 | 类型 | 说明 |
|------|------|------|
| `messageId` | string | 消息 id |
| `type` | string | 消息类型 |
| `queued` | boolean | `true` 已入异步队列 |
| `duplicate` | boolean | `true` 幂等命中，**未再次入队** |

**限制**

| 限制项 | 值 |
|--------|-----|
| 载荷大小 | 规范化后 JSON ≤ **256KB**（UTF-8） |
| 限流 | 默认每机器人 **60 次/分钟**（`ROBOT_QUOTA_CONFIG.maxMessagePushPerMinute`，0=不限） |
| 幂等缓存 | 默认 **24 小时**（`messageIdempotencyHours`） |

**异步语义**：`code=0` 且 `queued=true` 仅表示入队成功；JVM `RobotMessageSubscriber` 与出站 Hook 在队列消费后执行，**非同步完成**。

---

## 3. 出站 Hook 回调（业务系统实现 HTTP 服务端）

Autumn **主动 POST** 到创建 Hook 时登记的 `callbackUrl`。非开放 API，但对接方必须实现。

### 3.1 请求

```http
POST {callbackUrl}
Content-Type: application/json;charset=UTF-8
X-Robot-Event: order.paid
X-Robot-Timestamp: 1714000000123
X-Robot-Signature: <hex>
```

Body 为 JSON 字符串（用于签名），结构：

```json
{
  "event": "order.paid",
  "robot": "<机器人uuid>",
  "timestamp": 1714000000123,
  "data": { }
}
```

**`data` 两种形态**

1. **生命周期事件**（`robot.created` 等）：`data` 含 `owner`, `name`, `status` 等。  
2. **入站消息转发**（由 `message/push` 触发）：`data` 为：

```json
{
  "messageId": "...",
  "type": "order.paid",
  "owner": "<用户uuid>",
  "payload": { "orderId": "O10001" }
}
```

`payload` 为 **JSON 对象**（非字符串）。

### 3.2 签名验证

```
payload_to_sign = X-Robot-Timestamp + "." + <原始请求体字符串>
expected = HMAC_SHA256_hex(key = hook_secret, message = payload_to_sign)
```

`X-Robot-Signature` 须与 `expected` 一致（小写 hex）。

### 3.3 响应要求

- 返回 **HTTP 2xx** 视为投递成功。
- 非 2xx 将按 `hookDispatchRetries` 重试，耗尽后进死信队列。

---

## 4. 事件名一览

### 4.1 平台内置（生命周期）

| event | 说明 |
|-------|------|
| `robot.created` | 创建 |
| `robot.disabled` | 停用 |
| `robot.enabled` | 启用 |
| `robot.deleted` | 软删 |
| `robot.destroyed` | 销毁 |

### 4.2 业务入站（自定义）

由 `message/push` 的 `type` 指定，例如 `order.paid`、`ticket.created`。Hook `events` 填相同名称或 `*` 可接收。

### 4.3 订阅匹配

- `events` = `*` 或空：匹配全部。  
- 否则逗号分隔，**完全相等** 匹配（无通配前缀）。

---

## 5. 接口速查表

| 路径 | 令牌 | 说明 |
|------|------|------|
| `/list` | 用户 | 机器人列表 |
| `/create` | 用户 | 创建 + 首个 rbt_ |
| `/disable` `/enable` `/delete` | 用户 | 生命周期 |
| `/destroy` | **系统管理员** | 硬销毁（须先软删） |
| `/hook/list` | 用户 | Hook 列表 |
| `/hook/create` | 用户 | 创建 Hook |
| `/hook/update` | 用户 | 更新 Hook |
| `/hook/delete` `/disable` `/enable` | 用户 | Hook 管理 |
| `/token/list` | 用户 | 令牌列表 |
| `/token/create` | 用户 | 新建 rbt_ |
| `/token/revoke` | 用户 | 删除令牌行 |
| `/token/rotate` | 用户 | 轮换 rbt_ |
| `/config/get` | 用户 | 配额查询 |
| `/config/save` | 用户（管理员） | 配额保存 |
| `/message/push` | **机器人** | 入站消息 |

---

*接口实现以 `cn.org.autumn.modules.bot.controller.RobotApiController`、`RobotInboundApiController` 为准；升级 Autumn 版本后请核对发行说明。*
