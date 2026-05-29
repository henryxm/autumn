# 支付密码与安全凭证（safe 模块）

> 适用 Autumn **2.0.0**。收单/扣款在业务仓；本模块负责支付 PIN、手势、生物绑定与支付前校验。  
> 实现：`cn.org.autumn.modules.safe`；开放入口：`PayCredentialApiController`（`POST /safe/api/v1/*`）。  
> 表名/类名：**`docs/AI_BOOT.md` §3.2**、**`docs/AI_STANDARDS.md` §9**。

## 1. 模块一览

| 项 | 值 |
|----|-----|
| 基础路径 | `{ORIGIN}/safe/api/v1`（无尾斜杠）；**25** 个 `POST` |
| 鉴权 | 用户访问令牌 + `@Authenticated`（**禁止** `rbt_`） |
| 配置 | `PAY_CREDENTIAL_CONFIG`（`PayCredentialConfig`）；模块内 **`SafeConfig.get()`** |
| Shiro | `SafeConfig` 注册 `/safe/api/v1/**` → `anon`（开放 API 自带 `@Authenticated`） |
| 对内 SPI | `PayPinVerifier` ← `PayUserPinService` |
| 重置 / 闸门 SPI | `PayCredentialResetVerifier`；`PayGateRiskContributor` |

**配置项（后台「支付凭证」）**

| 字段 | 默认 | 说明 |
|------|------|------|
| `pinLength` | 6 | 支付密码位数（全数字） |
| `maxFailAttempts` | 5 | 连续失败锁定阈值 |
| `lockMinutes` | 30 | 锁定时长 |
| `verifyTokenMinutes` | 5 | `verify` 成功后 `verifyToken` 有效分钟 |
| `challengeMinutes` | 5 | 生物 `challenge` 有效分钟 |
| `gestureMinPoints` | 4 | 手势最少连接点数 |
| `maxBiometricDevices` | 0 | 每用户设备上限，0=不限 |
| `auditLogEnabled` | true | 是否写 `safe_pay_credential_log` |
| `logRetentionDays` | 180 | 审计日志保留天数，`≤0` 不自动清理 |
| `gateEnabled` | true | 是否启用支付闸门（须先 assess） |
| `passwordlessEnabled` | true | 是否启用小额免密 |
| `passwordlessMaxAmountCent` | 1000 | 免密金额上限（分），1000=10元 |
| `passwordlessWindowMinutes` | 15 | 最近一次校验成功后的免密窗口 |
| `gateTokenMinutes` | 5 | `gateToken` 有效分钟 |
| `highAmountThresholdCent` | 50000 | 超过该金额强制输支付密码 |
| `duplicateAmountWindowMinutes` | 10 | 短时相同金额检测窗口 |
| `duplicateAmountAlertCount` | 2 | 窗口内同额次数达阈则强制输密码 |
| `passwordlessRequireTrustedDevice` | false | 免密是否须常用设备 |
| `passwordlessRequireTrustedIp` | false | 免密是否须常用 IP |
| `auditGateEnabled` | true | assess 摘要写入 `safe_pay_credential_log`（`method=GATE`） |
| `verifyTokenBindAmount` | true | 业务 `requireVerifyToken` 校验金额/订单 |
| `newDeviceRequirePassword` | true | 非常用设备强制输密 |
| `passwordlessDailyMaxCount` | 0 | 每日免密次数上限，0=不限 |
| `passwordlessDailyMaxAmountCent` | 0 | 每日免密金额上限（分），0=不限 |
| `clientTimeSkewSeconds` | 300 | 客户端时间与服务器偏差阈值 |

用户级覆盖：`safe_pay_user_security_setting`（`POST /security/settings/update`），含 `gesturePaymentEnabled`（默认 **false**，开启后闸门才允许 `GESTURE` 校验）。

**对接指南**：`docs/AI_SAFE_CREDENTIAL_INTEGRATION.md`（Quick Start、业务仓示例、安全清单）。

