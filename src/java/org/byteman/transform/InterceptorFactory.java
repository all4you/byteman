/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.byteman.transform;


import java.util.HashMap;
import java.util.Map;

/**
 * 拦截器工厂
 *
 * @author houyi.wh
 * @since 2021-03-16
 */
public class InterceptorFactory {

    private static final Map<String, Object> interceptors = new HashMap<String, Object>();

    /**
     * 加载interceptor
     */
    public synchronized static <T> T newInterceptor(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return newInterceptor(className, null);
    }

    /**
     * 加载interceptor
     */
    public synchronized static <T> T newInterceptor(Class clazz) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return newInterceptor(clazz, null);
    }

    /**
     * 加载interceptor
     */
    public synchronized static <T> T newInterceptor(String className, ClassLoader classLoader) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        ClassLoader targetClassLoader = classLoader != null ? classLoader : InterceptorFactory.class.getClassLoader();
        // 先检查是否存在该实例
        String instanceKey = getInstanceKey(className, targetClassLoader);
        T instance = getInstance(instanceKey);
        if (instance != null) {
            return instance;
        }
        // 创建instance后再返回该实例
        return newInstance(className, targetClassLoader, instanceKey);
    }

    /**
     * 加载interceptor
     */
    public synchronized static <T> T newInterceptor(Class clazz, ClassLoader classLoader) throws InstantiationException, IllegalAccessException {
        ClassLoader targetClassLoader = classLoader != null ? classLoader : InterceptorFactory.class.getClassLoader();
        // 先检查是否存在该实例
        String instanceKey = getInstanceKey(clazz, targetClassLoader);
        T instance = getInstance(instanceKey);
        if (instance != null) {
            return instance;
        }
        // 创建instance后再返回该实例
        return newInstance(clazz, instanceKey);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getInstance(String instanceKey) {
        return (T) interceptors.get(instanceKey);
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(String className, ClassLoader classLoader, String instanceKey) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class clazz = Class.forName(className, true, classLoader);
        return newInstance(clazz, instanceKey);
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(Class clazz, String instanceKey) throws InstantiationException, IllegalAccessException {
        T instance = (T) clazz.newInstance();
        interceptors.put(instanceKey, instance);
        return (T) instance;
    }


    private static String getInstanceKey(Class clazz, ClassLoader loader) {
        return getInstanceKey(clazz.getName(), loader);
    }

    private static String getInstanceKey(String className, ClassLoader loader) {
        return className + "_of_" + loader.getClass().getName();
    }


}
