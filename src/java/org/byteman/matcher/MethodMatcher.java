package org.byteman.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * 方法匹配器
 *
 * @author houyi.wh
 * @since 2021-07-01
 * @since 0.0.1
 */
public interface MethodMatcher {

    /**
     * 获取方法匹配器
     */
    ElementMatcher<MethodDescription> getDescription();

    class Matchers  {

        public static MethodMatcher byAnnotation(final String annotation) {
            return new MethodMatcher() {
                @Override
                public ElementMatcher<MethodDescription> getDescription() {
                    if (annotation == null || "".equals(annotation)) {
                        return null;
                    }
                    return isAnnotatedWith(named(annotation));
                }
            };
        }

        public static MethodMatcher byName(final String name) {
            return new MethodMatcher() {
                @Override
                public ElementMatcher<MethodDescription> getDescription() {
                    if (name == null || "".equals(name)) {
                        return null;
                    }
                    return named(name);
                }
            };
        }

        public static MethodMatcher byCustomizeMatcher(final ElementMatcher<MethodDescription> matcher) {
            return new MethodMatcher() {
                @Override
                public ElementMatcher<MethodDescription> getDescription() {
                    return matcher;
                }
            };
        }

        public static MethodMatcher byConstructor(final ElementMatcher<MethodDescription> matcher) {
            return new MethodMatcher() {
                @Override
                public ElementMatcher<MethodDescription> getDescription() {
                    ElementMatcher.Junction<MethodDescription> description = ElementMatchers.isConstructor();
                    if (matcher != null) {
                        description = description.and(matcher);
                    }
                    return description;
                }
            };
        }
    }

}