### 1.1 自旧 `pay` 模块迁移

| 旧 | 新 |
|----|-----|
| `cn.org.autumn.modules.pay` | `cn.org.autumn.modules.safe` |
| `/pay/api/v1/*` | `/safe/api/v1/*` |
| `pay_*` 表 | `safe_pay_*` |

框架**不自动迁数据**；DBA 一次性脚本迁移后，业务仓改调 safe API 与 `PayPinVerifier`。

---

## 2. 通用约定

### 2.1 HTTP

| 项 | 值 |
|----|-----|
| Method | 全部为 **`POST`** |
| Content-Type | `application/json;charset=UTF-8` |
| 字符编码 | UTF-8 |

### 2.2 请求包装 `Request<T>`

业务字段放在 **`data`** 中：

```json
{
  "data": { }
}
```

`status`、`list` 等无业务字段接口可省略 `data` 或传 `{}`。

### 2.3 响应包装 `Response<T>`

```json
{
  "code": 0,
  "msg": "success",
  "data": { }
}
```

| 字段 | 说明 |
|------|------|
| `code` | **`0` 成功**；非 0 失败（业务错误见 §8） |
| `msg` | 提示文案 |
| `data` | 业务数据；无体接口成功时可能为 `null` 或省略 |

### 2.4 鉴权

须为**当前登录人类用户**令牌（与 `usr` 模块一致），任选其一：

```http
Token: <用户访问令牌>
Authorization: Bearer <用户访问令牌>
```

| 场景 | code | msg 示例 |
|------|------|----------|
| 未登录 / 令牌无效 | `-10000` | 请登录 |
| 机器人令牌 | `-10000` | 请使用用户令牌 |
| 用户 UUID 不可用 | `-10000` | 用户不可用 |

Shiro 对 `/safe/api/v1/**` 为 `anon`，实际鉴权由 **`@Authenticated`** + `UserContext` 完成。

### 2.5 安全建议

- PIN、手势、生物私钥相关字段建议走 **`docs/AI_CRYPTO.md`** 传输加密（`ciphertext` + `session` / `X-Encrypt-Session`）。
- 服务端仅存 PIN/手势**哈希**与设备**公钥**，不回传明文密码。
- `verifyToken` 为**一次性**短期令牌，供业务仓 `PayPinVerifier.requireVerifyToken` 消费。

### 2.6 推荐调用顺序（含支付闸门）

**单笔支付（启用 `gateEnabled` 时）**

1. `POST /gate/assess` — 提交金额（**分**）、设备、地点、理由、环境等  
2. 若 `authorized=false`：展示 `reasons`，终止  
3. 若 `authMode=PASSWORDLESS`：业务侧 `requireGateToken(userUuid, gateToken, amountCent)` 后扣款（免输 PIN）  
4. 若 `authMode=PASSWORD_REQUIRED`：`pin/verify` 或 `biometric/verify`（须带 **`gateToken` + `amountCent`**）→ `verifyToken` → 业务 `requireVerifyToken` 或 `requireGateToken`  
5. 扣款成功后可选 `security/device/trust`、`security/ip/trust` 固化常用环境  

**支付密码（设置与维护）**

1. `pin/status` → 是否已设置、是否锁定  
2. 未设置：`pin/set`；已设置改密：`pin/change`；忘记：`pin/reset`（需登录密码等身份校验）  
3. 支付前（无闸门或 assess 后）：见上「单笔支付」  

**手势**（与 PIN 独立，可并行开通）

1. `gesture/status` → `gesture/set` / `change` / `reset` / `verify`（`verify` 同样返回 `verifyToken`）

**生物识别**

1. `biometric/register`（上传设备公钥）  
2. `biometric/challenge` → 客户端用私钥对 `challenge` 做 **SHA256withRSA** 签名  
3. `biometric/verify` → `verifyToken`  
4. 解绑：`biometric/revoke`；列表：`biometric/list`

