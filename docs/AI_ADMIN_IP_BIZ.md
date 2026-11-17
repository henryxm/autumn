# 业务仓管理 IP 门禁接入标准（`wall_ip_admin_white`）

面向 **非框架仓 / 非 `cn.org.autumn.modules` 前缀** 的业务系统：如何与 Autumn 3.x 统一「**系统管理员 + 管理 IP 白名单**」策略对齐。

兼容总表见 **`docs/AI_SECURITY_HARDENING_3_0_COMPAT.md` C-20**；页面门禁见 **`docs/AI_STANDARDS.md` §14**。

---

## 1. 目标语义（不可降级）

| 项 | 要求 |
|----|------|
| 管理 API / gen 全表 CRUD | 已登录 **系统管理员** + 客户端 IP ∈ **`wall_ip_admin_white`**（`IpAdminWhiteEntity`） |
| 真相源 | `AdminAccessGuard.requireGenAdmin` / `requireOpsAdmin` → `IpAdminWhiteService.checkAdmin` |
| 框架切面 | `GenAdminAccessAspect`（判定 `shouldRequireAdminIpApi`） |
| 自助例外 | `usr` / `safe` / `bot` / `qrc` + SPI `selfGenModules()`；本人 `nav` / `info()` / `password` / `session/self*` 等 |
| Cookie 写操作 | 另受 `SECURITY_AUTH_CONFIG.csrfEnforce` + `X-CSRF-Token` |

**禁止**：仅靠菜单 `@RequiresPermissions`、仅超管不验 IP、对 gen 前缀 Shiro `anon`。

---

## 2. 包名与切面覆盖（优先选标准包）

框架切面织入范围（3.x）：

```text
within(*..modules..controller..*)
  || within(*..controller.gen..*)
  || within(cn.org.autumn.modules.install..*)
&& !@within(jakarta.websocket.server.ServerEndpoint)
```

`@ServerEndpoint` 必须排除：被 CGLIB 代理后 `ServerEndpointExporter` 会按代理类注册，Tomcat 启动失败（兼容项 **C-22**）。JSR-356 端点勿标 `@RestController`。

方法级判定（兼容项 **C-23**）：切面传入方法**声明类**——声明在 `*ControllerGen` / `*.controller.gen.*` 的 CRUD 仍强制；业务模块子类扩展且无 `@RequiresPermissions` 的公开/集群 API **不连坐**；`GEN_MODULES` 下 `*Api` / `*ApiController` 不因模块毯误拦。

### 模式 A — 推荐（零业务切面）

包名符合下列之一即可被框架切面织入，再由 `shouldRequireAdminIpApi` 判定是否强制：

| 包形态 | 示例 | 模块短名解析 |
|--------|------|----------------|
| `{任意}.modules.{module}.controller[.gen]` | `com.minclouds.shop.modules.goods.controller.gen` | `goods`（取 `.modules.` 后第一段） |
| `{任意}..controller.gen..*` | `cn.bighub.modules.bigmodel.controller.gen` | 同上；无 `.modules.` 时 gen 仍按「含 `.controller.gen.`」强制 |

**手写管理 Controller**（非 gen、非 `*AdminController`）若模块短名不在框架 `GEN_MODULES`（sys/wall/db/job/gen/oss/lan/spm/node），须用 **模式 B** 注册，否则切面织入后仍会判定「不强制」。

### 模式 B — SPI 注册「管理模块」短名

业务仓实现并注册 Spring Bean：

```java
@Component
public class ShopAdminAccessModuleHandler implements AdminAccessModuleHandler {
    @Override
    public Set<String> managedModules() {
        // 手写管理 API 所在 modules.{name} 的 name
        return Set.of("goods", "order", "promotion");
    }

    @Override
    public Set<String> selfGenModules() {
        // 仅「本人数据」gen；禁止把全站业务模块塞进豁免
        return Set.of("shopuser");
    }
}
```

| SPI 方法 | 作用 |
|----------|------|
| `managedModules()` | 追加「全局管理」模块：该模块下手写 Controller **强制**超管+管理 IP（与框架 `GEN_MODULES` 同语义） |
| `selfGenModules()` | 追加「自助 gen」豁免：不强制超管+IP（仍须权限与行级隔离） |

