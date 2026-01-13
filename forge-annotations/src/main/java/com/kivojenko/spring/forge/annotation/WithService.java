package com.kivojenko.spring.forge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Triggers the generation of a service class and JPA repository for the annotated entity.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface WithService {
    /**
     * The package name where the generated service should be placed.
     * If empty, it will be placed in a default service package.
     */
    String packageName() default "";
}