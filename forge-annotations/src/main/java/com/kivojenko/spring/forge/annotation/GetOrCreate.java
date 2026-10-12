package com.kivojenko.spring.forge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated entity should have a "get or create" operation generated.
 * By default targets the "name" field; can be pointed to other fields via {@link #fields()}.
 * Several fields form a composite key: the entity is looked up by all of them and created with all of them set.
 * Generates a method in the service class if entity annotated with WithService and/or controller if annotated with WithRestController.
 * <p>
 * Repeatable: declare it once per lookup key. With a single annotation the generated methods are named
 * {@code getOrCreate}; with several they are named after their fields ({@code getOrCreateBySku},
 * {@code getOrCreateByBrandAndModel}) and the endpoints share the default path, told apart by the request parameters.
 */
@Repeatable(GetOrCreates.class)
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
     * The entity fields to use for get-or-create lookup and creation; together they form the lookup key.
     * Defaults to {@code name}. Nested relation IDs are supported, e.g. {@code country.code}.
     */
    String[] fields() default {"name"};

    /**
     * Whether to use case-insensitive lookup for the String fields of the key.
     * Ignored for non-String fields. Default: true.
     */
    boolean ignoreCase() default true;
}