package org.byteman.transform.advice;

import org.byteman.logger.InternalLogger;
import org.byteman.logger.InternalLoggerFactory;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.byteman.transform.InstanceMethodInterceptor;
import org.byteman.transform.InterceptorFactory;
import org.byteman.transform.InterceptorName;

import java.lang.reflect.Method;

/**
 * 普通方法的分发器
 * 将每个方法需要增强的逻辑
 * 分发给具体的 {@link InstanceMethodInterceptor} 去执行
 * 在Advice类中的代码不要使用那些高于JDK6才有的特性，因为Advice需要将这些代码织入到被代理的方法中，
 * 如果被织入的代码高于JDK6，比如lambda表达式，这时织入逻辑会通过invoke dynamic方式来织入，所以织入时会报错：
 * java.lang.IllegalStateException: Cannot write invoke dynamic instruction for class file version Java 6
 *
 * @author houyi.wh
 * @since 2021-03-21
 */
public class InstanceMethodAdvice {

    // 由于这两个变量需要从“被增强的类”中进行访问，所以需要设置为public，
    // 否则会报错：java.lang.IllegalAccessError
    public static final InternalLogger logger = InternalLoggerFactory.getLogger(InstanceMethodAdvice.class);

    /**
     * 在实例target的方法执行之前触发该方法调用
     *
     * @param interceptorName 被委托给的拦截器
     * @param target          被代理的实例对象
     * @param method          被代理的方法
     * @param args            被代理的方法参数
     * @param localValue      临时变量，可以传递给 afterMethod
     * @return beforeMethod方法生成的临时对象
     */
    @Advice.OnMethodEnter
    public static <T> T beforeMethod(@InterceptorName String interceptorName, @Advice.This Object target, @Advice.Origin Method method, @Advice.AllArguments Object[] args, @Advice.Local("localValue") Object localValue) {
        T packet = null;
        logger.info("target={}", target);
        try {
            InstanceMethodInterceptor<T> interceptor = InterceptorFactory.newInterceptor(interceptorName);
            packet = interceptor.beforeMethod(target, method, args);
        } catch (Throwable e) {
            logger.warn("InstanceMethodAdvice occurred error beforeMethod, cause=" + e.getMessage(), e);
        }
        return packet;
    }

    /**
     * 在实例target的方法执行之后触发该方法调用
     * 如果被代理的方法异常退出，则不会调用该方法
     * 除非在@Advice.OnMethodExit注解中指定了onThrowable参数
     *
     * @param interceptorName 被委托给的拦截器
     * @param packet          OnMethodEnter标注的方法返回的值
     * @param localValue      临时变量，可以传递给 afterMethod
     * @param originResult    被代理方法的执行结果 @Advice.Return注解必须要指定(typing = Assigner.Typing.DYNAMIC) 表示动态赋值，否则对于返回值是void的方法，无法用Object类型来接收
     * @param throwable       被代理的方法执行时抛出的异常
     */
    @Advice.OnMethodExit(onThrowable = RuntimeException.class)
    public static <T> void afterMethod(@InterceptorName String interceptorName, @Advice.Enter T packet, @Advice.Local("localValue") Object localValue, @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object originResult, @Advice.Thrown Throwable throwable) {
        if (throwable != null) {
            logger.error(throwable);
        }
        // 如果beforeMethod方法返回的packet不为空
        // 则需要调用afterMethod方法，反之不需要
        if (packet != null) {
            try {
                InstanceMethodInterceptor<T> interceptor = InterceptorFactory.newInterceptor(interceptorName);
                interceptor.afterMethod(packet, originResult);
            } catch (Throwable e) {
                logger.warn("InstanceMethodAdvice occurred error afterMethod, cause=" + e.getMessage(), e);
            }
        }
    }


}
