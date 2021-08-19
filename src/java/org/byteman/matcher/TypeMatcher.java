package org.byteman.matcher;

import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;

/**
 * 类型匹配器
 *
 * @author houyi.wh
 * @since 2021-07-24
 * @since 0.0.1
 */
public interface TypeMatcher {

    /**
     * 获取类型描述
     */
    TypeDescription getDescription();

    class Matchers {

        private static final TypePool typePool = TypePool.Default.ofSystemLoader();

        public static TypeMatcher byClassName(String className) {
            return new TypeMatcher() {
                @Override
                public TypeDescription getDescription() {
                    if (className == null || "".equals(className)) {
                        return TypeDescription.UNDEFINED;
                    }
                    return typePool.describe(className).resolve();
                }
            };
        }

        public static TypeMatcher byClass(Class clazz) {
            return new TypeMatcher() {
                @Override
                public TypeDescription getDescription() {
                    if (clazz == null) {
                        return TypeDescription.UNDEFINED;
                    }
                    return TypeDescription.ForLoadedType.of(clazz);
                }
            };
        }

        public static TypeMatcher byPackageName(String pkgName) {
            return new TypeMatcher() {
                @Override
                public TypeDescription getDescription() {
                    if (pkgName == null || "".equals(pkgName)) {
                        return TypeDescription.UNDEFINED;
                    }
                    return new TypeDescription.ForPackageDescription(new PackageDescription.Simple(pkgName));
                }
            };
        }

        public static TypeMatcher byPackage(Package pkg) {
            return new TypeMatcher() {
                @Override
                public TypeDescription getDescription() {
                    if (pkg == null) {
                        return TypeDescription.UNDEFINED;
                    }
                    return new TypeDescription.ForPackageDescription(new PackageDescription.ForLoadedPackage(pkg));
                }
            };
        }

    }

}
