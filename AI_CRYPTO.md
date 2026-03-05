# Autumn 加密兼容专项（按需加载）

> 用途：仅在“接口加解密改造、兼容协议迁移、客户端对接”任务时加载。  
> 日常 CRUD 开发默认不加载。

## 1. 核心目标

- 同一接口兼容“新加密协议（`Request<T>`）”与“旧明文协议（平铺字段）”。
- 通过统一解析与拦截，不拆分双接口。

## 2. 推荐组合与边界

- 推荐组合：
  - 入参：`CompatibleRequest<T>`
  - 出参：`CompatibleResponse<T>`（需要包装）或原始 DTO（不包装）
- 适用边界：
  - 请求参数已实现 `Encrypt`：不要再套 `CompatibleRequest`。
  - 返回参数已实现 `Encrypt`：不要再做额外兼容包装。

## 3. 关键类与职责

- `cn.org.autumn.model.CompatibleRequest`
- `cn.org.autumn.model.CompatibleResponse`
- `cn.org.autumn.model.Wrap`
- `cn.org.autumn.model.EndpointInfo`
- `cn.org.autumn.modules.oauth.resolver.EncryptArgumentResolver`
- `cn.org.autumn.modules.oauth.interceptor.EncryptInterceptor`
- `cn.org.autumn.service.RsaService`（`getEncryptEndpoints` 计算 `wrap`）

## 4. 触发条件与行为矩阵

- 触发条件：
  - 请求解密：请求体包含 `ciphertext + session`
  - 响应加密：请求头包含 `X-Encrypt-Session`
- `CompatibleRequest` 归一化：
  - 标准 `{"data": ...}` -> 写入 `request.data`
  - 非标准对象/数组/基础类型 -> 直接写入 `request.data`
- 行为矩阵：
  - `CompatibleRequest + CompatibleResponse`：有 session 加密包装；无 session 解包 `data`
  - `CompatibleRequest + Response`：请求兼容；响应按 header 决定是否加密
  - `CompatibleRequest + 老返回对象`：有 session 且 JSON 时兼容加密；无 session 返回原值
  - 文件流/非 JSON：不自动包装，不强制加密包装

## 5. 标准接口写法模式

- 阶段 A：初始化握手接口（不参与业务加密）
  - 示例：`RsaController.initEncryption(...)`
  - 建议 `@Endpoint(hidden = true)`
- 阶段 B：能力探测接口（前端/客户端自适配）
  - 示例：`RsaController.getEncryptEndpoints(...)`
  - 基于 `wrap.request/wrap.response` 动态决定调用方式
- 阶段 C：业务接口
  - 优先 `Request/Response` 或 `Compatible*`
  - 无 `X-Encrypt-Session` 时返回明文；`CompatibleResponse` 可降级为 `data`

## 6. 代码模板

```java
@PostMapping("/biz/action")
public CompatibleResponse<BizVO> action(@RequestBody(required = false) CompatibleRequest<BizDTO> request) {
    BizDTO dto = request != null ? request.getData() : null;
    if (dto == null) {
        CompatibleResponse<BizVO> fail = new CompatibleResponse<>();
        fail.setCode(-1);
        fail.setMsg("非法请求");
        return fail;
    }
    return CompatibleResponse.ok(service.action(dto));
}
```

```java
@PostMapping("/demo/action")
public Response<DemoVO> action(@Valid @RequestBody Request<DemoDTO> request) {
    DemoDTO dto = request != null ? request.getData() : null;
    if (dto == null) {
        return Response.error("非法请求");
    }
    return Response.ok(service.action(dto));
}
```

## 7. AI 硬约束与迁移建议

- 不要自建平行加密字段协议（如 `cipher/iv/token`）。
- 不要混用握手接口与业务接口。
- 除文件下载/流式响应外，JSON 接口按统一包装语义实现。
- 迁移顺序：先高频写接口，再普通读接口，最后下载导出接口。
- 每批次必须回归：明文请求、密文请求、header 有/无。

## 8. 联调排错（最小）

- `FORCE_ENCRYPT_REQUEST_REQUIRED`：改为密文请求后重试
- `FORCE_ENCRYPT_SESSION_REQUIRED`：重新执行 `/rsa/api/v1/init`
- `RSA_CLIENT_PUBLIC_KEY_NOT_FOUND`：重新上传客户端公钥并 init
- 其他解密失败：清理本地 session/AES 缓存后重握手

## 9. 与安全专项关系

- 强校验（`X-Encrypt-Auth`、`X-Encrypt-Agent`、`X-Encrypt-Timestamp`、`X-Encrypt-Nonce`、`X-Encrypt-Signature`）
- 灰度上线、攻防演练
- 以上统一见：`@AI_SECURITY.md`
