# Autumn 双主键模型（自增 id + 业务键）

> **用途**：约定每个实体的两类标识、标记接口、自动生成与开发约束。约束权威与 **§10.4** 一致；本文侧重**落地步骤与 API**。
> **关联**：`docs/AI_STANDARDS.md` §10.4、`docs/AI_DATABASE.md` §1.1、`docs/AI_CODEGEN.md` 第 1～2 步。

## 1. 模型总览

| 列 | 类型 | 职责 | 允许使用场景 | 禁止使用场景 |
|----|------|------|--------------|--------------|
| **`id`** | `Long`，自增 | 技术主键 | 后台代码生成列表/表单 CRUD；框架 `updateById` / `deleteById` | 业务外键、对外 API、缓存键、消息引用、日志对外资源 ID |
| **`uuid`** 或 **`user`** | `String` / `Long` / `String` | 第二主键（业务键，**按需**） | 表间关联、对外 ID、缓存、领域引用 | 与 `id` 混用；与另一种第二主键列并存 |

**第二主键选型（有则三选一，无则仅 `id` + `user` 等业务列）**：

| 场景 | 第二主键列 | 标记接口 | 说明 |
|------|------------|----------|------|
| **默认**（需全局行级业务标识） | **`uuid`** | **`UuidBased`** / **`SnowBased`** | 随机生成；列名固定 `uuid` |
| **按用户唯一**（每真人一行） | **`user`** | **`UserBased`** | 存 `sys_user.uuid`；**禁止**再设 `uuid` 第二主键 |
| **用户维度即可**（**无第二主键**） | **无** | **无** | 仅自增 **`id`** + 非唯一 **`user`**（及业务字段）；存取与查询靠 **`user`**（可叠加其它字段 / 联合唯一）即可满足时**不必**定义 **`uuid`**。见 **§3.4** |

**何时可不设第二主键**：数据与用户直接相关；用 **`user`** 写入与按 **`user`**（及类型、时间等）查询/list 已能覆盖业务；每用户可零条、一条或多条；唯一性可由 **`user` + 其它字段** 联合承担。**不宜**在它表以 **`uuid`** 外键引用本行、也不宜对外 API 以行级 **`uuid`** 定位单条记录——此类场景仍须 **§3.1** 的 **`uuid`**。

仍使用 **`uuid`** 第二主键时：可有**非唯一**的 **`user`** 外键列（所属/调用主体），与 **`uuid`** **同时存在**；该 **`user`** **不得** `isUnique = true`（除非整表走 **`UserBased`**）。见 **§3.5**。

### 1.1 调用主体：`SysUserEntity` 与 `RobotEntity`（业务 `user` 字段）

框架在基础结构中定义**两类**无 Session API 调用主体，均为独立实体类型，且 **`uuid` 全局不得相同**（由 **`UuidNamespaceService`** 在 `sys_user` 与 `bot_robot` 间统一分配与校验）：

| 实体 | 表 | 框架类型 | `UserContext#isRobot()` |
|------|-----|----------|-------------------------|
| **`SysUserEntity`** | `sys_user` | 真人系统用户 | `false` |
| **`RobotEntity`** | `bot_robot` | 机器人 API 身份 | `true` |

**业务层简化约定（默认接受）**：

- 业务模块通常**不**为机器人单独复制一套业务表；与「用户」相关的业务表沿用 **`user`**（或 **`owner`** 等）列表示**调用主体 / 所属主体**。
- 该列存 **32 位 `uuid` 字符串**，取值可以是 **`sys_user.uuid`** 或 **`bot_robot.uuid`**，按实际调用方写入；解析时用 **`UserContextService#getUserContext(uuid)`** 或接口注入的 **`UserContext`**，必要时 **`isRobot()`** 区分行为。
- **`RobotEntity.owner`**、**`UserBased`** 按用户唯一表的 **`user`** 等**特指真人**的列，仍只存 **`sys_user.uuid`**（见 §3.3）；勿与上述「业务泛化 `user`」混为一谈。

