package com.kivojenko.spring.forge.annotation.endpoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures generation of multiple endpoints for a collection association field.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface WithEndpoints {
    /**
     * The custom path for the endpoints.
     * If empty, the field name will be used as the path.
     */
    String path() default "";

    /**
     * The name of the getter method in the service to use for this association.
     * If empty, a default "get" + FieldName will be used.
     */
    String getMethodName() default "";

    /**
     * Whether to generate a GET endpoint for this association.
     */
    boolean read() default true;

    /**
     * Whether to generate a POST endpoint for this association.
     */
    boolean add() default false;

    /**
     * Whether to generate a DELETE endpoint for this association.
     */
    boolean remove() default false;
}
