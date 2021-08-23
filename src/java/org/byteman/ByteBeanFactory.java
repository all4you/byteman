package org.byteman;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.byteman.logger.InternalLogger;
import org.byteman.logger.InternalLoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

/**
 * Bean相关的操作
 *
 * @author houyi.wh
 * @since 2021-07-24
 * @since 0.0.1
 */
public interface ByteBeanFactory {

    /**
     * 对源对象进行trim操作
     *
     * @param bean 源对象
     * @param <T>  源类型
     * @return trim后的对象
     */
    <T> T trimBean(T bean);

    <S, T> T copyBean(S source, Class<T> target);

    enum Factories implements ByteBeanFactory {

        /**
         * 对创建的代理Class进行缓存
         */
        WithCache(true),
        /**
         * 每次创建新的代理Class
         */
        WithoutCache(false),
        ;

        private boolean useCache;

        Factories(boolean useCache) {
            this.useCache = useCache;
        }

        private static final InternalLogger logger = InternalLoggerFactory.getLogger(Factories.class);

        private static final Object trimBeanLock = new Object();

        private static final Object copyBeanLock = new Object();

        private static final TypeCache<Class> typeCache = new TypeCache<>(TypeCache.Sort.SOFT);

        @Override
        @SuppressWarnings("unchecked")
        public <T> T trimBean(T bean) {
            try {
                if (bean == null) {
                    return null;
                }
                ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                Class<?> originClass = bean.getClass();
                Class<?> interceptedClass;
                // 代理对象
                Object proxy;
                if (useCache) {
                    // 创建Class并缓存起来
                    interceptedClass = typeCache.findOrInsert(classLoader, originClass, new Callable<Class<?>>() {
                        @Override
                        public Class<?> call() throws Exception {
                            return Trimmed.buildTrimmedClass(classLoader, originClass);
                        }
                    }, trimBeanLock);
                } else {
                    // 每次都新创建代理对象
                    interceptedClass = Trimmed.buildTrimmedClass(classLoader, originClass);
                }
                if (interceptedClass == null) {
                    return null;
                }
                // 代理类的构造方法为：InterceptedClass(T bean)
                proxy = interceptedClass.getConstructor(originClass).newInstance(bean);
                return (T) proxy;
            } catch (Exception e) {
                logger.error("build buildTrimmedBean proxy failed cause={}", e.getMessage(), e);
                return null;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S, T> T copyBean(S source, Class<T> targetClass) {
            try {
                if (source == null || targetClass == null) {
                    return null;
                }
                ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                Class<?> interceptedClass;
                // 代理对象
                Object proxy;
                if (useCache) {
                    // 创建Class并缓存起来
                    interceptedClass = typeCache.findOrInsert(classLoader, targetClass, new Callable<Class<?>>() {
                        @Override
                        public Class<?> call() throws Exception {
                            return Copied.buildCopiedClass(classLoader, source, targetClass);
                        }
                    }, copyBeanLock);
                } else {
                    // 每次都新创建代理对象
                    interceptedClass = Copied.buildCopiedClass(classLoader, source, targetClass);
                }
                if (interceptedClass == null) {
                    return null;
                }
                // 代理类的构造方法为：InterceptedClass(T bean)
                proxy = interceptedClass.getConstructor(targetClass).newInstance(source);
                return (T) proxy;
            } catch (Exception e) {

                return null;
            }
        }


    }


    /**
     * 对指定的class的get方法的返回值做trim操作
     */
    class Trimmed {

        private static final InternalLogger logger = InternalLoggerFactory.getLogger(Trimmed.class);

        public static Class<?> buildTrimmedClass(ClassLoader classLoader, Class<?> originClass) {
            try {
                return new ByteBuddy()
                        // 对源类型进行增强，创建一个子类
                        .subclass(originClass)
                        // 在子类中定义一个字段，名称为：__origin__ ，类型为：originClass 该字段指向被代理的bean实例
                        .defineField("__origin__", originClass, Visibility.PRIVATE)
                        // 在子类中定义一个public型的构造器
                        .defineConstructor(Visibility.PUBLIC)
                        // 为刚刚定义的构造器，定义参数，类型是originClass
                        .withParameters(originClass)
                        // 当调用被代理类的构造器时，将被代理类的对象赋值给子类的 __origin__ 字段
                        .intercept(MethodCall.invoke(originClass.getConstructor())
                                .andThen(FieldAccessor.ofField("__origin__").setsArgumentAt(0)))
                        // 拦截被代理类的getter方法，交给 TrimmingInterceptor 处理
                        .method(nameStartsWith("get"))
                        .intercept(MethodDelegation.to(TrimmingInterceptor.class))
                        .make()
                        .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                        .getLoaded();
            } catch (NoSuchMethodException e) {
                logger.error("buildTrimmedClass occurred error, cause={}", e.getMessage(), e);
                return null;
            }
        }

        private static class TrimmingInterceptor {
            @RuntimeType
            public static Object intercept(@Origin Method method, @AllArguments Object[] args, @FieldValue("__origin__") Object delegate) throws Exception {
                // 先执行被代理对象的getter方法
                // delegate是被代理对象
                Object result = method.invoke(delegate, args);
                // 如果getter方法返回的是String类型，则trim
                if (args.length == 0 && result instanceof String) {
                    result = ((String) result).trim();
                }
                return result;
            }
        }
    }

    /**
     * 对指定的bean进行copy
     * 将值copy到另一个bean中
     */
    class Copied {

        private static final InternalLogger logger = InternalLoggerFactory.getLogger(Copied.class);

        public static <S> Class<?> buildCopiedClass(ClassLoader classLoader, S source, Class<?> targetClass) {
            try {
                DynamicType.Builder<?> builder = new ByteBuddy()
                        .subclass(targetClass)
                        // 在代理类中定义一个字段，名称为：__origin__ ，类型为：clazz 该字段指向被代理的bean实例
                        .defineField("__origin__", targetClass, Visibility.PRIVATE)
                        // 在代理类中定义一个构造器，接收的参数是被代理bean的类型
                        .defineConstructor(Visibility.PUBLIC)
                        // 为刚刚定义的构造器，定义一个clazz类型的参数
                        .withParameters(targetClass)
                        // 当调用代理类的构造器时，将被代理类的对象赋值给 __origin__ 字段
                        .intercept(MethodCall.invoke(targetClass.getConstructor())
                                .andThen(FieldAccessor.ofField("__origin__").setsArgumentAt(0)));
                Class sourceClass = source.getClass();
                // 遍历source的方法
//                for (Method method : sourceClass.getDeclaredMethods()) {
//                    String methodName = method.getName();
//                    if (methodName.startsWith("get")) {
//                        Method setter = sourceClass.getMethod();
//                        builder = builder
//                                .define(method)
//                                .intercept(MethodCall.invoke(method).on(source).with())
//                    }
//                }

                return null;
            } catch (NoSuchMethodException e) {
                logger.error("buildTrimmedClass occurred error, cause={}", e.getMessage(), e);
                return null;
            }
        }
    }


}
