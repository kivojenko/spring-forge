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
   *
   * @return the custom path
   */
  String path() default "";

  /**
   * Whether to generate a GET endpoint to read items from this association.
   *
   * @return true if a GET endpoint should be generated, false otherwise
   */
  boolean read() default true;

  /**
   * Whether to generate a POST endpoint that creates a brand-new entity and adds it to this association.
   *
   * @return true if a POST endpoint for adding a completely new entity to this association should be generated,
   * false otherwise
   */
  boolean addNew() default true;

  /**
   * Whether to generate a POST endpoint that links an existing entity to this association (without creating it).
   *
   * @return true if a POST endpoint for linking an existing entity to this association should be generated, false
   * otherwise
   */
  boolean linkExisting() default true;

  /**
   * Whether to generate a DELETE endpoint to remove an item from this association.
   *
   * @return true if a DELETE endpoint should be generated, false otherwise
   */
  boolean remove() default true;
}
