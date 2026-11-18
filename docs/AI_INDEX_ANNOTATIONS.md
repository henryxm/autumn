# 实体索引注解：业务仓标准写法与推荐方案

> **权威约束**仍在 **`docs/AI_STANDARDS.md` §10.2**（硬性纪律、ASC/DESC 基线）。  
> **本文**面向业务仓：把「主查询 SQL → `@Index` / `@Indexes`」落成**统一风格**的推荐清单与评审表，便于 `vshort` / `im` / `account` 等同构实现。  
> 框架收集与同步行为见 **`docs/AI_MAP.md` §2.10**、**`docs/AI_BOOT.md` §3**。

## 1. 文档职责

| 文档 | 读什么 |
|------|--------|
| **`AI_STANDARDS` §10.2** | 硬性禁止项、默认 ASC、何时写 DESC、命名/身份摘要 |
| **本文** | 业务建模流程、模式目录、**§5 isUnique**、**§7 命名与身份判定**、冗余裁剪、PR 清单 |
| **`AI_MAP` §2.10** | 注解收集、前缀策略、方言降级、**指纹配对 / RENAME** |
| **`AI_BOOT` §3** | 启动同步顺序、`Index rebuild upgrade` / rename 日志 |

**索引只写在 Entity**；Dao / `*DaoSql` **禁止**声明索引。查询写在 Provider，索引按查询设计。

## 2. 建模流程（业务仓统一）

对每一张表、每一族高频 SQL：

1. **写出主查询**（WHERE + ORDER BY；含 `limitOne` 的 tie-break `id`）。  
2. **拆列角色**（从左到右）：  
   - **等值 / IN**（`status=?`、`user=?`、频道开关 `xsign=1`）  
   - **范围**（`create < ?`、`expire_at < now`）  
   - **排序键**（`ORDER BY living_at DESC, id DESC`）  
3. **落注解**：  
   - 单列查找 → 字段 `@Index`  
   - **单列业务唯一 → 仅字段 `@Column(isUnique = true)`（以字段为准；禁止再 `@Index` / 禁止「唯一列最左」冗余组合）**  
   - 多列业务唯一 → 类级 `indexType = UNIQUE`（参与列不要再单列 `isUnique`）  
   - ≥2 列普通组合 → 类级 `@Indexes` + `@IndexField` **按上列顺序**（`field` **优先驼峰属性名**）  
   - `@Index(name)`：可读压缩语义；可空（框架派生）；仅改名不制造重建（§7）  
4. **方向**：默认 ASC；仅对 **ORDER BY 中的排序键** 写 `IndexOrder.DESC`（等值列保持 ASC）。  
5. **冗余**：若组合最左前缀已覆盖某字段级 `@Index` 的同一查询族，删除字段级；若组合含 `isUnique` 列，按 **§5** 整理。  
6. **注释**：组合索引上方用一行注释写清服务的 Dao/Service 方法或 SQL 形态（便于 Review）。

## 3. 硬性约束（摘要，细节以 STANDARDS 为准）

1. **单列唯一以字段为准**：`@Column(isUnique = true)` 是该列唯一约束的**唯一合法声明**；**禁止**再叠字段 `@Index`；**禁止**再用类级组合表达「该唯一列 + 附带过滤」的冗余最左前缀（见 **§5**）。  
2. 单列普通索引 → **字段** `@Index`；类级 `@Indexes` **仅**组合（≥2 列）或 FULLTEXT/SPATIAL/部分索引。  
3. `@IndexField` **数组顺序 = 落库顺序**；列序优先于 ASC/DESC。  
4. `IndexOrder.DESC` **仅**排序键，且对齐主查询 `ORDER BY`；禁止无 EXPLAIN 全库批量改 DESC。  
5. 宽字符串（如 `VARCHAR(1000)`）参与组合索引时用 **`@IndexField(length = 191)`**（或字段 `@Index(length = n)`），勿依赖 `@Column.length` 当前缀。  
6. Dao 层不写索引注解。  
7. **索引身份不含 name**：列序/方向相同仅改 name → 启动 **RENAME**（§7.3），禁止靠改名触发无意义重建。

## 4. 模式目录（SQL → 注解）

下列模式为**推荐统一风格**；业务仓新索引优先套用，再按表微调。

### 4.1 单列等值查找

