# 数据库只读模式（CrudGuard）框架标准

> **用途**：在系统升级/维护期间关闭写库（`databaseWrite=false` 或 `localWrite=false`）时，**用户仍可登录与只读浏览**，**业务写操作返回 834**；Init、定时任务、审计日志等**可选写**静默跳过，不刷 ERROR。  
> **统一入口**：`cn.org.autumn.database.CrudGuard`（跨 Autumn 2.x / 3.x 业务项目复用同一套 API）。

---

## 1. 开关与拦截层次

| 开关 | 配置键 | 含义 |
|------|--------|------|
| `databaseWrite` | `sys_config.SYSTEM_UPGRADE` JSON | **系统级**：false = 全局只读（除「必要写」绕过） |
| `localWrite` | 同上 | **用户级**：false = 仅禁止 HTTP 用户作用域写库，后台 SYSTEM 仍可写 |

**enforcement（勿在 Service 重复校验业务写）：**

| 层级 | 组件 | 职责 |
|------|------|------|
| MyBatis | `CrudInterceptor` | 拦截全部 INSERT/UPDATE/DELETE |
| JDBC/DDL | `CrudGuard.enforce()` | 原生 JDBC、备份恢复、DDL |
| HTTP | `CrudScopeInterceptor` | 标记 `Scope.USER` |
| 异步 | `TagTaskExecutor` | 传播 ThreadLocal 作用域 |

违反时抛 `AException`，错误码 **`Error.DATABASE_READ_ONLY`（834）**。

---

## 2. 全局标准 API（业务/框架必用）

**所有项目只认以下静态方法，禁止在各 Service 再写 `canPersistXxx()` 私有方法。**

### 2.1 查询：当前是否允许写库

```java
if (CrudGuard.writable()) {
    // 可选：仅在允许时更新
}
```

已注入 `CrudGuard` Bean 时，等价实例方法为 `allow()`（Java 不允许静态/实例同名 `writable()`）。

### 2.2 可选写（Init 种子、统计、审计日志）

不允许写库时**静默跳过**，不抛异常（与先调用 `writable()` 再 return 等价，**推荐 `opt`**）：

```java
CrudGuard.opt(() -> {
    baseMapper.insert(entity);
});
```

带返回值：

```java
SysConfigEntity saved = CrudGuard.opt(() -> insertAndReturn(entity));
// 只读时为 null
```

### 2.3 必要写（极少数路径，显式绕过只读）

仅用于：**登录认证**、**保存 CRUD 开关本身**、**安装向导**等框架级必须写库场景：

```java
CrudGuard.force(() -> subject.login(token));

R result = CrudGuard.force(() -> doLogin(...));
```

**禁止**用 `force` 包裹普通业务 CRUD。

### 2.4 框架 catch：只读异常消化

Init、LoopJob、catch 块标准写法：

```java
} catch (Exception e) {
    if (CrudGuard.suppress(e, "SysConfigService.init")) {
        return; // 或 continue
    }
    log.error("...", e);
}
```

- `blocked(Throwable)` — 判断异常链是否含 834（含 MyBatis `PersistenceException` 包装）
- `suppress(e, tag)` — 若为只读则 **debug 一条并返回 true**，否则返回 false

`ExceptionUtils.isDatabaseReadOnlyException(e)` 已委托 `CrudGuard.blocked`，可继续使用，**新代码优先 `CrudGuard`**。

---

## 3. 三类写操作（决策表）

| 类型 | 示例 | 标准 API | 只读下行为 |
|------|------|----------|------------|
| **用户业务写** | 保存用户、删菜单、改配置 | 直接 `insert/update`（走拦截器） | 抛 **834** 给前端 |
| **可选系统写** | Init 种子、登录日志、访问统计、白名单 | `writable()` / `opt` | **静默跳过** |
| **必要框架写** | Shiro 登录、写 `SYSTEM_UPGRADE` | `force` | **允许写** |

---

## 4. 框架已接入点（无需业务重复实现）

| 位置 | 行为 |
|------|------|
| `Factory.invoke`（Init/After/Post） | `suppress` → debug，不打 ERROR |
| `LoopJob.doExecuteJob` | 同上，不计入连续失败 |
| `CrudScopeInterceptor` | 认证路径排除 USER 作用域：`/sys/login`、`/loading.html` 等 |
| `CrudGuardService` | `@Order(-2000)` + `InitFactory.Before` 尽早加载开关 |
| `SysLoginController.login` | 整段 `force` |
| `AExceptionHandler` | 解包 MyBatis 包装的 834，避免 100000 |

---

## 5. 业务项目接入清单

1. **依赖** autumn-lib（含 `CrudGuard`、写拦截器注册）。
2. **管理页** 复用 `dbmanage.html` / `CrudGuardService` 持久化 `SYSTEM_UPGRADE`。
3. **新增 Service 写路径**：
   - 用户 CRUD → **不写守卫代码**，靠拦截器返回 834。
   - Init/定时任务里的**可选**写 → `opt`。
   - 确属框架必须写 → `force` + 代码审查。
4. **catch 块** 涉及写库失败 → `suppress(e, "YourService.method")`。
5. **禁止** 在 Service 层复制 `check` 或私有 `canPersist*`。

---

## 6. 代码示例（复制即用）

### Init 种子数据

```java
@Override
public void init() {
    CrudGuard.opt(() -> put(getSeedMapping()));
}
```

### 定时任务清理

```java
@Override
public void onOneHour() {
    CrudGuard.opt(this::cleanupExpired);
}
```

### Controller 登录

```java
@PostMapping("/sys/login")
public R login(...) {
    return CrudGuard.force(() -> doLogin(...));
}
```

### 用户保存（业务写 — 不加守卫）

```java
public void saveUser(SysUserEntity user) {
    updateById(user); // 只读时 MyBatis 拦截器抛 834，AExceptionHandler 返回前端
}
```

---

## 7. 相关文档

- 开关与运维页：`CrudGuardService`、`/dbmanage.html`
- 多库与 Dao 规范：`docs/AI_DATABASE.md`
- 会话与登录：`docs/AI_SESSION_GUARD.md`
