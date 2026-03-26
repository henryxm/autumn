# HTTP 透明代理说明文档

## 1. 概述

本模块提供**通用透明反向代理**能力，将客户端请求原样转发到指定上游（如 AI 大模型 OpenAPI），并将上游响应原样返回客户端。适用于对接需要固定入口、统一鉴权或跨域的前端，而实际请求由后端转发到第三方 API（如 OpenAI 兼容接口、阿里云 DashScope 等）的场景。

### 1.1 特性

- **透明转发**：请求体、响应体默认以流式读写；**例外**见下文「OpenAI 流式对话与 `stream_options`」。
- **全方法支持**：GET、POST、PUT、DELETE、PATCH、HEAD、OPTIONS、TRACE。
- **流式友好**：支持 SSE、长轮询等长时间流式响应；客户端中途断开时不会误报为服务端错误。
- **请求头透传**：Cookie、Authorization、Content-Type 等按需转发，并自动附加 X-Forwarded-For、X-Real-IP、X-Forwarded-Proto。
- **可选副本落盘**：请求体、响应体可分别保存到 `user.home/proxy/{sessionId}-request` 与 `{sessionId}-response`，便于排查问题。

---

## 2. 接入方式

### 2.1 代理路径

| 项目 | 说明 |
|------|------|
| 路径前缀 | `BaseHttpProxyService.proxy` = **`/http/proxy/v1`** |
| 示例 | `POST /http/proxy/v1/chat/completions`、`GET /http/proxy/v1/models` |

所有以 `/http/proxy/v1` 开头的请求由 `SysProxyController` 接收，并交给 `BaseHttpProxyService` 处理。

### 2.2 目标 URL 的指定方式（优先级从高到低）

1. **Query 参数**：`?target=https://api.example.com/v1`
2. **请求头**：`X-Target-URL: https://api.example.com/v1/chat/completions`（完整 URL）
3. **请求头**：`X-Base-URL: https://api.example.com`（会与请求路径拼接，例如路径为 `/http/proxy/v1/chat/completions` 时，会去掉前缀后得到 `/chat/completions`，最终目标为 `https://api.example.com/chat/completions`）
4. **静态默认**：`BaseHttpProxyService.setBase("https://api.example.com")`
5. **路径即 URL**：若请求路径本身以 `http://` 或 `https://` 开头（需在路由中支持），则直接作为目标 URL

若以上均未提供有效目标 URL，将返回 `400 missing_target_url`。

### 2.3 调用示例

**方式一：Query 参数**

```http
POST /http/proxy/v1/chat/completions?target=https://api.openai.com/v1
Content-Type: application/json
Authorization: Bearer sk-xxx

{"model":"gpt-4","messages":[{"role":"user","content":"hello"}]}
```

**方式二：请求头 X-Target-URL（推荐，避免 URL 过长）**

```http
POST /http/proxy/v1/chat/completions
X-Target-URL: https://api.openai.com/v1/chat/completions
Content-Type: application/json
Authorization: Bearer sk-xxx

{"model":"gpt-4","messages":[...]}
```

**方式三：请求头 X-Base-URL + 路径**

```http
POST /http/proxy/v1/chat/completions
X-Base-URL: https://api.openai.com
Content-Type: application/json
Authorization: Bearer sk-xxx

{"model":"gpt-4","messages":[...]}
```

此时目标 URL = `X-Base-URL` + 去掉 `/http/proxy/v1` 后的路径，即 `https://api.openai.com/chat/completions`。

---

## 3. 实现要点

### 3.1 请求体与响应体处理

- **请求体**：有 body 时一般从 `request.getInputStream()` 按块写入上游；**OpenAI 兼容流式对话**见下节。
- **响应体**：上游 `InputStream` 按块读到下游 `HttpServletResponse.getOutputStream()`，流式透传；不区分 SSE/JSON/二进制，统一按字节流处理。

### 3.1.1 OpenAI 流式对话与 `stream_options`（`BaseHttpProxyService`）

