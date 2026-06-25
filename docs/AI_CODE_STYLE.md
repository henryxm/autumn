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

## 7. import 与全限定类名

- **默认**：在 `.java` 源文件中引用类型时，**一律使用 `import`**，在代码里写**短类名**，提高可读性。
- **禁止**在方法体、字段声明、泛型参数等位置写**全限定类名**（FQN），例如 `java.lang.reflect.Method`、`com.foo.bar.Baz`，除非满足下列例外。
- **例外（允许 FQN）**：
  - **类名冲突**：同一编译单元内两个不同包的同名类无法通过 `import` 同时引入时，对其中一个（或冲突方）使用 FQN（如 `java.util.Date` 与 `java.sql.Date`、MyBatis `Configuration` 与 Spring `@Configuration`）。
  - **极少见、一次性**的歧义消除（Code Review 应能一眼看出原因）；**不得**以「懒得写 import」为由使用 FQN。
- **同包类**：同包类型可不写 import（语言默认）；跨包类型**必须** import，勿写 `cn.org.autumn.modules.sys.entity.SysUserEntity` 等形式。
- **不算违规**：字符串中的类名（`Class.forName("…")`、Quartz/自动配置属性值）、Javadoc `{@link …}`。

### 7.1 CI 与本地检查

| 命令 | 用途 |
|------|------|
| `bash scripts/check-java-fqn` | **门禁**：扫描 `autumn-lib/src`、`autumn-modules/src`、`web/src`；有命中 **exit 1** |
| `python3 scripts/check_java_fqn.py` | 同上，仅报告不失败 |
| `bash scripts/constraints-scan` | 全量规范扫描 **I 组**含 FQN；见 `docs/AI_INDEX.md` |

- **白名单**：合法冲突登记在 `scripts/fqn-allowlist.txt`（新增例外须注释原因）。
- **GitHub Actions**：`.github/workflows/java-style-check.yml` 在 push/PR 时运行 `check-java-fqn`。
- **Cursor**：`.cursor/rules/java-import-fqn.mdc`（编辑 `*.java` 时生效）。

```java
// 推荐
import java.lang.reflect.Method;

private static String handlerLabel(MethodParameter parameter) {
    Method method = parameter.getMethod();
    ...
}

// 避免（无冲突却写 FQN）
private static String handlerLabel(MethodParameter parameter) {
    java.lang.reflect.Method method = parameter.getMethod();
    ...
}

// 允许（类名冲突）
import java.util.Date;
...
if (val instanceof java.sql.Date) { ... }
```

## 8. 日志语句（`log.info` / `log.debug` 等）

- **语言**：`log.*` 消息字符串及传入 `ExceptionUtils.logDetailedException` 的首参须为 **英文**（便于检索与跨团队运维）；用户可见文案、注释、Javadoc 不受此限。
- **`log.trace` / `log.debug` / `log.info` / `log.warn` / `log.error` 调用必须单行写完**：从 `log.` 到语句结束分号**不得换行**拆成多行。
- **占位符**：继续用 SLF4J `{}` 传参；参数过多时可在**上一行**先赋给局部变量，但 **`log.xxx(...)` 本身仍占一行**。
- **禁止**为「对齐」或「省横向滚动」把一条日志拆成多行实参。
- 日志类使用 Lombok **`@Slf4j`**，禁止手动 `Logger` / `LoggerFactory`（见 **`docs/AI_STANDARDS.md` §2.0**）。

```java
// 推荐
log.debug("UserContextArgumentResolver enter: {} required={}", gate, AuthenticatedSupport.authRequired(parameter));

// 推荐：先算变量，日志仍一行
String summary = buildSummary(context);
log.info("Robot soft-delete purge: removed {} records older than {} days", summary, retentionDays);

// 避免（log 调用换行）
log.debug("UserContextArgumentResolver enter: {} required={}",
        gate, AuthenticatedSupport.authRequired(parameter));

// 避免（中文 log 消息）
log.error("请求失败:{}", e.getMessage());
```

- 与 §2「方法调用」的关系：一般方法调用过长时可折行；**日志调用不适用该折行例外**，一律单行。
