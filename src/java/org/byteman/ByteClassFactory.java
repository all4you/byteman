package org.byteman;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * 类相关的操作
 *
 * @author houyi.wh
 * @since 2021-07-24
 * @since 0.0.1
 */
public interface ByteClassFactory {

    /**
     * 根据map创建对应的Bean Class，并设置getter、setter方法
     * 参考：
     * https://stackoverflow.com/questions/45801132/create-dynamic-runtime-and-simple-bean-from-json-string
     *
     * @param map 源map对象
     * @return 创建的Bean Class
     */
    Class<?> createBeanClassByMap(Map<String, Object> map);

    /**
     * 根据接口创建对应的Bean Class，并设置getter、setter方法
     *
     * @param interfaceClass 接口类
     * @return 创建的Bean Class
     */
    Class<?> createBeanClassByInterface(Class interfaceClass);

    enum Factories implements ByteClassFactory {
        Default;

        @Override
        public Class<?> createBeanClassByMap(Map<String, Object> map) {
            if (map == null) {
                return null;
            }
            DynamicType.Builder<?> builder = new ByteBuddy().subclass(Object.class);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String name = entry.getKey();
                Type type = entry.getValue().getClass();
                // 定义子类的字段
                builder = defineProperty(builder, name, type);
            }
            return loadClass(builder);
        }

        @Override
        public Class<?> createBeanClassByInterface(Class interfaceClass) {
            if (interfaceClass == null) {
                return null;
            }
            DynamicType.Builder<?> builder = new ByteBuddy().subclass(Object.class);
            for (Method method : interfaceClass.getDeclaredMethods()) {
                String name = method.getName();
                Type type = method.getReturnType();
                if (type.equals(void.class)) {
                    continue;
                }
                // 定义子类的字段
                builder = defineProperty(builder, name, type);
            }
            return loadClass(builder);
        }

        private DynamicType.Builder<?> defineProperty(DynamicType.Builder<?> builder, String name, Type type) {
            // 定义getter、setter方法以及对应的field
            return builder.defineProperty(name, type);
        }

        private Class<?> loadClass(DynamicType.Builder<?> builder) {
            return builder.make()
                    .load(ClassLoader.getSystemClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();
        }

    }


}
