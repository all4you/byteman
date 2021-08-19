package org.byteman.transform;

import java.lang.reflect.Constructor;

/**
 * 构造方法拦截器：
 * onConstructor方法会在原构造方法调用之前触发
 *
 * @author houyi.wh
 * @since 2021-03-10
 */
public interface ConstructorInterceptor extends Interceptor {

    /**
     * 在构造方法执行之前触发该方法调用
     *
     * @param constructor 构造方法
     * @param args        参数
     */
    void onConstructor(Constructor<?> constructor, Object[] args);


}
