# Autumn 安全专项附录（低频按需加载）

> 目标：将强校验与攻防演练从 `docs/AI_MAP.md` 拆分，避免日常开发上下文过大。
> 默认不加载；仅在“安全改造/联调/灰度/演练”任务中引用。

## 1. 客户端签名接入参考（SDK/拦截器生成基线）

### 1.1 请求签名输入与输出

- 输入：
  - `auth`：`X-Encrypt-Auth`（来自 `/rsa/api/v1/init`）
  - `agent`：特征值（优先注入 `User-Agent`，浏览器可放 `X-Encrypt-Agent`）
  - `method`：HTTP 方法（大写）
  - `uriWithQuery`：请求路径+query（不含域名）
  - `timestamp`：毫秒时间戳（建议 `Date.now()`）
  - `nonce`：一次性随机串（建议 UUID 或随机 16~32 字节）
- 输出请求头：
  - `X-Encrypt-Auth`
  - `X-Encrypt-Agent`（浏览器场景）
  - `X-Encrypt-Timestamp`
  - `X-Encrypt-Nonce`
  - `X-Encrypt-Signature`

### 1.2 JS 伪代码（浏览器/Node 通用思路）

```javascript
async function buildSecurityHeaders({ auth, agent, method, uriWithQuery }) {
  const timestamp = Date.now().toString();
  const nonce = crypto.randomUUID().replace(/-/g, "");
  const canonical = [
    method.toUpperCase(),
    uriWithQuery,
    timestamp,
    nonce,
    agent
  ].join("\n");

  // HMAC-SHA256(auth, canonical) -> hex
  const signature = hmacSha256Hex(auth, canonical);

  return {
    "X-Encrypt-Auth": auth,
    "X-Encrypt-Agent": agent,
    "X-Encrypt-Timestamp": timestamp,
    "X-Encrypt-Nonce": nonce,
    "X-Encrypt-Signature": signature
  };
}
```

### 1.3 Java 伪代码（服务端转发/网关客户端）

```java
public Map<String, String> buildHeaders(String auth, String agent, String method, String uriWithQuery) {
    String timestamp = String.valueOf(System.currentTimeMillis());
    String nonce = UUID.randomUUID().toString().replace("-", "");
    String canonical = method.toUpperCase() + "\n"
            + uriWithQuery + "\n"
            + timestamp + "\n"
            + nonce + "\n"
            + agent;
    String signature = hmacSha256Hex(auth, canonical);

    Map<String, String> headers = new HashMap<>();
    headers.put("X-Encrypt-Auth", auth);
    headers.put("X-Encrypt-Agent", agent);
    headers.put("X-Encrypt-Timestamp", timestamp);
    headers.put("X-Encrypt-Nonce", nonce);
    headers.put("X-Encrypt-Signature", signature);
    return headers;
}
```

### 1.4 服务端校验注意事项（与当前实现保持一致）

- 签名串必须严格按换行拼接：`METHOD \n URI(+query) \n timestamp \n nonce \n agent`。
- 时间窗默认 `120s`，客户端时间漂移过大将触发 `403`。
- `nonce` 在窗口期内只允许一次，重放请求会被拒绝。
- 备用头 `X-Encrypt-Agent` 仅用于浏览器无法改写 `User-Agent` 的场景。

### 1.5 联调排错清单（签名场景）

- `403` 且签名不通过时：
  - 检查 `uriWithQuery` 是否与服务端看到的 URI 完全一致（特别是 query 顺序与编码）。
  - 检查 `timestamp` 是否毫秒级，客户端机器时间是否漂移。
  - 检查 `nonce` 是否意外复用（重试逻辑中最常见）。
  - 检查 `agent` 取值是否与请求头实际值一致。
  - 检查 `auth` 是否已过期或被轮换。

## 2. 强校验灰度上线策略（推荐）

### 2.1 上线目标与原则

- 目标：在不影响正常用户请求前提下，逐步启用 `agent/auth + 防重放签名` 强校验。
- 原则：
  - 先观测后拦截
  - 先指定接口后全局接口
  - 先低风险环境后生产全量

### 2.2 开关顺序（建议按序）

