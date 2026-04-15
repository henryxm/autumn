# 安装模式下的条件装配与自动配置排除

当 `autumn.install.wizard=true` 且不存在（或强制重装）数据源配置文件时，框架会进入**安装占位启动**（`autumn.install.mode=true`），并注入 H2 内存占位数据源等（见 `InstallEnvironmentPostProcessor`）。本文说明如何在该阶段**减少 Bean / 自动配置**的加载，加快启动并避免访问尚未就绪的真实库。

## 1. `@ConditionalOnNotInstallMode` / `@ConditionalOnInstallMode`

定义位置（`autumn-lib`）：

- `cn.org.autumn.annotation.ConditionalOnNotInstallMode` + `cn.org.autumn.install.condition.NotInstallModeCondition`
- `cn.org.autumn.annotation.ConditionalOnInstallMode` + `cn.org.autumn.install.condition.InstallModeActiveCondition`

判定依据为 **`ConditionContext.getEnvironment()`** 中的 `autumn.install.mode`，与 `InstallMode.isActive(Environment)` 一致；不依赖 `InstallMode.setRootEnvironment` 的静态绑定时机。

### 1.1 标在 `@Configuration` 类上

整块配置在安装期不解析、不注册其中任何 `@Bean`（含 `@Bean` 方法体不会执行），适合「整模块只在正式环境需要」的重型配置。

```java
@Configuration
@ConditionalOnNotInstallMode
public class HeavyBusinessConfiguration {

    @Bean
    public SomeClient someClient() {
        // 安装期不会执行
        return new SomeClient();
    }
}
```

### 1.2 标在单个 `@Bean` 方法上

仅跳过该 Bean 的注册与工厂方法执行；同配置类中其它未标注的 `@Bean` 仍会处理。

```java
@Configuration
public class MixedConfiguration {

    @Bean
    @ConditionalOnNotInstallMode
    public DataMigrationRunner migrationRunner() {
        return new DataMigrationRunner();
    }

    @Bean
    public LightweightBean lightweightBean() {
        return new LightweightBean();
    }
}
```

### 1.3 仅安装期存在的 Bean

```java
@Bean
@ConditionalOnInstallMode
public InstallOnlyHelper installOnlyHelper() {
    return new InstallOnlyHelper();
}
```

### 1.4 与现有机制的关系

| 机制 | 作用阶段 | 效果 |
|------|----------|------|
| `@ConditionalOnNotInstallMode` | Bean 定义注册前 | 不注册 Bean，**不执行** `@Bean` 工厂方法 / 不加载该 `@Configuration` |
| `InstallModeAwareCommonAnnotationBeanPostProcessor` | Bean 已实例化、依赖注入之后 | 安装期跳过 **`@PostConstruct`**（除非类型标 `AllowPostConstructDuringInstall`） |
| `InitializingBean` / `init-method` | 初始化链其它步骤 | **不受** PostConstruct 处理器影响；若需跳过请用条件装配或延后到 `ApplicationRunner` |

结论：连库逻辑若在**构造器**或 **`afterPropertiesSet`** 中执行，仅靠跳过 PostConstruct **不够**，应使用本节的条件注解或拆分配置类。

## 2. 安装引导时追加 `spring.autoconfigure.exclude`

属性 **`autumn.install.autoconfigure-exclude`**（见 `InstallConstants.AUTOCONFIGURE_EXCLUDE_EXTRA`）：**逗号分隔**的自动配置类全限定名。仅在进入安装引导并写入 `autumnInstallBootstrap` 时，与当前环境中已有的 `spring.autoconfigure.exclude` **合并**后写入引导属性源（高优先级）。

示例（`application.yml` 或与向导同级的启动配置）：

```yaml
autumn:
  install:
    wizard: true
    autoconfigure-exclude: org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration,com.example.HeavyAutoConfiguration
```

合并规则：若已配置 `spring.autoconfigure.exclude`，则在原值后追加逗号与新列表；若未配置，则仅使用本属性值。

## 3. 与 Spring Boot 手写排除的关系

仍可直接使用 Boot 官方属性 `spring.autoconfigure.exclude`；`autumn.install.autoconfigure-exclude` 便于**仅在向导场景**追加排除项，而无需维护两套完整 YAML。

## 4. 排查要点

- 条件求值时 **Environment 已包含** 各 `EnvironmentPostProcessor` 写入的属性；`InstallModeEnvironmentBindingPostProcessor` 使用 `Ordered.LOWEST_PRECEDENCE`，保证静态 `InstallMode` 与属性一致，但 **`@Conditional` 以传入的 Environment 为准**。
- 若某 Bean 仍被创建，检查是否来自 **另一配置类**、**组件扫描**的 `@Component`，或 **`@Import`** 链；条件注解需标在**实际声明**该 Bean 的类或方法上。
- `@Lazy` 只延迟**首次使用**，不能替代「安装期不注册」。