接口：`cn.org.autumn.config.AdminAccessModuleHandler`（`autumn-handler`）。

### 模式 C — 异形包（无 `modules`、无 `controller.gen`）

例如 `com.acme.admin.api.OrderAdminFacade`：框架切面织不进或判定不命中时，业务仓增加 **薄切面**，**禁止复制**门禁逻辑，统一委托：

```java
@Slf4j
@Aspect
@Order(0)
@Component
public class AcmeAdminIpAspect {

    @Autowired
    private AdminAccessGuard adminAccessGuard;

    @Before("within(com.acme.admin.api..*) && execution(public * *(..))")
    public void enforce(JoinPoint joinPoint) {
        adminAccessGuard.enforceAdminIpApiIfRequired(joinPoint);
    }
}
```

要点：

1. Pointcut 只收紧到**本仓管理面**包，勿 `within(*..*)` 扫全 classpath。  
2. **只调用** `AdminAccessGuard.enforceAdminIpApiIfRequired(JoinPoint)`（内部 = `shouldRequireAdminIpApi` + `requireGenAdmin`）。  
3. 若类名不以 `AdminController` 结尾、又不是 gen、模块也不在 `managedModules`：将该模块加入 **模式 B**，或把类改名为 `*AdminController`，或对方法显式 `requireOpsAdmin`。

---

## 3. 判定速查（`shouldRequireAdminIpApi`）

命中任一条即强制（在未被豁免的前提下）：

1. 非自助的 `*.controller.gen.*` / 继承受限 `*ControllerGen`  
2. 模块 ∈ 框架 `GEN_MODULES` ∪ SPI `managedModules()`  
3. 类名以 `AdminController` 结尾  

常见豁免：登录/公开 API、install 向导、`WallController` 认领链、自助 gen、本人会话与 `nav` 等（以源码 `AdminAccessGuard` 为准）。

---

## 4. 页面与运维 REST

| 场景 | 做法 |
|------|------|
| 业务全表 / 运维后台页 | `@PageAware(login = true, admin = true)`；经 `SysPageController` / SPM 时走 `gateView` |
| 手写运维 REST（切面未覆盖） | `adminAccessGuard.requireOpsAdmin(request, getClass(), method)` |
| 管理台综合 API | `SystemAdminApi.execute`（已 = 超管+管理 IP） |

---

## 5. 业务仓落地检查清单

- [ ] gen / 管理 Controller 优先落在 `..modules.{module}.controller[.gen]..`（模式 A）  
- [ ] 非 gen 管理模块已 `managedModules()` 注册（模式 B）或类名 `*AdminController`  
- [ ] 异形包已模式 C 薄切面，且委托 `enforceAdminIpApiIfRequired`  
- [ ] 未把整站模块写入 `selfGenModules()`  
- [ ] 管理页 `admin=true`；Cookie POST 带 CSRF（`csrfEnforce`）  
- [ ] 非超管 / 管理 IP 未加白调 gen → 403；日志含 `SecurityAlert denied`  
- [ ] 生产 profile **勿**叠 `dev`/`h2`/`sqlite`（否则 `checkAdmin` 在 `Config.isDev()` 下短路）

---

## 6. 反例

| 反例 | 问题 |
|------|------|
| 包名 `com.acme.web.GoodsController` 无切面、无 `requireOpsAdmin` | 菜单用户可直打全表 |
| SPI `selfGenModules()` 塞入 `im`/`goods` 全站 | 架空管理 IP |
| 业务仓复制一套 IP 白名单表 | 与 `wall_ip_admin_white` 分叉 |
| 仅 `isSystemAdministrator` 不调 `checkAdmin` | 会话被盗后非白名单 IP 仍可运维 |

---

## 7. 相关源码

| 组件 | 位置 |
|------|------|
| 切面 | `autumn-modules` … `GenAdminAccessAspect` |
| 判定 / 执行 | `AdminAccessGuard#shouldRequireAdminIpApi` / `#enforceAdminIpApiIfRequired` |
| gen 模板 | `template/ControllerGen.java.vm`（各 CRUD 入口显式 `enforceAdminIpApiIfRequired`） |
| SPI | `autumn-handler` … `AdminAccessModuleHandler` |
| 管理 IP 实体 | `IpAdminWhiteEntity`（`wall_ip_admin_white`） |