---

## 3. 支付密码 API

### 3.1 查询状态 — `POST /safe/api/v1/pin/status`

| 项 | 值 |
|----|-----|
| 请求 `data` | 可省略 |
| 响应 `data` | `PayPinStatusResult` |

```json
{
  "set": true,
  "locked": false,
  "remainingAttempts": 5
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `set` | boolean | 是否已设置支付密码 |
| `locked` | boolean | 是否处于锁定态 |
| `remainingAttempts` | int | 当前剩余可尝试次数（锁定前） |

---

### 3.2 首次设置 — `POST /safe/api/v1/pin/set`

**请求 `data`（`PayPinSetRequest`）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pin` | string | 是 | 支付密码，长度见配置 `pinLength`，全数字 |
| `confirm` | string | 是 | 与 `pin` 一致 |

**响应**：`data` 常为 `null`（成功即可）。

**错误**：`839` 已设置；`843` 格式；`842` 弱密码；`840` 两次不一致。

---

### 3.3 修改 — `POST /safe/api/v1/pin/change`

**请求 `data`（`PayPinChangeRequest`）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `oldPin` | string | 是 | 原支付密码 |
| `newPin` | string | 是 | 新密码 |
| `confirm` | string | 是 | 确认新密码 |

**错误**：`838` 未设置；`841` 锁定；`840` 原密码错误；`843`/`842` 新密码校验失败。

---

### 3.4 重置 — `POST /safe/api/v1/pin/reset`

忘记支付密码时使用；须通过 **重置身份 SPI**（默认校验**登录密码**）。

**请求 `data`（`PayPinResetRequest`）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `newPin` | string | 是 | 新支付密码 |
| `confirm` | string | 是 | 确认新密码 |
| `loginPassword` | string | 条件 | 登录密码（默认校验器） |
| `smsCode` | string | 条件 | 短信码（需业务实现 `PayCredentialResetVerifier`） |

**错误**：登录失败、无可用校验方式 → 见 `Error` 枚举；密码规则同 `set`。

---

### 3.5 校验 — `POST /safe/api/v1/pin/verify`

支付或敏感操作前校验 PIN，成功返回短期令牌。