```java
// 业务表：同一 user 列可记录真人或机器人 uuid
@Column(length = 32, comment = "用户:调用主体uuid，可为sys_user或bot_robot")
@Index
private String user;

// 写入：开放 API 中通常取 UserContext.getUuid()
void onCreate(UserContext ctx) {
    entity.setUser(ctx.getUuid());
}
```

详见 **`docs/AI_ROBOT.md` §3.4～§3.5**；框架实现 **`cn.org.autumn.modules.sys.service.UuidNamespaceService`**、**`UserContextService`**。

### 1.2 存量代码（默认不升级）

本文 §1～§6 的纪律**默认仅约束新开发与任务明确范围内的变更**：

| 范围 | 约定 |
|------|------|
| **历史已存在** | 未按 **`uuid`** 第二主键、**`UserBased`**、标准 **`user`** 列命名/语义开发的实体与表（含 `userUuid`、`parentId` 等存量形态），**默认保持不动** |
| **默认禁止** | 仅为「对齐文档」而对无关存量批量补 **`uuid`**、改字段名、加 **`UuidBased`** / **`UserBased`**、调整 **`comment`** |
| **何时升级** | **仅当**开发任务或需求**明确要求**按第二主键 / **`user`** 标准升级**某一模块、表或实体**时，在该任务范围内按本文落地 |
| **增量合规** | **新表、新实体、新字段**及在同一实体上**实质性新增**业务列时，须按本文选型；与存量并存时勿破坏已有 API、外键与数据 |
| **扫描** | **`constraints-scan` G 组**等对存量的提示**仅参考**，不构成强制整改 |

## 2. 标记接口与生成器

| 接口 | 第二主键列 | 类型 / 生成 | 实体示例 |
|------|------------|-------------|----------|
| **`IdBased`** | — | — | 具备自增 **`id`** 的实体可实现（含第二主键实体与 **§3.4** 仅 **`user`** 无第二主键实体） |
| **`UuidBased`** | **`uuid`** | `String` 32 位 hex · **`Uuid.uuid()`** | `RobotEntity`、`RobotHookEntity` |
| **`SnowBased`** | **`uuid`** | `Long` 雪花 · **`Snow.uuid()`** | 高吞吐、需时序的 Long 业务键 |
| **`UserBased`** | **`user`** | `String` · 存用户 `uuid`，业务赋值 · **`isUnique`** | `RobotConfigEntity`（每用户一行配额） |

代码位置（`autumn-lib`）：

- `cn.org.autumn.entity.IdBased` / `UuidBased` / `SnowBased` / **`UserBased`**
- `cn.org.autumn.utils.Uuid` / `Snow`
- `cn.org.autumn.service.AutoIdService`

**`Uuid` 工具摘要**（跨项目可直接使用）：

| 方法 | 用途 |
|------|------|
| `uuid()` | 生成规范 32 位小写 hex |
| `norm(raw)` | 入库规范化（去连字符、小写） |
| `requireValid(raw)` | 规范化，非法抛错（API 入参） |
| `isValid` / `isUnset` | 格式校验 / 是否未赋值 |
| `equals(a, b)` | 忽略大小写与连字符比较 |
| `format` / `formatLower` | 带连字符展示（大写 / 小写） |
| `prefix(n)` / `prefix(uuid, n)` | 短前缀截取 |
| `toJavaUuid` / `fromJavaUuid` | 与 `java.util.UUID` 互转 |

**`SnowflakeId`** 已废弃（委托 **`Snow`**）；经 **`SnowBased` + `AutoIdService`** 或手动生成时统一使用 **`Snow`**。

## 3. 命名与 `comment` 标准

**对称纪律**：`@Column.comment` 冒号前的 **`{概念}`** / **`{概念}ID`** 与 Java 字段名的 **`{concept}`** / **`{concept}Id`** 采用相同策略——**二选一、新代码优先短形（`{概念}` / `{concept}`）、存量保持不回退**。

### 3.1 第二主键列 `uuid`

