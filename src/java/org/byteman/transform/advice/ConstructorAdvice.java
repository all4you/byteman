package org.byteman.transform.advice;

import org.byteman.logger.InternalLogger;
import org.byteman.logger.InternalLoggerFactory;
import net.bytebuddy.asm.Advice;
import org.byteman.transform.ConstructorInterceptor;
import org.byteman.transform.InterceptorFactory;
import org.byteman.transform.InterceptorName;

import java.lang.reflect.Constructor;

/**
 * 构造方法的的分发器
 * 构造方法中不支持捕获throwable
 * 所以要在@Advice.OnMethodExit中去除掉 @Advice.Thrown
 * 并且不能在@Advice.OnMethodEnter中获取 @Advice.This
 * 因为在构造方法中无法引用 this 对象
 * 参考：https://github.com/raphw/byte-buddy/issues/324
 * <p>
 * 将每个构造方法需要增强的逻辑
 * 分发给具体的 {@link ConstructorInterceptor} 去执行
 * 在Advice类中的代码不要使用那些高于JDK6才有的特性，因为Advice需要将这些代码织入到被代理的方法中，
 * 如果被织入的代码高于JDK6，比如lambda表达式，这时织入逻辑会通过invoke dynamic方式来织入，所以织入时会报错：
 * java.lang.IllegalStateException: Cannot write invoke dynamic instruction for class file version Java 6
 *
 * @author houyi.wh
 * @since 2021-03-21
 */
public class ConstructorAdvice {

    // 由于这两个变量需要从“被增强的类”中进行访问，所以需要设置为public，
    // 否则会报错：java.lang.IllegalAccessError
    public static final InternalLogger logger = InternalLoggerFactory.getLogger(ConstructorAdvice.class);

    /**
     * 在构造方法执行之前触发该方法调用
     *
     * @param interceptorName 被委托给的拦截器
     * @param constructor     被代理的构造方法
     * @param args            被代理的方法参数
     */
    @Advice.OnMethodEnter
    public static void onConstructor(@InterceptorName String interceptorName, @Advice.Origin Constructor<?> constructor, @Advice.AllArguments Object[] args) {
        try {
            ConstructorInterceptor interceptor = InterceptorFactory.newInterceptor(interceptorName);
            interceptor.onConstructor(constructor, args);
        } catch (Throwable e) {
            logger.warn("ConstructorAdvice occurred error onConstructor, cause=" + e.getMessage(), e);
        }
    }



}
