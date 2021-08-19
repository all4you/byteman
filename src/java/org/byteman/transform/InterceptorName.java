package org.byteman.transform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在Advice中接收到的interceptorName
 *
 * @author houyi.wh
 * @since 2021-03-23
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface InterceptorName {

}