| 项 | 标准 |
|----|------|
| **Java 字段名** | 固定 **`uuid`**（`String` 或 `Long`） |
| **`String` 长度** | 默认 **`length = 32`**（`Uuid.CANONICAL_LENGTH`）；无特殊理由不得缩短 |
| **`@Column.comment`** | **`{概念}:说明`** 或 **`{概念}ID:说明`**（二选一、语义等价；与字段名 **`{concept}`** / **`{concept}Id`** 纪律对称）。**新代码优先 `{概念}`**（如 **`机器人:全局唯一业务主键`**）；存量已写 **`{概念}`** 或 **`{概念}ID`** 的**均保持**，禁止为统一互改。须满足 **`AI_STANDARDS` §10.1** 字段间简介名互异 |
| **唯一** | **`isUnique = true`**；**禁止**再叠 **`@Index`**（§10.2） |

示例：

```java
@Column(length = 32, isUnique = true, comment = "机器人:全局唯一业务主键")
private String uuid;

@Column(type = "bigint", isUnique = true, comment = "订单:全局唯一业务主键")
private Long uuid;
// 亦可：comment = "订单ID:全局唯一业务主键"（存量保持）
```

### 3.2 外键列（引用它表 `uuid`）

| 项 | 标准 |
|----|------|
| **Java 字段名** | **`{concept}`** 或 **`{concept}Id`** — `concept` 为**单个**简洁英文单词（如 `robot`、`owner`、`user`），避免复合词与过长命名 |
| **新代码** | **优先 `{concept}`**（如 `robot`、`owner`） |
| **存量 / 已改型** | 已使用 **`{concept}Id`**（或历史变体如 `parentUuid`、`userUuid`）的**保持不动**；**禁止**为「统一」回退成 `{concept}` |
| **列值** | 存**被引用方**的 **`uuid`**，**禁止**存自增 **`id`**。业务 **`user`** 列在泛化语义下可为 **`sys_user.uuid`** 或 **`bot_robot.uuid`**（§1.1）；**`owner`**（机器人表）、**`UserBased.user`** 等特指真人时仅 **`sys_user.uuid`** |
| **`String` 外键** | 默认 **`length = 32`** |
| **`@Column.comment`** | **`{概念}:说明`** 或 **`{概念}ID:说明`** — 概念对应**被引用方**；**新代码优先 `{概念}`**（如 **`机器人:对应bot_robot.uuid`**）；存量 **`{概念}`** / **`{概念}ID`** 均保持 |

示例（新代码，短名优先）：

```java
@Column(length = 32, comment = "机器人:对应bot_robot.uuid")
@Index
private String robot;

@Column(length = 32, comment = "用户:所属用户uuid")
@Index
private String owner;
```

存量兼容示例（字段名或 comment 已定型，保持不动）：

```java
@Column(length = 32, comment = "父级:对应demo_item.uuid")
@Index
private String parentUuid;   // 已存在 parentUuid，禁止改名为 parent

@Column(length = 32, comment = "父级ID:对应demo_item.uuid")
@Index
private String parentId;     // 已存在 parentId 或 comment 带 ID，禁止回退
```

### 3.3 按用户唯一：第二主键列 `user`（`UserBased`）

| 项 | 标准 |
|----|------|
| **适用** | 表业务语义为**每个真人用户至多一行**（用户配额、用户级配置等） |
| **Java 字段名** | 固定 **`user`**（`String`，`length = 32`） |
| **列值** | **仅** **`sys_user.uuid`**（真人）；**不可**填机器人 **`uuid`**；插入前由业务赋值 |
| **唯一** | **`isUnique = true`**；**禁止**再叠 **`@Index`**（§10.2） |
| **`@Column.comment`** | **`用户:说明`** 或 **`用户ID:说明`**（新代码优先 **`用户:`**） |
| **禁止** | 同时存在业务第二主键列 **`uuid`**；对 **`user`** 使用 **`AutoIdService`** 自动生成 |