```text
WHERE stream = ?
```

```java
@Column(comment = "流名:SRS stream", length = 64)
@Index
private String stream;
```

### 4.2 业务第二主键唯一

```java
@Column(comment = "场次:业务主键", length = 32, isUnique = true)
private String uuid;   // 禁止再 @Index
```

### 4.3 等值组合（关系表 / 状态过滤）

```text
WHERE user = ? AND status IN ('created','living')
ORDER BY id DESC LIMIT 1
```

```java
@Indexes(@Index(name = "userstatus", fields = {
    @IndexField(field = "user"),
    @IndexField(field = "status"),
    @IndexField(field = "id", order = IndexOrder.DESC)
}))
```

等值列 ASC；仅 tie-break / 排序键可 DESC。

### 4.4 列表：等值 + 时间倒序（高频推荐）

```text
WHERE status = 'living'
ORDER BY living_at DESC, id DESC
```

```java
@Index(name = "statuslivingat", fields = {
    @IndexField(field = "status"),
    @IndexField(field = "livingAt", order = IndexOrder.DESC),
    @IndexField(field = "id", order = IndexOrder.DESC)
})
```

带类目过滤时：`status → category → livingAt DESC → id DESC`。

### 4.5 多排序键热度列表

```text
WHERE status = 'living'
ORDER BY pin_order ASC, reorder DESC, living_at DESC, id DESC
```

```java
@Index(name = "statuspinreorderliving", fields = {
    @IndexField(field = "status"),
    @IndexField(field = "pinOrder"),                          // ASC
    @IndexField(field = "reorder", order = IndexOrder.DESC),
    @IndexField(field = "livingAt", order = IndexOrder.DESC),
    @IndexField(field = "id", order = IndexOrder.DESC)
})
```

说明：SQL 中若含 `CASE WHEN pin_order > 0 …` 等表达式，索引无法完全同构；仍按**稳定排序列**建组合，接受表达式列走 filesort/额外过滤。

### 4.6 频道 / 开关等值列必须靠左

错误（等值列在范围/过滤之后）：

```text
(audit, visible, xsign)   // eq xsign=1 时难以用上最左前缀
```

正确：

```text
WHERE xsign = 1 AND visible >= ? AND audit >= ?
ORDER BY reorder DESC, id DESC
```

```java
@Index(name = "xsignvisibleauditreorder", fields = {
    @IndexField(field = "xsign"),
    @IndexField(field = "visible"),
    @IndexField(field = "audit"),
    @IndexField(field = "reorder", order = IndexOrder.DESC),
    @IndexField(field = "id", order = IndexOrder.DESC)
})
```

`community` / `apple` 等同理，各建一族索引，**不要**把互斥频道列塞进同一索引中间。

### 4.7 范围条件 + 排序

```text
WHERE status = 0 AND create < ?
ORDER BY id ASC
```

```java
@Index(name = "statuscreate", fields = {
    @IndexField(field = "status"),
    @IndexField(field = "create"),   // 范围：ASC
    @IndexField(field = "id")
})
```

### 4.8 宽 URL / 长字符串等值

```text
WHERE url = ? AND status = ? ORDER BY id DESC
```

```java
@Index(name = "urlstatusid", fields = {
    @IndexField(field = "url", length = 191),
    @IndexField(field = "status"),
    @IndexField(field = "id", order = IndexOrder.DESC)
})
```

勿对超长列做无前缀的组合索引（易触 InnoDB 键长上限）。

### 4.9 投放位 / 目录排序（正序）

```text
WHERE position = ? AND enabled = 1
ORDER BY sort_order ASC, id ASC
```

```java
@Index(name = "positionenabled", fields = {
    @IndexField(field = "position"),
    @IndexField(field = "enabled"),
    @IndexField(field = "sortOrder"),
    @IndexField(field = "id")
})
```

### 4.10 组合唯一（关系去重）

```text
UNIQUE(user, value)   // 关注/拉黑等
```

```java
@Index(name = "uservalue", fields = {
    @IndexField(field = "user"),
    @IndexField(field = "value")
}, indexType = IndexTypeEnum.UNIQUE, indexMethod = IndexMethodEnum.BTREE)
```

单列唯一仍只用 `isUnique = true`。

### 4.11 多排序键优选（认证主展示）

