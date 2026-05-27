# 实体 Service 内聚与类规模（开发共识）

> 与 `docs/AI_STANDARDS.md` §2 互补：**架构分层**以 STANDARDS 为准；本文约定 **业务代码落在哪一类、何时才拆类**。

## 1. 核心原则

| 原则 | 说明 |
|------|------|
| **一实体一 Service** | 与表/实体对应的 **`XxxService extends ModuleService<XxxDao, XxxEntity>`** 承载该域 **全部业务**：CRUD、规则、事务、**`LoopJob` 定时任务**、缓存回源（`@Cache` + 基类 `getCache`）。 |
| **Controller 薄** | 只做入参、鉴权上下文、调用 Service、返回 `Response`；**禁止**在 Controller 写 CRUD、配额判断、生命周期状态机。 |
| **禁止为“定时任务”单独拆 Service** | 例如软删超期硬销毁、每日清理，属于 **`RobotEntity` 域运维**，应写在 **`RobotService`** 的 `onOneDay()` / `purge*` 中，**不要**再建 `RobotMaintenanceService` 等平行类。 |
| **跨实体才拆 Service** | **`RobotQuotaService`**（读全局 `RobotQuotaConfig`）、**`RobotConfigService`**（`bot_robot_config` 实体）等，因 **实体或配置域不同** 而独立，不是因为“方法太多”随意拆。 |
| **少类、少工具类** | 相似逻辑放在 **同一 Service 的 private 方法** 或 **同一 `support` 包内少量类**；避免 `XxxHelper` / `XxxUtil` 泛滥。 |
| **开发期可破坏性重构** | 无历史兼容负担时：**删除悬空类、未引用方法、过时注释**；不保留“曾经用过”的垃圾代码。 |

## 2. 何时允许再拆一个 Service / 类

仅在满足 **其一** 时考虑拆分（且优先 **按实体/子域**，不按技术层）：

1. **对应另一张表/实体**（如 `RobotHookService` ↔ `RobotHookEntity`）。
2. **单一 `XxxService` 实现类（不含 gen）稳定超过约 1000 行**，且 private 方法已无法再清晰分区。
3. **明确的跨模块 Facade**（编排多个 Service 的用例入口），命名如 `XxxApplicationService`，**不**承载单表 CRUD。

**禁止**因下列理由拆类：

- “定时任务想单独放一个类”
- “清理/运维想单独放一个类”
- “Controller 太长把逻辑挪到 Helper”（应下沉到 Service）

## 3. 定时任务（`LoopJob`）

- **禁止**生产环境 `@Scheduled`（见 `AI_STANDARDS.md` §5）。
- **必须**在 **该实体对应的 Service** 上 `implements LoopJob.OneDay`（或其它周期接口），在 **`onOneDay()`** 内调用本类 **private / public** 业务方法。
- 任务所需 **其它实体** 的数据：注入 **其它 Service**，不在任务类里直接 `@Autowired` Dao。

示例（机器人）：

```text
RobotService
  ├── create / delete / destroyByAdministrator  （用户/管理员 API）
  ├── onOneDay()                                 （定时：超期软删硬销毁）
  └── purgeAllRobotsForOwner()                   （集成测试/运维清理，仍属 bot_robot 域）
```

## 4. CRUD 与 Dao

- **持久化只经本实体 Service 的 `baseMapper`**（继承自 `ModuleService`），**禁止**本模块 Controller 或其它 Service **@Autowired 本实体 Dao**。
- **例外**：另一 **实体** 的 Service 通过 **对方 Service 公共方法** 协作；或团队书面约定的 **Facade** 编排层。

## 5. 交叉请求（少数例外）

多个 HTTP 入口共用一个 **非实体** 用例（如开放 API 编排）时：

- 允许在 **发起用例的 Controller** 注入 **多个 Service**；
- **仍禁止** Controller 调用 Dao；
- 若编排逻辑超过数屏，可增 **Facade Service**，但 **不** 把单表 CRUD 迁到 Facade。

## 6. 机器人模块对照（参考实现）

| 类 | 实体/域 | 职责 |
|----|---------|------|
| `RobotService` | `bot_robot` | 生命周期、定时硬销毁、清库、Hook 派发触发 |
| `RobotConfigService` | `bot_robot_config` | 用户配额覆盖、`@Cache` 读配置 |
| `RobotQuotaService` | 无表（读 `RobotQuotaConfig`） | 全局配额与软删门禁断言 |
| `RobotHookService` | `bot_robot_hook` | Hook CRUD |
| `RobotTokenService` | `bot_robot_token` | 令牌 CRUD |

## 7. 重构检查清单

- [ ] 是否新增了一个 **仅包一层调用**、无独立实体对应的 Service？若有，合并回实体 Service。
- [ ] 定时任务是否在 **实体 Service** 上实现 `LoopJob`？
- [ ] Controller 是否仍含 **状态变更 / 计数 / 配额**？若有，下沉。
- [ ] 是否留下 **无引用的类、方法、常量**？删除。
- [ ] 拆类后文档、测试、Skill 是否仍引用旧类名？同步改。

## 8. 相关文档

- 分层与 API：`docs/AI_STANDARDS.md`
- 代码版式：`docs/AI_CODE_STYLE.md`
- 基类缓存/队列：`docs/AI_CODEGEN.md` §4
- 机器人业务：`docs/AI_ROBOT.md`、`docs/AI_ROBOT_API.md`
