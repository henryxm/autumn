# Java 代码版式（Autumn 推荐）

> 与 `docs/AI_STANDARDS.md` 架构纪律互补，侧重**可读性与行阅读节奏**。新写与重构业务代码时默认遵循。

## 1. 方法声明

- **方法签名占一行**：修饰符、返回类型、方法名、参数列表同一行。
- **多参数不换行**（参数列表在行长可接受时）；仅当单行明显过长（如超过约 160 字符）时再拆行，拆行时每个参数独占一行并与上一行参数对齐。

```java
// 推荐
public RobotCreateResult create(String owner, String name, String description, String icon, Integer tokenExpireDays, String access) throws Exception {
    ...
}

// 避免（参数不多却换行）
public RobotCreateResult create(
        String owner,
        String name) throws Exception {
```

## 2. 方法调用

- **实参保持同一行**，与声明规则一致；过长时再折行。

```java
robotService.updateProfile(data.getUuid(), requireOwner(context), data.getName(), data.getDescription(), data.getIcon(), data.getAccess(), data.getBlack());
```

## 3. if / else

- **条件与 `if` 同一行**；`if` 后**换行**，语句体从下一行开始书写。
- **单条语句**：可不写 `{}`，但**必须换行**（不得与 `if` 写在同一行）。
- **多条语句**或需在分支内声明局部变量时：必须使用 `{}` 包裹。

```java
// 推荐：单条语句，换行、无花括号
if (StringUtils.isBlank(owner))
    return;

// 推荐：多条或需块作用域，使用花括号
if (pending > limit) {
    throw softDeleteGateException(pending, limit);
}

// 避免：条件与语句挤在同一行
if (StringUtils.isBlank(owner)) return;

// 避免：单条语句却与 if 同行且无换行块感（可读性差）
if (pending > limit) throw softDeleteGateException(pending, limit);
```

- `else`、`else if`：条件与关键字同一行；单条 `else` 体可换行无 `{}`，多条仍用 `{}`。习惯写法：`} else if (cond) {` 或 `else if (cond)` 后换行单语句。

## 4. 循环

- `for` / `while` 条件与关键字同一行；循环体使用 `{}`。

## 5. 内聚与类规模

- **实体 Service 内聚、定时任务、何时拆类**：见 **`docs/AI_SERVICE_COHESION.md`**。
- 跨实体编排放在 Facade 或发起用例的 Service，不在 Controller 堆叠规则。

## 6. 维护约定

- 机器人模块示例实现见 `cn.org.autumn.modules.bot.service` 包。
- 与 Checkstyle/IDE 格式化冲突时，以本文与 `AI_STANDARDS.md` 为准。