```text
WHERE user=? AND enable=1 AND black=0
ORDER BY preferred DESC, priority DESC, update DESC
```

```java
@Index(name = "userenableblack", fields = {
    @IndexField(field = "user"),
    @IndexField(field = "enable"),
    @IndexField(field = "black"),
    @IndexField(field = "preferred", order = IndexOrder.DESC),
    @IndexField(field = "priority", order = IndexOrder.DESC),
    @IndexField(field = "update", order = IndexOrder.DESC)
})
```

## 5. `isUnique` 优先与组合告警（统一整理方案）

### 5.1 原则：字段注解为准

单字段若已声明 **`@Column(isUnique = true)`**：

1. **唯一性真相源** = 该字段注解（框架生成单列 `UNIQUE KEY`）。  
2. **不得**再对该字段写字段级 `@Index`（硬禁止；框架剔除）。  
3. **通常不得**再以该类级组合索引把该列放在**最左**（或整体仅为「唯一列 + 状态过滤」）——应**删除该组合**，保留字段 `isUnique`。  
4. 多列业务唯一（如关注关系 `(user, value)`）→ **不要**给其中一列单独 `isUnique`；改用类级 `indexType = UNIQUE`。

### 5.2 启动 WARN 含义

日志形如：

```text
实体 […] 组合索引 [xxx] 含已 isUnique 列 [uuid]，已保留声明顺序；建议去掉冗余唯一首列以免语义重叠。
```

| 项 | 说明 |
|----|------|
| 是否阻断启动 | **否**；框架对组合**保留**声明，仅 WARN |
| 触发条件 | 类级组合（≥2 列）的 `fields` 中出现已 `isUnique` 的列名 |
| 单列类级含 isUnique | 框架**直接剔除**该列（更强），并另有 WARN |
| 业务动作 | 按 **§5.3** 决策表整理实体，消掉 WARN、统一风格 |

### 5.3 原因（为何要整理）

- 单列 `UNIQUE(uuid)` 上 `WHERE uuid=?` 最多 1 行；再拼 `visible`/`audit`/`enable`/`user` 等**不能**提升该点查，只增加一把索引与写放大。  
- 「`UNIQUE(uuid)` + 组合 `(uuid, …)`」语义重叠：阅读实体时不清楚以哪条为准；启动每次刷 WARN。  
- 约定 **字段 `isUnique` 优先**，组合侧让位，业务仓评审标准一致。

### 5.4 决策表（统一整理）

| 情况 | 做法 | 示例 |
|------|------|------|
| **A.** `isUnique` 列在组合**最左**，主查询是「按该唯一键 ± 附带过滤」 | **删除整段组合**；保留字段 `isUnique` | `(uuid, visible, audit)`、`(vuuid, audit)`、`(account, enable)`、`(user, enable)` 且 `user`/`uuid`/`vuuid`/`account` 已 `isUnique` |
| **B.** 真实唯一是**多列**，单列并不唯一 | **去掉**单列 `isUnique`；保留类级 `UNIQUE` 组合 | 关注/拉黑 `UNIQUE(user, value)` |
| **C.** 列表查询的等值列**不是**唯一列（唯一列出现在组合非最左，或曾想塞进组合） | **不要**把唯一列挪到最左；按 **§5.6** 评估：多数应**从组合中去掉唯一列**，最左留给真正的列表谓词 | 见 §5.6；反例 `(enable, uuid)`，正例 `(enable, expireTime)` |
| **D.** 组合本身是 `UNIQUE(uniqueCol, other)` 且 `uniqueCol` 已单列唯一 | **删除组合 UNIQUE**；只留字段 `isUnique`（`other` 无法构成第二行） | `UNIQUE(vuuid, enable)` 且 `vuuid` 已 `isUnique` |

**默认落点**：业务第二主键（`uuid` / `vuuid` / `user` / `account` 等）→ **只**字段 `isUnique`；不要为「唯一键 + 状态」再建最左带唯一列的组合。

### 5.5 正反例