**请求 `data`（`PayPinVerifyRequest`）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pin` | string | 是 | 支付密码 |
| `gateToken` | string | 条件 | `gate/assess` 返回；启用闸门时必传 |
| `amountCent` | long | 条件 | 与 assess 一致（分） |

**响应 `data`（`PayPinVerifyResult`）**

```json
{
  "verifyToken": "32位随机字符串"
}
```

| 字段 | 说明 |
|------|------|
| `verifyToken` | 一次性令牌，有效时间见 `verifyTokenMinutes`；业务侧调用 `PayPinVerifier.requireVerifyToken(userUuid, token)` |

**错误**：`838`/`841`/`840`；失败累计达上限触发 `841` 并写审计 `LOCK`。

---

## 4. 手势密码 API

九宫格编号 **0～8**（左上为 0，右下为 8）。轨迹为**按顺序**经过的点，**不可重复经过同一点**（连续相同点非法）。最少点数见 `gestureMinPoints`（默认 4）。

存储为规范化串，例如 `[0,1,2,5]` → `"0-1-2-5"`。

### 4.1 状态 — `POST /safe/api/v1/gesture/status`

同 `PayGestureStatusResult`，字段含义与 `pin/status` 相同。

### 4.2 设置 — `POST /safe/api/v1/gesture/set`

**请求 `data`（`PayGestureSetRequest`）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `points` | int[] | 是 | 手势轨迹 |
| `confirmPoints` | int[] | 是 | 确认轨迹，须与 `points` 规范化后一致 |

### 4.3 修改 — `POST /safe/api/v1/gesture/change`

**请求 `data`（`PayGestureChangeRequest`）**

| 字段 | 类型 | 必填 |
|------|------|------|
| `oldPoints` | int[] | 是 |
| `newPoints` | int[] | 是 |
| `confirmPoints` | int[] | 是 |

### 4.4 重置 — `POST /safe/api/v1/gesture/reset`

**请求 `data`（`PayGestureResetRequest`）**

| 字段 | 类型 | 必填 |
|------|------|------|
| `points` | int[] | 是 |
| `confirmPoints` | int[] | 是 |
| `loginPassword` | string | 是（默认 SPI） |

### 4.5 校验 — `POST /safe/api/v1/gesture/verify`

**请求 `data`（`PayGestureVerifyRequest`）**

| 字段 | 类型 | 必填 |
|------|------|------|
| `points` | int[] | 是 |
| `gateToken` | string | 条件 | 启用闸门或已 assess 时必传 |
| `amountCent` | long | 条件 | 与 assess 一致（分） |

**响应 `data`**：`PayPinVerifyResult`（含 `verifyToken`）。须用户开启 `gesturePaymentEnabled`。

**错误**：`844` 手势无效；`852` 未开启手势支付；`851`/`849`/`850` 闸门相关；锁定/未设置等复用 PIN 码（`838`/`841`/`839`）。

---

## 5. 生物识别 API

客户端生成 RSA 密钥对；注册时上传 **X.509 SubjectPublicKeyInfo** 的 **Base64** 公钥。验签算法：**SHA256withRSA**，对 **`challenge` 的 UTF-8 字节**签名，**signature** 为 Base64。

### 5.1 注册/更新 — `POST /safe/api/v1/biometric/register`

**请求 `data`（`PayBiometricRegisterRequest`）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `deviceId` | string | 是 | 业务侧设备唯一 ID |
| `platform` | string | 否 | 如 `ios` / `android` |
| `credentialId` | string | 否 | 凭据标识（Passkey 等） |
| `publicKey` | string | 是 | Base64 公钥 |

同一 `deviceId` 再次注册视为**更新公钥**。超 `maxBiometricDevices` 时拒绝。

### 5.2 设备列表 — `POST /safe/api/v1/biometric/list`

**请求 `data`**：可省略。  
**响应 `data`**：`PayBiometricDeviceView[]`

| 字段 | 说明 |
|------|------|
| `uuid` | 记录业务主键 |
| `deviceId` | 设备 ID |
| `platform` | 平台 |
| `credentialId` | 凭据 ID |
| `lastUsedTime` | 最近验签成功时间 |
| `createTime` | 绑定时间 |

（不返回公钥。）

### 5.3 吊销 — `POST /safe/api/v1/biometric/revoke`

**请求 `data`（`PayBiometricDeviceRequest`）**：`deviceId`

### 5.4 挑战 — `POST /safe/api/v1/biometric/challenge`

**请求 `data`（`PayBiometricDeviceRequest`）**：`deviceId`  
**响应 `data`（`PayBiometricChallengeResult`）**

```json
{
  "deviceId": "my-device-1",
  "challenge": "随机挑战串"
}
```

挑战须在 `challengeMinutes` 内用于 `verify`，且**单次消费**。

### 5.5 验签 — `POST /safe/api/v1/biometric/verify`

**请求 `data`（`PayBiometricVerifyRequest`）**

| 字段 | 类型 | 必填 |
|------|------|------|
| `deviceId` | string | 是 |
| `challenge` | string | 是 | 来自 challenge 接口 |
| `signature` | string | 是 | Base64 签名 |
| `gateToken` | string | 条件 | 启用闸门时必传 |
| `amountCent` | long | 条件 | 与 assess 一致（分） |

**响应 `data`**：`PayPinVerifyResult`（`verifyToken`）。

**错误**：`845` 设备未注册；`846` 验签失败或 challenge 无效；`847` 令牌无效（消费 verifyToken 时）。

---

## 6. 支付安全设置 API

| 路径 | 说明 |
|------|------|
| `POST .../security/status` | 生效策略、`passwordlessWindowActive` / `passwordlessRemainingSeconds`、`gesturePaymentEnabled`、常用设备/IP |
| `POST .../security/settings/update` | 免密开关/上限/窗口、`gesturePaymentEnabled` |
| `POST .../security/device/trust` | 标记常用设备 |
| `POST .../security/device/untrust` | 取消常用设备 |
| `POST .../security/ip/trust` | 标记常用 IP（`ip` 空则用当前请求 IP） |
| `POST .../security/ip/untrust` | 取消常用 IP |

---

## 7. 支付闸门 API

### 7.1 评估 — `POST /safe/api/v1/gate/assess`

**请求 `data`（`PayGateAssessRequest`）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `amountCent` | long | 是 | 支付金额（**分**） |
| `currency` | string | 否 | 默认 CNY |
| `reason` | string | 否 | 支付理由/商品说明 |
| `orderId` | string | 否 | 业务订单号 |
| `merchantId` | string | 否 | 商户标识 |
| `payScene` | string | 否 | 场景，如 APP/H5/扫码 |
| `deviceId` | string | 否 | 稳定设备 ID |
| `deviceFingerprint` | string | 否 | 设备指纹 |
| `platform` | string | 否 | ios/android 等 |
| `location` | string | 否 | 城市或 `lat,lng` |
| `environment` | string | 否 | 环境 JSON 摘要 |
| `clientTime` | long | 否 | 客户端时间戳 |

服务端会读取请求 **IP**、**User-Agent** 参与评估；已注册生物识别的 `deviceId` 视为已知设备。

**响应 `data`（`PayGateAssessResult`）**

| 字段 | 说明 |
|------|------|
| `authorized` | 是否允许进入下一步（输密或免密扣款） |
| `authMode` | `DENIED` / `PASSWORD_REQUIRED` / `PASSWORDLESS` |
| `needPassword` | 是否须校验支付密码 |
| `passwordlessEligible` | 本次是否满足免密条件 |
| `gateToken` | 授权通过时返回，短时有效 |
| `trustedDevice` / `trustedIp` | 是否常用设备/IP |
| `effectivePasswordlessMaxCent` | 生效免密上限（分） |
| `effectivePasswordlessWindowMinutes` | 生效免密窗口（分钟） |
| `reasons` | 拒绝原因列表（`authorized=false`） |
| `warnings` | 风险提示（如非常用设备、短时同额） |
| `allowedVerifyMethods` | 本次允许的校验方式：`PIN`、`BIO`；用户开启手势支付时含 `GESTURE` |

**评估规则摘要**

- PIN 已锁定 → `DENIED`  
- 金额 &gt; `highAmountThresholdCent`、短时同额过多、**非常用设备**（`newDeviceRequirePassword`）→ `PASSWORD_REQUIRED`  
- 免密：`PASSWORDLESS` 须全局+用户开启、金额 ≤ 上限、**免密窗口内**（PIN/BIO/手势 verify 成功后刷新）、可选常用设备/IP；受日累计次数/金额限制  
- 内置风控：`DuplicateOrderRiskContributor`（同 `orderId` 短时重复）、`AbnormalTimeRiskContributor`（`clientTime` 偏差）  
- 扩展：实现 `PayGateRiskContributor` 追加 `reasons`  
- `auditGateEnabled` 时 assess 摘要写入 `safe_pay_credential_log`（`method=GATE`）

```json
{
  "authorized": true,
  "authMode": "PASSWORD_REQUIRED",
  "needPassword": true,
  "passwordlessEligible": false,
  "gateToken": "32位随机串",
  "trustedDevice": false,
  "trustedIp": true,
  "effectivePasswordlessMaxCent": 1000,
  "effectivePasswordlessWindowMinutes": 15,
  "reasons": [],
  "warnings": ["当前设备非常用支付设备"],
  "allowedVerifyMethods": ["PIN", "BIO"]
}
```

`allowedVerifyMethods`：用户开启 `gesturePaymentEnabled` 时含 `GESTURE`。`gateToken`、`verifyToken` **单次消费**；`verifyToken` 可绑定 `amountCent`/`orderId`（`verifyTokenBindAmount`）。

---

## 8. 业务错误码（838～852）

| code | 枚举 | 说明 |
|------|------|------|
| 838 | `PAY_PIN_NOT_SET` | 未设置支付密码/手势 |
| 839 | `PAY_PIN_ALREADY_SET` | 已设置（重复 set） |
| 840 | `PAY_PIN_MISMATCH` | 密码不正确或确认不一致 |
| 841 | `PAY_PIN_LOCKED` | 已锁定 |
| 842 | `PAY_PIN_WEAK` | 弱密码（如 123456） |
| 843 | `PAY_PIN_FORMAT` | 格式不符（位数/非数字） |
| 844 | `PAY_GESTURE_INVALID` | 手势无效 |
| 845 | `PAY_BIOMETRIC_NOT_FOUND` | 设备未注册 |
| 846 | `PAY_BIOMETRIC_VERIFY_FAILED` | 生物验签失败 |
| 847 | `PAY_VERIFY_TOKEN_INVALID` | 校验令牌无效或已过期 |
| 848 | `PAY_GATE_DENIED` | 支付未通过安全评估 |
| 849 | `PAY_GATE_TOKEN_INVALID` | 闸门令牌无效或已过期 |
| 850 | `PAY_GATE_AMOUNT_MISMATCH` | 支付金额与闸门评估不一致 |
| 851 | `PAY_GATE_REQUIRED` | 请先完成支付安全评估 |
| 852 | `PAY_GESTURE_PAYMENT_DISABLED` | 未开启手势支付 |

鉴权类 **`-10000`** 见 §2.4。

---

## 9. 业务仓对接（服务端）

```java
@Autowired
private PayPinVerifier payPinVerifier;