```java
@Column(length = 32, isUnique = true, comment = "用户:对应sys_user.uuid，唯一")
private String user;
```

### 3.4 仅 `user`、无第二主键（用户维度数据）

当业务**不需要**独立的全局行级业务标识，且以 **`user`** 存取与查询（可配合其它字段过滤、排序、联合唯一）**已能完全满足**需求时，**不必**定义 **`uuid`** 第二主键，也**不必**实现 **`UuidBased`** / **`SnowBased`** / **`UserBased`**。

| 项 | 标准 |
|----|------|
| **适用** | 与用户直接相关的业务数据；典型为按 **`user`** 列表/筛选/统计，每用户 **0～N** 行 |
| **唯一性** | **`user`** 单独**不得** `isUnique = true`（每用户一行请用 **§3.3 `UserBased`**）；允许 **`user` + 其它字段** 联合唯一 |
| **`user` 列** | 非唯一外键；值可为 **`sys_user.uuid`** 或 **`bot_robot.uuid`**（**§1.1**）；建议 **`@Index`** 便于按用户查询 |
| **`@Column.comment`** | **`用户:说明`** 或 **`用户ID:说明`**（新代码优先 **`用户:`**） |
| **禁止** | 同时声明业务第二主键列 **`uuid`**；为实现 **`UserBased`** 却把 **`user`** 标为唯一（语义不符时每用户一行才用 **§3.3**） |
| **仍需** | 自增技术主键 **`id`**（gen CRUD） |
| **不宜选用本模式** | 它表须以稳定业务键引用本行；对外 API / 缓存 / 消息须以行级 **`uuid`** 定位；跨用户全局去重须独立业务键 |

```java
@TableName("demo_user_favorite")
@Table(comment = "用户收藏:按用户维度的多条记录")
public class DemoUserFavoriteEntity {

    @TableId
    @Column(isKey = true, type = "bigint", isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "用户:所属用户uuid，可为sys_user或bot_robot")
    @Index
    private String user;

    @Column(length = 32, comment = "目标:收藏对象uuid")
    @Index
    private String target;

    // 业务上「每用户每目标一条」时，用联合唯一而非 uuid 第二主键
    // @Indexes({ @Index(fields = {"user", "target"}, unique = true) })
}
```

### 3.5 非唯一的 `user` 外键（与 `uuid` 第二主键并存）

当表**不是**按用户唯一、**仍使用 `uuid` 第二主键**时，可增加 **`user`** 表示所属/调用主体（**§1.1**：可为 **`sys_user.uuid`** 或 **`bot_robot.uuid`**）：

| 项 | 标准 |
|----|------|
| **`user`** | **不得** `isUnique = true`；可加 **`@Index`** 便于按用户查询 |
| **与 `UserBased`** | **互斥**——有独立 `uuid` 第二主键时**不得** `implements UserBased` |

```java
@Column(length = 32, isUnique = true, comment = "标识:全局唯一业务主键")
private String uuid;

@Column(length = 32, comment = "用户:调用主体uuid，可为sys_user或bot_robot")
@Index
private String user;   // 非唯一，禁止 isUnique
```

## 4. 实体模板

### 4.1 字符串 uuid（推荐默认）

```java
@TableName("demo_item")
@Table(comment = "示例:演示双键实体")
public class DemoItemEntity implements UuidBased {

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, isUnique = true, comment = "标识:全局唯一业务主键")
    private String uuid;

    @Column(length = 32, comment = "父级:对应demo_item.uuid")
    @Index
    private String parent;  // 新代码优先 concept；存量 parentUuid 等保持不变
}
```

### 4.2 雪花 Long uuid

```java
public class DemoOrderEntity implements SnowBased {

    @TableId
    @Column(isKey = true, type = "bigint", isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(type = "bigint", isUnique = true, comment = "订单:全局唯一业务主键")
    private Long uuid;

    @Column(type = "bigint", comment = "用户:所属用户uuid")
    @Index
    private Long user;  // 新代码优先 user；存量 userUuid / userId 保持不变
}
```