```java
// ✅ 单列业务主键：字段为准
@Column(length = 32, comment = "视频:业务主键", isUnique = true)
private String uuid;

// ❌ A：再叠「唯一键 + 过滤」组合（应删组合）
// @Index(name = "uuidvisibleaudit", fields = {
//     @IndexField(field = "uuid"), @IndexField(field = "visible"), @IndexField(field = "audit") })

// ✅ 多列唯一：不要给 user 单独 isUnique
@Index(name = "uservalue", fields = {
    @IndexField(field = "user"), @IndexField(field = "value")
}, indexType = IndexTypeEnum.UNIQUE)

// ❌ C 误区：列表按 enable，却把唯一 uuid 塞进组合（或幻想把 uuid 调到最左）
// @Index(name = "enableuuid", fields = {
//     @IndexField(field = "enable"), @IndexField(field = "uuid") })

// ✅ C 正解：最左 = 列表等值/范围谓词；组合内不要带 isUnique 列
// listExpiredEnabled: WHERE enable=1 AND expire_time < ? ORDER BY expire_time
@Index(name = "enableexpire", fields = {
    @IndexField(field = "enable"),
    @IndexField(field = "expireTime")
})
```

### 5.6 C 类深评：为何「把唯一列调到最左」通常不是最优

核心事实：**索引最左列必须能被 WHERE 以等值（或可靠前缀）用上**。唯一列再「选择性高」，若本次 SQL **没有** `唯一列 = ?`，挪到最左也**用不上**该索引做列表扫描。

#### 5.6.1 两种查询族不可混为一谈

| 查询族 | 典型 SQL | 最优索引 |
|--------|----------|----------|
| **点查** | `WHERE uuid = ?`（± `AND enable=1` 内存/二次过滤即可） | **仅**字段 `isUnique`；**禁止**再组合 |
| **列表 / 扫段** | `WHERE enable = 1 AND expire_time < ? ORDER BY expire_time` | 最左 = `enable`（或更选择性的列表谓词），后续 = 范围/排序列；**不要**塞 `uuid` |

点查已经有 UNIQUE；列表**按定义**不知道（或不先按）唯一键 → 无法也不应「改写成最左唯一列」来加速。

#### 5.6.2 「优化成唯一列最左」何时成立、何时不成立

| 想法 | 是否成立 | 说明 |
|------|----------|------|
| 把 `(enable, uuid)` 改成 `(uuid, enable)` 让列表更快 | **不成立** | 列表无 `uuid=?` 时，`(uuid, …)` 对 `WHERE enable=?` **整段无法用**（退回全表或别的索引） |
| 改业务 SQL，先拿到 uuid 再点查 | **仅当**本来就能拿到唯一键 | 变成点查族 → 走 UNIQUE，**删组合**（回 A）；不是「保留组合并把唯一列左移」 |
| 列表必须扫「所有 enable=1」 | **成立的是另一条索引** | 最左用 `enable`（或 `status` 等），后缀用真正过滤/排序列；**省略唯一列** |
| 组合里带唯一列当 covering / 稳定排序 | **极少需要** | InnoDB 二级索引已带主键；再挂 `uuid` 多半重复、还触发 WARN；优先 `id` / 业务排序列 |

结论：**C 的最优解不是「保留含唯一列的组合」或「左移唯一列」**，而是：

1. **点查** → 只留字段 `isUnique`（必要时 SQL 仍写 `AND enable=1`，成本可忽略）。  
2. **列表** → 另建**不含** `isUnique` 列的组合，最左对齐列表等值列，后缀对齐范围/`ORDER BY`。  
3. 若发现「想依赖唯一列最左才能快」→ 说明 SQL 应改成点查，或列表谓词选错了。

#### 5.6.3 用 `UserLoginLimit` 对照

```text
getUuid / getEnabledByUuid:  WHERE uuid = ? [AND enable = 1]
  → UNIQUE(uuid) 足够；不要 (uuid, enable) / (enable, uuid)

getAccount:  WHERE account = ? AND enable = ?
  → UNIQUE(account) 点查后滤 enable；不要 (account, enable)

listExpiredEnabled:  WHERE enable = 1 AND expire_time < ? ORDER BY expire_time
  → ✅ (enable, expireTime)；❌ (enable, uuid)（uuid 不参与谓词/排序）；❌ (uuid, enable)（列表无 uuid）
```

#### 5.6.4 C 类落地检查（评审用）

1. 写出主 SQL：有没有 **`唯一列 = ?`**？  
   - 有 → 点查；删含该列的组合（A/D）。  
   - 无 → 列表；组合最左必须是列表等值列，**字段列表里不要出现 isUnique 列**。  