// 方式 A：直接校验 PIN
payPinVerifier.requireVerified(userUuid, pin);

// 方式 B：客户端先 assess → verify，业务提交 verifyToken（建议带金额/订单）
payPinVerifier.requireVerifyToken(userUuid, verifyToken, amountCent, orderId);

// 方式 C：assess 通过后（含 PASSWORDLESS），扣款前校验并消费 gateToken
payPinVerifier.requireGateToken(userUuid, gateToken, amountCent);
```

扩展重置：`PayCredentialResetVerifier`；扩展闸门：`PayGateRiskContributor`（对接示例见 **`docs/AI_SAFE_CREDENTIAL_INTEGRATION.md` §6**）。

---

## 10. 数据表（实现参考）

| 实体 | 表名 |
|------|------|
| `PayUserPinEntity` | `safe_pay_user_pin` |
| `PayUserGestureEntity` | `safe_pay_user_gesture` |
| `PayUserBiometricEntity` | `safe_pay_user_biometric` |
| `PayCredentialLogEntity` | `safe_pay_credential_log` |
| `PayUserSecuritySettingEntity` | `safe_pay_user_security_setting` |
| `PayUserTrustedDeviceEntity` | `safe_pay_user_trusted_device` |
| `PayUserTrustedIpEntity` | `safe_pay_user_trusted_ip` |
| `PayGateAttemptEntity` | `safe_pay_gate_attempt` |

站点：`SafeConfig`（配置 + Shiro）、`SafeMenu` / `SafePages`（后台 gen，非开放 API）。

---

## 11. 相关文档

- **`docs/AI_CRYPTO.md`**：传输加密  
- **`docs/AI_DISTRIBUTED_LOCK.md`**：凭证操作分布式锁  
- **`docs/AI_ASYNC_TASK.md`**：勿与 `BaseQueueService` 持久化队列混淆  
- **`docs/AI_ROBOT_API.md`**：`Request`/`Response` 风格可参考
