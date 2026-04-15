package cn.org.autumn.install.condition;

import cn.org.autumn.install.InstallMode;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link ConditionalOnNotInstallMode} 的判定：仅当 {@code autumn.install.mode} 不为 true 时匹配。
 */
public class NotInstallModeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !InstallMode.isActive(context.getEnvironment());
    }
}
