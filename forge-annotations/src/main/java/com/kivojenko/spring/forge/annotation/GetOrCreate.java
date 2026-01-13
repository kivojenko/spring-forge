package com.kivojenko.spring.forge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated entity should have a "get or create" operation generated.
 * Only works with entities that implement the HasName interface.
 * Generates a method in the service class if entity annotated with WithService and/or controller if annotated with WithRestController.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GetOrCreate {
    /**
     * The custom path for the "get or create" endpoint.
     * If empty, a default path based on the entity name will be used.
     */
    String path() default "";
}