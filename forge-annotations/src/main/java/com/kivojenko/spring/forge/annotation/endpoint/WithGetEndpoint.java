package com.kivojenko.spring.forge.annotation.endpoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method should be exposed as a GET endpoint.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface WithGetEndpoint {
  /**
   * The custom path for the GET endpoint.
   * If empty, a default path based on the method name will be used.
   *
   * @return the custom path
   */
  String path() default "";

  /**
   * The Jackson JSON view to apply to the generated GET endpoint.
   * If {@link Void}, no view is applied.
   *
   * @return the JSON view class
   */
  Class<?> view() default Void.class;

  /**
   * The default sort properties to apply to the generated GET endpoint (e.g. {@code {"name"}}, {@code {"name,desc"}}).
   *
   * @return the default sort properties
   */
  String[] sort() default {};
}
