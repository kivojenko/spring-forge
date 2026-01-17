package com.kivojenko.spring.forge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Triggers the generation of a Spring REST controller and JPA repository for the annotated entity.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface WithRestController {
    /**
     * The base path for the generated controller.
     * If empty, a default path based on the entity name will be used.
     *
     * @return the base path
     */
    String path() default "";

    /**
     * The package name where the generated controller should be placed.
     * If empty, it will be placed in a default controller package.
     *
     * @return the package name
     */
    String packageName() default "";

    /**
     * Whether the generated controller should be abstract.
     * If true, the controller will be abstract, allowing for custom implementation later.
     *
     * @return true if the controller should be abstract, false otherwise
     */
    boolean makeAbstract() default false;
}