2. 禁止用「把唯一列调到最左」当作列表优化手段。  
3. 整理后目标：启动日志**不再**出现 `含已 isUnique 列`（组合根本不含唯一列）。

## 6. 字段级 vs 组合：冗余裁剪（非 isUnique）

| 情况 | 做法 |
|------|------|
| 仅有单列 `WHERE user=?`（且 **非** `isUnique`） | 保留字段 `@Index` |
| 已有 `(user, status, …)` 且无「仅 user」孤立热点 | **删除**字段 `@Index` on `user` |
| 字段 `@Index` 与类级单列同构 | 禁止；类级单列本就不该存在 |
| 字段 `@Index` 与组合**最左一列**相同，但另有其它查询只打该列 | 可保留字段索引；或合并评估后删一留一 |
| `pinOrder` / `reorder` 仅出现在组合排序中 | **不要**再单独字段 `@Index` |
| 字段已 `isUnique` | **不要**再字段 `@Index`；组合整理见 **§5** |

原则：**一个查询族尽量一把索引**；避免「组合 + 同族最左单列」双开。`isUnique` 场景优先走 **§5**，不以「覆盖索引」为由保留最左唯一列组合。

## 7. 索引命名与身份判定（规程）

### 7.1 `@Index(name)` 

- 推荐：小写、压缩列语义、**可无下划线**（`statuslivingat`、`usertagplace`、`xsignvisibleauditreorder`）。  
- 名称应能看出**列序意图**（读名可知大致 fields 顺序）。  
- **允许为空**：框架按列 underline 名去 `_` 后拼接派生，再走长度截断。  
- **改列序或 ASC/DESC 可保留原名**；列集合/角色大变再改名。  
- **仅改名不再被当成新索引**：定义指纹相同、名字不同 → MySQL `RENAME INDEX`（失败则成对 DROP+ADD），不会「ADD 新名 + REMOVE 旧名」双开。  
- 超长名（默认 **64**）：自动截断（前缀 + `_` + 8 位 hash）并 WARN，**不阻断**启动。  
- 不要用无意义名如 `idx1`；禁止依赖「改名制造重建」。

### 7.2 `@IndexField(field)` 

- **推荐写实体 Java 属性驼峰**（与字段名一致）：`createAt`、`userTag`。  
- **兼容库列下划线**：`create_at`、`user_tag`（框架 `HumpToUnderline`，二者等价）。  
- 比对用列 `identityKey`（去 `_` + lower）；禁止把两种写法当成「不同列」。

```java
@Index(name = "usertagplace", fields = {
    @IndexField(field = "user"),
    @IndexField(field = "tag"),
    @IndexField(field = "place"),
    @IndexField(field = "createAt")  // 或 create_at，等价
})
```

### 7.3 库↔实体「是否同一索引」

| 层级 | 规则 |
|------|------|
| **身份（指纹）** | 类型/方法 + 有序列规格（`identityKey` + order + 前缀软匹配）等，**不含 name** → `sameIndexColumnsAndKind` |
| **名字配对** | 规范化：`lower` + 去 `_`；另认 `表名_逻辑名` 别名 |
| **配对顺序** | 先规范化名 → 再在剩余库索引中**唯一**指纹匹配；一对多则 WARN 并保守 ADD |
| **同指纹异名** | 原因 `NAME` → 优先 `RENAME INDEX` |
| **同名异定义** | 原因 `ORDER` / `DEFINITION` → 成对 DROP+ADD |

#### 7.3.1 仅 name 不同、列序与 ASC/DESC 均相同（重启行为）

```text
库：  KEY `oldname` (user, tag, place, create_at)
实体：@Index(name = "usertagplace", 相同列序/方向)
```

1. 规范化名配对失败（`oldname` ≠ `usertagplace`）。  
2. 指纹唯一命中 → `classifyAfterPair` 返回 `Reason.NAME`。  
3. 执行 `ALTER TABLE … RENAME INDEX \`oldname\` TO \`usertagplace\``（失败则同任务内 DROP+ADD）。  
4. **不会**把旧索引当孤儿 REMOVE、同时 ADD 新名（避免双开）。  
5. 再次启动：名与指纹均一致 → 跳过。

