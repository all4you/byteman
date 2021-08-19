package org.byteman.transform.delegation;

import org.byteman.logger.InternalLogger;
import org.byteman.logger.InternalLoggerFactory;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.byteman.transform.InterceptorFactory;
import org.byteman.transform.StaticMethodInterceptor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代理拦截器
 * 将每个方法需要增强的逻辑
 * 分发给具体的 {@link StaticMethodInterceptor} 去执行
 *
 * @author houyi.wh
 * @since 2021-03-10
 */
public class StaticMethodDelegation {

    private static final InternalLogger logger = InternalLoggerFactory.getLogger(StaticMethodDelegation.class);

    private static final Map<StaticMethodInterceptor, StaticMethodDelegation> cache = new ConcurrentHashMap<>();

    private StaticMethodInterceptor interceptor;

    private StaticMethodDelegation(StaticMethodInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public synchronized static StaticMethodDelegation of(String className, ClassLoader classLoader) {
        StaticMethodInterceptor interceptor = null;
        try {
            interceptor = InterceptorFactory.newInterceptor(className, classLoader);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            logger.error(e);
        }
        return of(interceptor);
    }

    public synchronized static StaticMethodDelegation of(Class clazz, ClassLoader classLoader) {
        StaticMethodInterceptor interceptor = null;
        try {
            interceptor = InterceptorFactory.newInterceptor(clazz, classLoader);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(e);
        }
        return of(interceptor);
    }

    public synchronized static StaticMethodDelegation of(StaticMethodInterceptor interceptor) {
        if (interceptor == null) {
            return null;
        }
        StaticMethodDelegation delegation = cache.get(interceptor);
        if (delegation != null) {
            return delegation;
        }
        delegation = new StaticMethodDelegation(interceptor);
        cache.putIfAbsent(interceptor, delegation);
        return delegation;
    }

    @SuppressWarnings("unchecked")
    @RuntimeType
    public Object intercept(@Origin Class clazz, @AllArguments Object[] args, @Origin Method method, @SuperCall Callable<?> zuper) {
        Object packet = null;
        Object originResult = null;
        try {
            if (interceptor == null) {
                logger.warn("interceptor is null with method={}", method);
                return zuper.call();
            }
            // 1.调用beforeMethod方法
            packet = interceptor.beforeMethod(clazz, method, args);
            // 2.调用原方法
            originResult = zuper.call();
        } catch (Throwable throwable) {
            logger.error(throwable);
        } finally {
            // 如果beforeMethod方法返回的packet不为空
            // 则需要调用afterMethod方法，反之不需要
            if (packet != null) {
                // 3.调用afterMethod方法
                interceptor.afterMethod(packet, originResult);
            }
        }
        return originResult;
    }

}
