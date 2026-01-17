package com.kivojenko.spring.forge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Triggers the generation of a Spring Data JPA repository for the annotated entity.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface WithJpaRepository {
    /**
     * The package name where the generated repository should be placed.
     * If empty, it will be placed in a default repository package.
     *
     * @return the package name
     */
    String packageName() default "";
}
