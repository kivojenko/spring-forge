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

    /**
     * Whether default endpoints by id should allow slashes (e.g. {@code /{*id}}).
     *
     * @return true if default endpoints by id should allow slashes, false otherwise
     */
    boolean allowSlashes() default false;

    /**
     * The Jackson JSON view to apply to the generated REST controller as a default.
     * If {@link Void}, no default view is applied.
     *
     * @return the default JSON view class
     */
    Class<?> view() default Void.class;

    /**
     * The Jackson JSON view to apply to list endpoints (e.g. {@code /entities} / {@code findAll}).
     * If {@link Void}, falls back to {@link #view()}.
     *
     * @return the JSON view class for list endpoints
     */
    Class<?> listView() default Void.class;

    /**
     * The Jackson JSON view to apply to detail endpoints (e.g. {@code /entities/{id}} / {@code getById}).
     * If {@link Void}, falls back to {@link #view()}.
     *
     * @return the JSON view class for detail endpoints
     */
    Class<?> detailView() default Void.class;

    /**
     * The default sort properties to apply to list endpoints (e.g. {@code {"name"}}, {@code {"name,desc"}}).
     *
     * @return the default sort properties
     */
    String[] sort() default {};
}