**注解纪律**（详见 §3）：

- 第二主键与外键 **`comment`** 为 **`{概念}:说明`** 或 **`{概念}ID:说明`**（新代码优先 **`{概念}`**）；存量两种写法均保持。
- **`String`** 型 **`uuid` / 外键** 默认 **`length = 32`**。
- 外键字段名新代码优先 **`{concept}`**；存量 **`{concept}Id`** 不回退。
- **按用户唯一**表用 **`UserBased` + `user`**，**禁止** `uuid` 第二主键。
- **用户维度、无需行级 `uuid`** 时用 **§3.4**（仅 **`id` + `user`** 等），**禁止**多余 **`uuid`**。
- 仍有 **`uuid`** 第二主键时，**`user`** 仅作非唯一外键（**§3.5**），可与 **`uuid`** 并存。

### 4.3 按用户唯一（`UserBased`）

```java
@TableName("bot_robot_config")
@Table(comment = "用户配额:按用户覆盖机器人全局默认")
public class RobotConfigEntity implements UserBased {

    @TableId
    @Column(isKey = true, type = "bigint", isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, isUnique = true, comment = "用户:对应sys_user.uuid，唯一")
    private String user;
    // 禁止再声明 private String uuid; 作为第二主键
}
```

### 4.4 仅 `user`、无第二主键

```java
@TableName("demo_user_note")
@Table(comment = "用户笔记:按用户查询，无需uuid第二主键")
public class DemoUserNoteEntity {

    @TableId
    @Column(isKey = true, type = "bigint", isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 32, comment = "用户:所属用户uuid，可为sys_user或bot_robot")
    @Index
    private String user;

    @Column(type = "text", comment = "内容:笔记正文")
    private String content;
}
```

## 5. 自动填充（AutoIdService）

业务 **`ModuleService`** 继承链已包含 **`AutoIdService`**，无需在子类重复实现：

```
ModuleService → BaseService → DistributedService → ShareCacheService
    → BaseCacheService → BaseQueueService → AutoIdService → DialectService → ServiceImpl
```

**行为**：在 `insert*` / `insertOrUpdate*` / `update*` 写路径上，若实体 `instanceof UuidBased` 且 `uuid` 为空 → `Uuid.uuid()`；若 `instanceof SnowBased` 且 `uuid` 为 `null` 或 `0L` → `Snow.uuid()`。**已有值不覆盖**。

**不适用**：**`UserBased`** 的 **`user`** 由业务赋值，**不**经 `AutoIdService` 生成。

**非继承链**（Listener、工具类、独立组件）：持久化前调用 `AutoIdService.autoId(entity)`。

**业务显式赋值**：允许（如命名空间分配、导入幂等键）；`AutoIdService` 仅在空值时补全。

## 6. 开发约束清单

> **适用范围**：下列清单针对**新开发与任务明确要求升级的变更**；**存量代码默认不强制对齐**（**§1.2**）。

### 必须

- [ ] 实体具备自增 **`id`**
- [ ] 已判断第二主键必要性：需全局行标识 → **`uuid`** / **`UserBased`**；**仅用户维度即可** → **§3.4**（**无 `uuid`**）
- [ ] 有第二主键时：通用表 **`UuidBased`** / **`SnowBased`**；每真人一行 **`UserBased`**（**禁止**同时有 `uuid`）
- [ ] 第二主键列（若有）**`isUnique = true`**，且**不**叠 `@Index`；**§3.4** 的 **`user`** **不得**单独 unique
- [ ] 第二主键 / 外键 **`comment`** 为 **`{概念}:说明`** 或 **`{概念}ID:说明`**（新代码优先 **`{概念}`**）
- [ ] **单字段索引**在字段上 **`@Index`**；类级 **`@Indexes`** 仅用于**组合索引**（**`docs/AI_STANDARDS.md` §10.2**）
- [ ] **`String`** 型 **`uuid` / 外键** 默认 **`length = 32`**
- [ ] 新外键字段优先 **`{concept}`**；存量 **`{concept}Id`** 未擅自回退
- [ ] 对外 API / 缓存：通用表用实体 **`uuid`**；按用户唯一表用 **`user`**（或产品约定的用户维度键）
- [ ] Service 继承 **`ModuleService`**（或显式 `AutoIdService.autoId`）

