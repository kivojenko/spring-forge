package com.kivojenko.spring.forge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated entity should have a "get or create" operation generated.
 * By default targets the "name" field; can be pointed to another field via {@link #field()}.
 * Generates a method in the service class if entity annotated with WithService and/or controller if annotated with WithRestController.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GetOrCreate {
    /**
     * The custom path for the "get or create" endpoint.
     * If empty, a default path based on the entity name will be used.
     *
     * @return the custom path
     */
    String path() default "";

    /**
     * The entity field to use for get-or-create lookup and creation.
     * Defaults to {@code name}. When set, the processor will generate
     * repository/service/controller methods that use this field instead of requiring HasName.
     */
    String field() default "name";

    /**
     * When the target field is a String, whether to use case-insensitive lookup.
     * Ignored for non-String fields. Default: true.
     */
    boolean ignoreCase() default true;
}