## 8. 注解旁注释（推荐）

```java
@Indexes({
    // livingNew / followingLiving：status(+category) + ORDER BY living_at DESC, id DESC
    @Index(name = "statuslivingat", fields = { ... }),
})
```

注释写：**方法名或 SQL 意图 + 关键谓词 + ORDER BY**，禁止只写「索引」。

## 9. 与框架同步的行为（业务须知）

- 启动 `autumn.table.auto=update` 时比对实体与库；身份见 **§7.3**。  
- 列序/方向不一致默认 **成对 DROP+ADD**（`autumn.table.index.rebuild-on-order-mismatch=true`）。  
- 仅改 `@Index(name)`：优先 **RENAME**；日志含 `reason=NAME` / `Index rename`。  
- 日志：`Index rebuild upgrade: ok=…`；失败 WARN 不阻断启动。  
- 索引名超长：`索引名超长已截断`；名为空：`索引名为空已按列派生`。  
- **`含已 isUnique 列 … 建议去掉冗余唯一首列`**：不阻断；按 **§5** 整理实体后消失。  
- 大表改 DESC/加宽组合：低峰发布，或临时 `rebuild-on-order-mismatch=false` 后手工 DDL（逃生舱，事后改回 true）。  
- 元数据已批量预载；健康路径下索引比对不应主导启动耗时。

## 10. PR / Agent 评审清单

- [ ] 每个新/改组合索引能否指出**主查询**（文件+方法或 SQL）？  
- [ ] `@IndexField` 顺序是否 = 等值 → 范围 → 排序？  
- [ ] `DESC` 是否只出现在 ORDER BY 排序键？等值列是否仍 ASC？  
- [ ] `ORDER BY …, id DESC` 是否在索引末尾体现（若该查询稳定使用）？  
- [ ] **单列唯一是否只写字段 `isUnique`**（未叠字段 `@Index`）？  
- [ ] 组合是否含 `isUnique` 列？若含：是否按 **§5.4 / §5.6** 处理（点查删组合；列表组合**去掉**唯一列，禁止「唯一列调最左」伪优化）？  
- [ ] 是否删除已被组合最左前缀覆盖的字段 `@Index`？  
- [ ] 宽字符串是否使用 `length` 前缀？  
- [ ] Dao 是否仍无索引注解？  
- [ ] 索引名是否可读？是否有一行注释对齐查询？`@IndexField` 是否优先驼峰属性名？  
- [ ] 索引名是否未无故超长（超长会被截断 WARN）？  
- [ ] 启动日志是否仍刷 `含已 isUnique 列` WARN（新改动应清零或仅剩有注释的 C 类）？

## 11. 反模式

| 反模式 | 后果 |
|--------|------|
| 类级 `@Indexes` 只含单列 | 违反 §10.2；应改字段 `@Index` |
| `isUnique` + 字段 `@Index` | 重复 UNIQUE；框架剔除字段索引 |
| `isUnique` 列作组合最左 + 附带过滤 | 启动 WARN；索引冗余；应删组合（§5 A/D） |
| 列表组合挂唯一列 / 幻想「唯一列调最左」加速列表 | 最左用不上或语义重叠；应按 §5.6 拆成点查 UNIQUE + 列表专用组合 |
| 多列唯一却给其中一列 `isUnique` | 与组合 UNIQUE 冲突；应去掉单列 `isUnique`（§5 B） |
| 频道等值列放在组合中间/右侧 | 最左前缀失效 |
| 凡 `ORDER BY DESC` 就全列 DESC | 无收益、放大重建 |
| 超长 URL 无前缀进组合 | 键长失败或隐式截断风险 |
| 无主查询的「预防性」十几个组合 | 写放大、重建慢 |
| 靠改 `@Index(name)`「制造」索引重建 | 现已指纹配对 + RENAME，无意义且噪音大 |
| 在 Dao 上幻想索引 | 框架不识别 |

## 12. 维护

- 约束变更 → 先改 **`AI_STANDARDS` §10.2**，再同步本文模式与清单（含 **§5**）。  
- 框架收集/同步行为变更 → 同步 **`AI_MAP` §2.10**、**`AI_BOOT` §3**。  
- 业务仓（如 vshort）落地范例变更时，优先把**可复用模式**抽回本文，而不是只留在业务注释里。
