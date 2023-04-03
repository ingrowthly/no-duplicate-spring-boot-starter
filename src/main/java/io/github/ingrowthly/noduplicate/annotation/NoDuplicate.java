package io.github.ingrowthly.noduplicate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防止重复提交
 *
 * @since 2023/4/3
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoDuplicate {

    /**
     * 重复提交判定间隔，单位秒
     *
     * <p>两次相同参数的请求，如果间隔时间大于该参数，系统不会认定为重复提交的数据
     */
    int ttl() default 2;

    /**
     * 校验 key，Spring EL 表达式
     *
     * <p>如果为空，则默认取请求参数；如果不为空，则取 Spring EL 表达式的值
     */
    String key() default "";

    /**
     * 是否拼接上 URI
     */
    boolean uri() default true;

    /**
     * 重复提交时的提示信息
     */
    String message() default "请勿重复提交";
}
