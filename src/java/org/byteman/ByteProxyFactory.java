package org.byteman;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.matcher.ElementMatcher;
import org.byteman.logger.InternalLogger;
import org.byteman.logger.InternalLoggerFactory;
import org.byteman.matcher.MethodMatcher;
import org.byteman.matcher.TypeMatcher;
import org.byteman.transform.*;
import org.byteman.transform.advice.ConstructorAdvice;
import org.byteman.transform.advice.InstanceMethodAdvice;
import org.byteman.transform.advice.StaticMethodAdvice;
import org.byteman.transform.delegation.ConstructorDelegation;
import org.byteman.transform.delegation.InstanceMethodDelegation;
import org.byteman.transform.delegation.StaticMethodDelegation;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * 实现动态代理，创建代理对象
 *
 * @author houyi.wh
 * @since 2021-07-21
 * @since 0.0.1
 */
public interface ByteProxyFactory {

    /**
     * 根据目标类、目标方法、拦截器
     * 创建一个代理对象
     *
     * @param typeMatcher   目标类
     * @param methodMatcher 目标方法
     * @param interceptor   代理的拦截器
     * @return 代理对象
     */
    Object buildProxy(TypeMatcher typeMatcher, MethodMatcher methodMatcher, Class<? extends Interceptor> interceptor);

    /**
     * 根据目标类、目标方法、拦截器
     * 创建一个代理对象
     *
     * @param typeMatcher   目标类
     * @param methodMatcher 目标方法
     * @param classLoader   类加载器
     * @param interceptor   代理的拦截器
     * @return 代理对象
     */
    Object buildProxy(TypeMatcher typeMatcher, MethodMatcher methodMatcher, ClassLoader classLoader, Class<? extends Interceptor> interceptor);

    /**
     * 根据目标类、目标方法、拦截器
     * 创建一个代理对象
     *
     * @param typeMatcher   目标类
     * @param methodMatcher 目标方法
     * @param interceptor   代理的拦截器
     * @param proxyType     代理对象的类型
     * @return 代理对象
     */
    <T> T buildProxy(TypeMatcher typeMatcher, MethodMatcher methodMatcher, Class<? extends Interceptor> interceptor, Class<T> proxyType);

    /**
     * 根据目标类、目标方法、拦截器
     * 创建一个代理对象
     *
     * @param typeMatcher   目标类
     * @param methodMatcher 目标方法
     * @param classLoader   类加载器
     * @param interceptor   代理的拦截器
     * @param proxyType     代理对象的类型
     * @return 代理对象
     */
    <T> T buildProxy(TypeMatcher typeMatcher, MethodMatcher methodMatcher, ClassLoader classLoader, Class<? extends Interceptor> interceptor, Class<T> proxyType);

    enum Factories implements ByteProxyFactory {

        /**
         * 通过Advice实现
         */
        ByAdvice(true, false),

        /**
         * 通过Delegation实现
         * 且使用对象缓存
         */
        ByAdviceWithCache(false, true),

        /**
         * 通过Delegation实现
         */
        ByDelegation(false, false),

        /**
         * 通过Delegation实现
         * 且使用对象缓存
         */
        ByDelegationWithCache(false, true),;

        private boolean useAdvice;
        private boolean useCache;

        Factories(boolean useAdvice, boolean useCache) {
            this.useAdvice = useAdvice;
            this.useCache = useCache;
        }

        private static final InternalLogger logger = InternalLoggerFactory.getLogger(Factories.class);

        private static final Object lock = new Object();

        private static final TypeCache<Class> typeCache = new TypeCache<>(TypeCache.Sort.SOFT);

        private static final Map<Class<?>, Object> objectCache = new ConcurrentHashMap<>();


        @Override
        public Object buildProxy(TypeMatcher typeMatcher, MethodMatcher methodMatcher, Class<? extends Interceptor> interceptor) {
            return buildProxy(typeMatcher, methodMatcher, (ClassLoader) null, interceptor);
        }