- 第一步：仅下发特征值，不强制
  - 客户端已携带 `X-Encrypt-Auth`、`X-Encrypt-Agent`、签名三头
  - 服务端仅记录校验结果，不拦截
- 第二步：指定 URI 强校验
  - 选择高价值且调用可控接口（如敏感写接口）
  - 防护 URI 不使用 `"/"`，先精确到 1~3 条路径
- 第三步：模块级放量
  - 扩展到 `oauth/usr` 关键接口
- 第四步：全局强校验（可选）
  - 防护 URI 包含 `"/"` 前，确保客户端覆盖率和时钟同步已达标

### 2.3 观测指标（至少要有）

- 基础指标：强校验总请求数、强校验失败率、按失败类型拆分（时间窗/nonce/签名/特征缺失）
- 业务指标：核心接口成功率、登录/授权成功率、前端错误率与重试率
- 安全指标：重放命中次数、高频异常 IP 数、被拦截 URI TopN

### 2.4 放量节奏（参考）

- 环境节奏：Dev -> Test -> Staging -> Prod
- 生产节奏示例：Day1 5% -> Day2 20% -> Day3 50% -> Day4 100%（指定接口）

### 2.5 回滚与应急

- 快速回滚：关闭强力模式（或移除目标 URI）立即恢复非强制校验
- 降级策略：异常客户端暂时降为“仅特征校验，不做签名校验”
- 应急排查优先级：时间漂移 -> URI/query canonical 不一致 -> nonce 复用 -> 旧客户端未带签名头

### 2.6 AI 执行要求（上线相关）

- AI 给上线方案必须包含：开关顺序、观测指标、回滚路径、风险阈值（如 403 失败率阈值）

## 3. 安全演练用例集（重放/伪造/漂移/篡改）

### 3.1 用例格式（统一）

- 每个用例输出四项：攻击场景、构造方式、预期结果、排查点

### 3.2 基础对照组（必须先通过）

- `CASE-BASE-001`：正常明文调用（强力模式关闭）-> 预期 200
- `CASE-BASE-002`：正常签名调用（强力模式开启且命中防护 URI）-> 预期 200
- `CASE-BASE-003`：非防护 URI 调用（强力模式开启）-> 预期按常规逻辑处理

### 3.3 重放攻击演练

- `CASE-REPLAY-001`：相同 `nonce` 重放同一请求 -> 首次 200，第二次 403
- `CASE-REPLAY-002`：仅修改 `timestamp`，复用旧签名 -> 403

### 3.4 签名伪造演练

- `CASE-SIGN-001`：篡改 query 参数，不重签名 -> 403
- `CASE-SIGN-002`：签名按 GET，实际发 POST -> 403
- `CASE-SIGN-003`：错误 `auth` 参与签名 -> 403

### 3.5 时钟漂移演练

- `CASE-TIME-001`：客户端时间超前 5 分钟 -> 403
- `CASE-TIME-002`：客户端时间滞后 5 分钟 -> 403
- `CASE-TIME-003`：客户端时间在 ±30 秒 -> 200

### 3.6 特征值伪造演练

- `CASE-AGENT-001`：缺失 `X-Encrypt-Auth` -> 403
- `CASE-AGENT-002`：`auth` 正确，但 `User-Agent/X-Encrypt-Agent` 都不匹配 -> 403
- `CASE-AGENT-003`：浏览器仅传 `X-Encrypt-Agent` 且匹配 -> 200

### 3.7 豁免与边界演练

- `CASE-BYPASS-001`：`/rsa/api/v1/init` -> 不被强校验阻断
- `CASE-BYPASS-002`：`/wall/api/*` -> 不被强校验阻断
- `CASE-BYPASS-003`：非 JSON 下载/流式接口 -> 不因响应加密逻辑破坏语义

### 3.8 压测与告警阈值建议

- 压测维度：QPS 100/500/1000 下失败率、nonce 去重 map 大小、403 峰值与误拦率
- 告警阈值（可按环境微调）：
  - 强校验失败率 > 5% 持续 5 分钟
  - 重放命中数突增（环比 > 300%）
  - 核心接口成功率下降 > 2%

### 3.9 AI 产物要求（演练任务）

- AI 生成演练方案必须包含：用例编号与目标、请求构造脚本/伪代码、通过失败判定、失败定位路径