为与 [OpenAI Chat Completions](https://platform.openai.com/docs/api-reference/chat/create) 流式语义一致，并便于上游（如 Ollama `/v1/chat/completions`）在 SSE 末尾返回 `usage`：

- **条件**：`POST`、代理路径以 **`/chat/completions`** 结尾、`Content-Type` 含 **`application/json`**。
- **行为**：将请求体**缓冲**（上限 **16MB**，超限返回错误），解析 JSON；若 `"stream": true` 且
  - 无 `stream_options` 字段 → 增加 `"stream_options": { "include_usage": true }`；
  - 已有 `stream_options` 对象但**没有** `include_usage` 键 → 补充 `"include_usage": true`；
  - 已显式包含 `include_usage`（含 `false`）→ **不修改**，尊重客户端。
- **非流式**（`stream` 为 false 或缺省）或其它路径：**不缓冲**，行为与原先一致。

客户端若未传 `stream_options`，代理会自动补齐，便于在流式响应最后一个 chunk 中解析 token 用量。

### 3.2 客户端断开

流式响应过程中，若客户端或前置 nginx 关闭连接，会触发“Connection reset by peer”等异常。服务端会：

- 在写响应时捕获此类异常，仅记录 debug 日志并正常结束写循环，不向上抛错。
- 在入口 catch 中识别“客户端断开”类异常（含 `AsyncRequestNotUsableException`），不记 error、不写 500；若响应已提交则不再调用 `createErrorResponse`。

因此客户端中途断开不会被视为“代理请求失败”。

### 3.3 请求/响应副本（可选）

- 目录：`{user.home}/proxy/`
- 文件名：`{sessionId}-request`、`{sessionId}-response`（sessionId 来自 `request.getSession(true).getId()`，无 session 时用 UUID）。
- 写入与主流并行，任一副本写入失败只打 warn，不影响代理主流程。

---

## 4. 与过滤器/拦截器的关系

代理路径已在以下位置被排除，保证请求体、响应体不被改写或二次消费：

- **XssFilter**：路径前缀 `/http/proxy/v1` 及静态资源等不再做 XSS 包装，直接放行。
- **XssFilterInterceptor**：对 `/http/proxy/v1/**` 等做 `excludePathPatterns`，不执行拦截逻辑。
- **SysProxyController**：类上标注 `@SkipInterceptor`、`@DisableXssFilter`，双重保证即使路径配置变更也不会对代理做参数/body 过滤。

---

## 5. 前置 Nginx 配置建议（流式场景）

当代理用于 AI 大模型等长时间流式输出时，前置 Nginx 需避免缓冲与短超时导致连接被提前关闭。建议对代理路径单独配置 location，例如：

```nginx
location /http/proxy/ {
    proxy_pass http://backend:8080;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header Connection "";

    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 900s;
    proxy_connect_timeout 60s;
    proxy_send_timeout 900s;
    send_timeout 900s;
    chunked_transfer_encoding on;
}
```

要点：

- **proxy_buffering off**：不缓冲上游响应，边收边发，避免流式中断。
- **proxy_read_timeout / send_timeout**：拉长（如 900s），避免长时间无新数据时 nginx 主动断连。
- **proxy_http_version 1.1** 与 **Connection ""**：利于 chunked 与长连接。

若单次流式超过 15 分钟，可将上述超时改为更大（如 1800s、3600s）。

---

## 6. 错误与状态码

| 情况 | HTTP 状态 | 说明 |
|------|-----------|------|
| 缺少目标 URL | 400 | 未提供 target、X-Target-URL、X-Base-URL 或 base，且路径非完整 URL |
| 读取请求体失败 | 400 | 请求体 InputStream 读取异常 |
| 代理过程异常 | 500 | 连接上游、写请求、读响应等发生非“客户端断开”类异常，且响应未提交时返回 JSON 错误体 |
| 客户端中途断开 | - | 不返回 500，仅 debug 日志；若响应已提交则不再写 body |

---

## 7. 相关类与配置

| 类/配置 | 说明 |
|---------|------|
| `cn.org.autumn.service.BaseHttpProxyService` | 代理核心逻辑：建连、转发请求/响应、副本写入、客户端断开判断 |
| `cn.org.autumn.modules.sys.controller.SysProxyController` | 暴露 `/http/proxy/v1/**` 的 REST 入口 |
| `BaseHttpProxyService.proxy` | 路径前缀常量 `/http/proxy/v1` |
| `BaseHttpProxyService.setBase(...)` | 设置默认上游 base URL（可选） |
| XssFilter / XssFilterConfig | 对 `/http/proxy/v1` 等路径排除 XSS 过滤与拦截器 |

---

## 8. 版本与维护

- 文档与实现对应项目当前版本（2026-03）。
- 若新增路径或部署方式，请同步更新本文档与 Nginx 示例。
