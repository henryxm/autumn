package cn.org.autumn.site;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link Factory#invoke} 须经接口 Method 派发，避免 CGLIB 实现类反射打到代理壳。
 */
class FactoryInvokeTest {

    interface InitLike {
        @Order(1)
        void init();
    }

    static class BeanWithField implements InitLike {
        String injected = "ok";
        final AtomicBoolean ran = new AtomicBoolean();

        @Override
        public void init() {
            if (injected == null) {
                throw new IllegalStateException("injected is null on invoke target");
            }
            ran.set(true);
        }
    }

    @Test
    void interfaceMethodInvoke_onCglibProxy_reachesTargetWithFields() {
        BeanWithField target = new BeanWithField();
        ProxyFactory pf = new ProxyFactory(target);
        pf.setProxyTargetClass(true);
        pf.addInterface(InitLike.class);
        Object proxy = pf.getProxy();

        GenericApplicationContext ctx = new GenericApplicationContext();
        ctx.registerBean("bean", InitLike.class, () -> (InitLike) proxy);
        ctx.refresh();

        Factory.invoke(ctx, InitLike.class, "init");
        Assertions.assertTrue(target.ran.get());
        ctx.close();
    }

    @Test
    void cglibClassMethod_onUnwrappedTarget_throwsIllegalArgument() throws Exception {
        BeanWithField target = new BeanWithField();
        ProxyFactory pf = new ProxyFactory(target);
        pf.setProxyTargetClass(true);
        Object proxy = pf.getProxy();

        Exception error = Assertions.assertThrows(Exception.class,
                () -> proxy.getClass().getMethod("init").invoke(target));
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        Assertions.assertInstanceOf(IllegalArgumentException.class, cause);
    }
}
