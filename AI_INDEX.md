# Autumn AI 文档总索引（统一入口）

> 用途：团队统一引用入口。先看本页，再按任务场景加载最小文档集合。

## 1. 文档职责一览

- `AI_BOOT.md`：最小启动上下文（首轮必读）
- `AI_MAP.md`：高频开发能力主图
- `AI_CRYPTO.md`：接口加解密兼容与迁移
- `AI_SECURITY.md`：安全强校验、灰度、演练
- `AI_TEMPLATES.md`：模块任务模板库
- `AI_GOVERNANCE.md`：治理协作、术语、维护口径
- `AI_PROMPTS.md`：可复制提示词模板
- `AI_GUIDE.md`：多项目导航与引用方式

## 2. 推荐加载矩阵（按场景）

- 日常开发：`AI_BOOT.md + AI_MAP.md`
- 新模块/代码生成：`AI_BOOT.md + AI_MAP.md + AI_TEMPLATES.md`
- 多项目模板整合（TemplateFactory）：`AI_BOOT.md + AI_MAP.md + AI_GOVERNANCE.md`
- 接口加解密改造：`AI_BOOT.md + AI_MAP.md + AI_CRYPTO.md`
- 安全改造/攻防演练：`AI_BOOT.md + AI_MAP.md + AI_SECURITY.md`
- 文档治理/多人协作：`AI_BOOT.md + AI_MAP.md + AI_GOVERNANCE.md`
- 快速起任务：在以上任一组合追加 `AI_PROMPTS.md`

## 3. 标准引用顺序

- 第一步：`@AI_BOOT.md`
- 第二步：`@AI_MAP.md`
- 第三步：按场景追加专项文档（`CRYPTO/TEMPLATES/GOVERNANCE/SECURITY/PROMPTS`）
- 第四步：追加当前项目上下文（README + 目标模块目录）

## 4. 相对路径示例（可复制）

```md
请先读取：
- @../autumn/AI_INDEX.md
- @../autumn/AI_BOOT.md
- @../autumn/AI_MAP.md
- （按需）@../autumn/AI_CRYPTO.md / @../autumn/AI_TEMPLATES.md / @../autumn/AI_GOVERNANCE.md / @../autumn/AI_SECURITY.md / @../autumn/AI_PROMPTS.md
- @./README.md
- @./<目标模块目录>
```