### 禁止

- [ ] 用 **`id`** 作业务外键或被其它表引用
- [ ] 在对外接口暴露 **`id`** 作为资源标识（生成 CRUD 后台页除外）
- [ ] 在缓存键、消息体、领域关联中使用 **`id`**
- [ ] 按用户唯一表同时使用 **`uuid`** 第二主键与唯一 **`user`**
- [ ] 非按用户唯一表中把 **`user`** 标为 **`isUnique = true`**

- [ ] 用户维度已够用却仍冗余定义 **`uuid`** 第二主键（**§3.4** 豁免场景）
- [ ] 无第二主键却需它表 / API 以 **`uuid`** 引用本行（应改用 **§3.1**）
- [ ] 继续新增 **`SnowflakeId`** 调用（已废弃，请用 **`Snow`**）

### 多节点（SnowBased）

生产环境为每个 JVM 配置：

```text
-Dautumn.snowflake.worker-id=0
-Dautumn.snowflake.datacenter-id=0
```

各实例 **worker-id / datacenter-id 组合须唯一**（范围各 0～31）。单机开发可依赖 `Snow` 默认构造的自动推导，**集群禁止**仅靠推导。

## 7. 完整性现状（框架侧）

| 能力 | 状态 | 说明 |
|------|------|------|
| 标记接口 `IdBased` / `UuidBased` / `SnowBased` / **`UserBased`** | ✅ | `autumn-lib/entity` |
| 生成器 `Uuid` / `Snow` | ✅ | 字符串与雪花 Long |
| `AutoIdService` 写路径钩子 | ✅ | 挂于 `BaseQueueService` 继承链 |
| `ModuleService` 默认具备自动填充 | ✅ | 业务 Service 无需改基类 |
| `UuidBased` 模块示例 | ✅ | `bot_robot`、`bot_robot_hook` 等 |
| **`UserBased` 模块示例** | ✅ | `bot_robot_config` |
| `SnowBased` 模块示例 | ⚠️ 待补 | 接口与生成器就绪，尚无生产实体样例 |
| `SnowflakeId` | ⚠️ 已废弃 | 兼容层，委托 `Snow`；勿新增引用 |
| 约束扫描 G 组 | ✅ | `AUTUMN_SCAN_EXTRA=1` 时启发式提示（见 §7） |

## 8. 约束扫描（按需）

```bash
AUTUMN_SCAN_EXTRA=1 bash scripts/constraints-scan
```

- **G1**：含 `@TableId` 的 Entity 是否可能有 `isUnique` 业务键（存量误报多，仅提示）。
- **G2**：业务代码中 `Uuid.uuid()` / `Snow.uuid()` / `SnowflakeId` 使用点（插入前赋值提示）。

## 9. 文档交叉引用

| 主题 | 文档 |
|------|------|
| 约束权威 §10.4 | `docs/AI_STANDARDS.md` |
| 真人 / 机器人与业务 `user` | **§1.1**；`docs/AI_ROBOT.md` §3.5 |
| 存量代码默认不升级 | **§1.2** |
| SQL / FK / Wrapper | `docs/AI_DATABASE.md` §1.1 |
| 实体建模与三步流程 | `docs/AI_CODEGEN.md` |
| `ModuleService` 继承链 | `docs/AI_MAP.md` §2.7 |
| 日志与 API 中的 uuid | `docs/AI_CODE_STYLE.md` §8 |

## 10. 维护约定

双主键相关 API、标记接口或 `AutoIdService` 行为变更时，**同步更新本文**及 `AI_STANDARDS` §10.4、`AI_DATABASE` §1.1、两个 **`autumn-framework-*x` Skill** 自检项。
