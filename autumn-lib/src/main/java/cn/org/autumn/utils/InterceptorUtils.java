package cn.org.autumn.utils;

import cn.org.autumn.annotation.SkipInterceptor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * 拦截器工具类
 * <p>
 * 提供检查方法是否跳过拦截器的工具方法
 *
 * @author Autumn
 */
public class InterceptorUtils {

    /**
     * 检查指定的handler是否应该跳过当前拦截器
     * <p>
     * 检查逻辑：
     * 1. 如果handler不是HandlerMethod，返回false（不跳过）
     * 2. 检查方法上的@SkipInterceptor注解
     * 3. 检查类上的@SkipInterceptor注解
     * 4. 如果注解存在且value为空，表示跳过所有拦截器
     * 5. 如果注解存在且value包含当前拦截器类，表示跳过当前拦截器
     *
     * @param handler          处理器对象
     * @param interceptorClass 当前拦截器类
     * @return true表示应该跳过，false表示不跳过
     */
    public static boolean skip(Object handler, Class<? extends HandlerInterceptor> interceptorClass) {
        if (!(handler instanceof HandlerMethod)) {
            return false;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        Class<?> beanType = handlerMethod.getBeanType();
        // 检查方法上的注解
        SkipInterceptor methodAnnotation = AnnotationUtils.findAnnotation(method, SkipInterceptor.class);
        if (methodAnnotation != null) {
            return skip(methodAnnotation, interceptorClass);
        }
        // 检查类上的注解
        SkipInterceptor classAnnotation = AnnotationUtils.findAnnotation(beanType, SkipInterceptor.class);
        if (classAnnotation != null) {
            return skip(classAnnotation, interceptorClass);
        }
        return false;
    }

    /**
     * 判断是否应该跳过拦截器
     *
     * @param annotation       SkipInterceptor注解
     * @param interceptorClass 当前拦截器类
     * @return true表示应该跳过
     */
    private static boolean skip(SkipInterceptor annotation, Class<? extends HandlerInterceptor> interceptorClass) {
        Class<?>[] skipClasses = annotation.value();
        // 如果value为空，表示跳过所有拦截器
        if (skipClasses.length == 0) {
            return true;
        }
        // 检查当前拦截器类是否在跳过列表中
        for (Class<?> skipClass : skipClasses) {
            if (skipClass.isAssignableFrom(interceptorClass) || interceptorClass.isAssignableFrom(skipClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查指定的handler是否应该跳过所有拦截器
     *
     * @param handler 处理器对象
     * @return true表示应该跳过所有拦截器
     */
    public static boolean skip(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return false;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        Class<?> beanType = handlerMethod.getBeanType();
        // 检查方法上的注解
        SkipInterceptor methodAnnotation = AnnotationUtils.findAnnotation(method, SkipInterceptor.class);
        if (methodAnnotation != null && methodAnnotation.value().length == 0) {
            return true;
        }
        // 检查类上的注解
        SkipInterceptor classAnnotation = AnnotationUtils.findAnnotation(beanType, SkipInterceptor.class);
        if (classAnnotation != null && classAnnotation.value().length == 0) {
            return true;
        }
        return false;
    }
}
