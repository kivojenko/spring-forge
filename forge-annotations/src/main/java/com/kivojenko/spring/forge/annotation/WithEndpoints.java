package com.kivojenko.spring.forge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface WithEndpoints {
    String path() default "";
    String getMethodName() default "";

    boolean read() default true;

    boolean add() default false;

    boolean remove() default false;
}
