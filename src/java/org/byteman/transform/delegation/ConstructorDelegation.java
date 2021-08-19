package org.byteman.transform.delegation;

import org.byteman.logger.InternalLogger;
import org.byteman.logger.InternalLoggerFactory;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.byteman.transform.ConstructorInterceptor;
import org.byteman.transform.InterceptorFactory;
import org.byteman.transform.StaticMethodInterceptor;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代理拦截器
 * 将每个方法需要增强的逻辑
 * 分发给具体的 {@link StaticMethodInterceptor} 去执行
 *
 * @author houyi.wh
 * @since 2021-03-10
 */
public class ConstructorDelegation {

    private static final InternalLogger logger = InternalLoggerFactory.getLogger(ConstructorDelegation.class);

    private static final Map<ConstructorInterceptor, ConstructorDelegation> cache = new ConcurrentHashMap<>();

    private ConstructorInterceptor interceptor;

    private ConstructorDelegation(ConstructorInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public static ConstructorDelegation of(String className, ClassLoader classLoader) {
        ConstructorInterceptor interceptor = null;
        try {
            interceptor = InterceptorFactory.newInterceptor(className, classLoader);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            logger.error(e);
        }
        return of(interceptor);
    }

    public static ConstructorDelegation of(Class clazz, ClassLoader classLoader) {
        ConstructorInterceptor interceptor = null;
        try {
            interceptor = InterceptorFactory.newInterceptor(clazz, classLoader);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(e);
        }
        return of(interceptor);
    }

    public synchronized static ConstructorDelegation of(ConstructorInterceptor interceptor) {
        if (interceptor == null) {
            return null;
        }
        ConstructorDelegation delegation = cache.get(interceptor);
        if (delegation != null) {
            return delegation;
        }
        delegation = new ConstructorDelegation(interceptor);
        cache.putIfAbsent(interceptor, delegation);
        return delegation;
    }

    @SuppressWarnings("unchecked")
    @RuntimeType
    public void intercept(@Origin Constructor<?> constructor, @AllArguments Object[] args) {
        if (interceptor == null) {
            logger.warn("interceptor is null with constructor={}", constructor);
            return;
        }
        try {
            // 1.调用beforeMethod方法
            interceptor.onConstructor(constructor, args);
        } catch (Throwable throwable) {
            logger.error(throwable);
        }
    }

}
