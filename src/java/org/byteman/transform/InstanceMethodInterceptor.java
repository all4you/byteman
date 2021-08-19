package org.byteman.transform;

import java.lang.reflect.Method;

/**
 * 普通方法拦截器
 * <p>
 * beforeMethod方法会在原方法调用之前触发
 * afterMethod方法会在原方法调用之后触发
 * 但是afterMethod也有可能不会被触发，因为beforeMethod提前就需要返回了
 * </p>
 *
 * @author houyi.wh
 * @since 2021-03-10
 */
public interface InstanceMethodInterceptor<T> extends Interceptor {

    /**
     * 在实例target的方法执行之前触发该方法调用
     *
     * @param target 实例对象
     * @param method 方法`
     * @param args   参数
     * @return beforeMethod方法生成的临时对象
     */
    T beforeMethod(Object target, Method method, Object[] args);

    /**
     * 在实例target的方法执行之后触发该方法调用
     *
     * @param packet       从beforeMethod中传递过来的数据
     * @param originResult 原方法的返回值
     */
    void afterMethod(T packet, Object originResult);

}