        @Override
        public Object buildProxy(TypeMatcher typeMatcher, MethodMatcher methodMatcher, ClassLoader classLoader, Class<? extends Interceptor> interceptor) {
            if (typeMatcher == null || typeMatcher.getDescription() == null || methodMatcher == null || methodMatcher.getDescription() == null || interceptor == null) {
                return null;
            }
            ClassLoader realClassLoader = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
            try {
                // 创建Class并缓存起来
                Class<?> clazz = typeCache.findOrInsert(realClassLoader, typeMatcher.getDescription().getClass(), new Callable<Class<?>>() {
                    @Override
                    public Class<?> call() throws Exception {
                        return buildInterceptedClass(typeMatcher, methodMatcher, realClassLoader, interceptor);
                    }
                }, lock);
                // 代理对象
                Object proxy;
                if (useCache) {
                    synchronized (this) {
                        proxy = objectCache.get(clazz);
                        if (proxy == null) {
                            // 创建代理对象并缓存
                            proxy = clazz.newInstance();
                            objectCache.putIfAbsent(clazz, proxy);
                        }
                    }
                } else {
                    // 每次都新创建代理对象
                    proxy = clazz.newInstance();
                }
                return proxy;
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("buildProxy failed cause={}", e.getMessage(), e);
                return null;
            }
        }

        @Override
        public <T> T buildProxy(TypeMatcher typeMatcher, MethodMatcher methodMatcher, Class<? extends Interceptor> interceptor, Class<T> proxyType) {
            return buildProxy(typeMatcher, methodMatcher, null, interceptor, proxyType);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T buildProxy(TypeMatcher typeMatcher, MethodMatcher methodMatcher, ClassLoader classLoader, Class<? extends Interceptor> interceptor, Class<T> proxyType) {
            Object proxy = buildProxy(typeMatcher, methodMatcher, classLoader, interceptor);
            return proxy == null ? null : (T) proxy;
        }

        private Class buildInterceptedClass(TypeMatcher typeMatcher, MethodMatcher methodMatcher, ClassLoader classLoader, Class<? extends Interceptor> interceptor) {
            TypeDescription typeDescription = typeMatcher.getDescription();
            ElementMatcher<MethodDescription> elementMatcher = parseElementMatcher(interceptor, methodMatcher);
            // 构造builder
            DynamicType.Builder builder = new ByteBuddy().subclass(typeDescription);
            if (useAdvice) {
                String interceptorName = interceptor.getName();
                Class advice = parseAdvice(interceptor);
                builder = builder
                        .method(elementMatcher)
                        .intercept(Advice
                                .withCustomMapping()
                                .bind(InterceptorName.class, new TextConstant(interceptorName), String.class) // 将拦截器类名绑定到注解上
                                .to(advice) // 在advice中将增强的代码委托给interceptor
                                .wrap(SuperMethodCall.INSTANCE)); // 对匹配到的method执行intercept拦截，具体的拦截逻辑，是通过wrap包装Advice实现的
                logger.info("buildProxy byAdvice with typeDescription={}, elementMatcher={}, advice={}", typeDescription, elementMatcher, advice);
            } else {
                Object delegation = parseDelegation(interceptor, classLoader);
                builder = builder
                        .method(elementMatcher)
                        .intercept(MethodDelegation.to(delegation));
                logger.info("buildProxy byDelegation with typeDescription={}, elementMatcher={}, delegation={}", typeDescription, elementMatcher, delegation);
            }
            return builder.make()
                    .load(classLoader)
                    .getLoaded();
        }

        private static ElementMatcher<MethodDescription> parseElementMatcher(Class interceptor, MethodMatcher methodMatcher) {
            ElementMatcher<MethodDescription> matcher = methodMatcher.getDescription();
            // 普通方法
            if (InstanceMethodInterceptor.class.isAssignableFrom(interceptor)) {
                return not(isStatic()).and(matcher);
                // 静态方法
            } else if (StaticMethodInterceptor.class.isAssignableFrom(interceptor)) {
                return isStatic().and(matcher);
                // 构造方法
            } else {
                return isConstructor().and(matcher);
            }
        }

        private static Class parseAdvice(Class interceptor) {
            // 普通方法分发器
            if (InstanceMethodInterceptor.class.isAssignableFrom(interceptor)) {
                return InstanceMethodAdvice.class;
                // 静态方法分发器
            } else if (StaticMethodInterceptor.class.isAssignableFrom(interceptor)) {
                return StaticMethodAdvice.class;
                // 构造方法分发器
            } else {
                return ConstructorAdvice.class;
            }
        }

        private static Object parseDelegation(Class interceptor, ClassLoader classLoader) {
            // 普通方法分发器
            if (InstanceMethodInterceptor.class.isAssignableFrom(interceptor)) {
                return InstanceMethodDelegation.of(interceptor, classLoader);
                // 静态方法分发器
            } else if (StaticMethodInterceptor.class.isAssignableFrom(interceptor)) {
                return StaticMethodDelegation.of(interceptor, classLoader);
                // 构造方法分发器
            } else {
                return ConstructorDelegation.of(interceptor, classLoader);
            }
        }

    }

}
