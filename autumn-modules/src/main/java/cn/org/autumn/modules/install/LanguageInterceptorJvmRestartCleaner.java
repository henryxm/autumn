package cn.org.autumn.modules.install;

import cn.org.autumn.modules.lan.interceptor.LanguageInterceptor;
import cn.org.autumn.config.JvmRestartHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 避免静态 {@link LanguageInterceptor#lang} 指向已销毁上下文中的 {@code LanguageService}。
 */
@Component
public class LanguageInterceptorJvmRestartCleaner implements JvmRestartHandler {

    @Override
    @Order(Ordered.HIGHEST_PRECEDENCE + 200)
    public void cleanAfterContextClosed() {
        LanguageInterceptor.lang = null;
    }
}
