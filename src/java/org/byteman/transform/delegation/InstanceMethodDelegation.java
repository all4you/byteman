package org.byteman.transform.delegation;

import org.byteman.logger.InternalLogger;
import org.byteman.logger.InternalLoggerFactory;
import net.bytebuddy.implementation.bind.annotation.*;
import org.byteman.transform.InstanceMethodInterceptor;
import org.byteman.transform.InterceptorFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代理拦截器
 * 将每个方法需要增强的逻辑
 * 分发给具体的 {@link InstanceMethodInterceptor} 去执行
 *
 * @author houyi.wh
 * @since 2021-03-10
 */
public class InstanceMethodDelegation {

    private static final InternalLogger logger = InternalLoggerFactory.getLogger(InstanceMethodDelegation.class);

    private static final Map<InstanceMethodInterceptor, InstanceMethodDelegation> cache = new ConcurrentHashMap<>();

    private InstanceMethodInterceptor interceptor;

    private InstanceMethodDelegation(InstanceMethodInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public synchronized static InstanceMethodDelegation of(String className, ClassLoader classLoader) {
        InstanceMethodInterceptor interceptor = null;
        try {
            interceptor = InterceptorFactory.newInterceptor(className, classLoader);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            logger.error(e);
        }
        return of(interceptor);
    }

    public synchronized static InstanceMethodDelegation of(Class clazz, ClassLoader classLoader) {
        InstanceMethodInterceptor interceptor = null;
        try {
            interceptor = InterceptorFactory.newInterceptor(clazz, classLoader);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error(e);
        }
        return of(interceptor);
    }

    public synchronized static InstanceMethodDelegation of(InstanceMethodInterceptor interceptor) {
        if (interceptor == null) {
            return null;
        }
        InstanceMethodDelegation delegation = cache.get(interceptor);
        if (delegation != null) {
            return delegation;
        }
        delegation = new InstanceMethodDelegation(interceptor);
        cache.putIfAbsent(interceptor, delegation);
        return delegation;
    }
    

    @SuppressWarnings("unchecked")
    @RuntimeType
    public Object intercept(@This Object target, @AllArguments Object[] args, @Origin Method method, @SuperCall Callable<?> zuper) {
        Object packet = null;
        Object originResult = null;
        try {
            if (interceptor == null) {
                logger.warn("interceptor is null with method={}", method);
                return zuper.call();
            }
            // 1.调用beforeMethod方法
            packet = interceptor.beforeMethod(target, method, args);
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
