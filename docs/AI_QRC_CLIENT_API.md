# QRC APP / 客户端 API 手册

> APP 须已登录；鉴权同机器人/安全模块（Header `Token` 或 Bearer）。

## 1. 扫码确认流程

1. 解析 QR 得到 `uuid`：
   - `https://{host}/qrc/v1/t/{uuid}`
   - `autumn://qrc/t/{uuid}`
   - （可选）`GET /qrc/v1/t/{uuid}` 校验票据
2. `GET /qrc/api/v1/ticket/{uuid}` — 展示 client 名、scope、`scopeLabels`
3. `POST /qrc/api/v1/ticket/scan`

```json
{ "data": { "uuid": "..." } }
```

4. 用户点确认 → `POST /qrc/api/v1/ticket/confirm`（同 body）
5. 若 `delivery=DEEP_LINK`，响应 `result.deepLink`，由 APP 打开第三方 scheme

## 2. 拒绝

```json
POST /qrc/api/v1/ticket/deny
{ "data": { "uuid": "..." } }
```

## 3. 确认页字段（GET detail）

| 字段 | 说明 |
|------|------|
| `uuid` | 票据 id |
| `intent` | 场景 |
| `clientName` / `clientIconUri` | 第三方应用展示 |
| `scope` | 授权范围（原始字符串） |
| `scopeLabels` | 展示用 scope 列表（`ConsentProvider` 或 scope 拆分） |
| `redirectUri` | OAuth 回调（展示用） |

## 4. UX 要求

- 必须展示：应用名、scope/`scopeLabels`、当前操作类型（登录本站 / 授权第三方）
- 扫码后**默认不自动 confirm**，需用户点击
- `DEEP_LINK` 仅在用户确认后跳转

## 5. 错误处理

| 情况 | 建议 |
|------|------|
| `8610` 票据过期 | 提示重新扫码 |
| `8617` 扫码用户不一致 | 提示使用同一账号确认 |
| `8630` 未启用 QRC | 联系管理员开通 client |
