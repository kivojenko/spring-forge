package com.kivojenko.spring.forge.annotation.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated field should participate in multi-field string searching on the generated filter.
 *
 * <p>When present on an entity, the generated filter exposes a string search parameter (by default named {@code search})
 * that matches against any of the fields annotated with {@code @FilterSearchField} using their configured match mode.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(FilterSearchFields.class)
public @interface FilterSearchField {
  /**
   * Defines how string values are matched against the target field (e.g. contains, starts with, equals, ignore case).
   *
   * @return the string match mode to apply
   */
  StringMatchMode stringMatchMode() default StringMatchMode.CONTAINS;

  /**
   * The name of the field in the entity that this search field targets.
   * If empty, the name of the annotated field is used.
   *
   * @return the target field name
   */
  String targetField() default "";

  /**
   * The name of the search parameter in the filter DTO.
   * If empty, defaults to {@code "search"}.
   *
   * @return the search parameter name
   */
  String name() default "";
}
