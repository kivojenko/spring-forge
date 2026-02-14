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
   * @return true if a GET endpoint should be generated, false otherwise
   */
  boolean read() default true;

  /**
   * @return true if a POST endpoint for adding completely new entity this association should be generated, false
   * otherwise
   */
  boolean addNew() default true;

  /**
   * @return true if a POST endpoint for linking an existing entity to this association should be generated, false
   * otherwise
   */
  boolean linkExisting() default true;

  /**
   * @return true if a DELETE endpoint should be generated, false otherwise
   */
  boolean remove() default true;
}
