# Autumn 多项目 AI 开发指南（导航版）

> 目标：在多项目场景下，用最小上下文让 AI 快速进入正确执行路径。
> 统一索引入口：`@<autumn-root>/AI_INDEX.md`

## 1. 推荐喂给顺序（3 层）

1. **框架层（固定）**
   - 首轮必读：`@<autumn-root>/AI_BOOT.md`
   - 核心能力：`@<autumn-root>/AI_MAP.md`
   - 按需追加：`@<autumn-root>/AI_CRYPTO.md` / `@<autumn-root>/AI_TEMPLATES.md` / `@<autumn-root>/AI_GOVERNANCE.md` / `@<autumn-root>/AI_SECURITY.md` / `@<autumn-root>/AI_PROMPTS.md`
2. **当前项目层（变化）**
   - 当前项目 `README`、模块目录、关键配置
3. **当前任务层（变化）**
   - 目标 + 约束 + 验收标准 + 禁止事项

## 2. 文档分工（避免全量加载）

- `AI_BOOT.md`：最小启动上下文（首轮固定）
- `AI_MAP.md`：高频开发能力主图
- `AI_CRYPTO.md`：接口加解密兼容专项
- `AI_SECURITY.md`：签名/灰度/演练专项
- `AI_TEMPLATES.md`：模块任务模板库
- `AI_GOVERNANCE.md`：治理、术语、协作约定
- `AI_PROMPTS.md`：可复制提问模板

## 3. 常用加载组合

- 日常开发：`AI_BOOT.md + AI_MAP.md`
- 接口加解密改造：`AI_BOOT.md + AI_MAP.md + AI_CRYPTO.md`
- 模块新建/代码生成：`AI_BOOT.md + AI_MAP.md + AI_TEMPLATES.md`
- 多项目模板整合（TemplateFactory）：`AI_BOOT.md + AI_MAP.md + AI_GOVERNANCE.md`
- 安全改造/攻防演练：`AI_BOOT.md + AI_MAP.md + AI_SECURITY.md`
- 多人协作/规范治理：`AI_BOOT.md + AI_MAP.md + AI_GOVERNANCE.md`
- 快速发起任务：在以上组合基础上追加 `AI_PROMPTS.md`

## 4. 相对路径最小示例

```md
请先读取：
- @../autumn/AI_BOOT.md
- @../autumn/AI_MAP.md
- （按需）@../autumn/AI_CRYPTO.md / @../autumn/AI_TEMPLATES.md / @../autumn/AI_GOVERNANCE.md / @../autumn/AI_SECURITY.md / @../autumn/AI_PROMPTS.md
- @./README.md
- @./<目标模块目录>

任务目标：
- <一句话>
```

## 5. 一句话工作流

每次提需求时固定：`BOOT -> MAP -> 按需专项 -> 项目目录 -> 目标与约束`。
