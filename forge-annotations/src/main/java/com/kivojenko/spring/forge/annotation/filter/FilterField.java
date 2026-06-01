package com.kivojenko.spring.forge.annotation.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated field should be used as a filter field.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface FilterField {
  /**
   * Defines how string values are matched when filtering (e.g., contains, starts with).
   *
   * @return the string match mode to apply
   */
  StringMatchMode stringMatchMode() default StringMatchMode.CONTAINS;

  /**
   * Defines how elements of a collection field must match the provided filter values.
   *
   * @return the iterable match mode (e.g., ANY or ALL)
   */
  IterableMatchMode iterableMatchMode() default IterableMatchMode.ANY;

  /**
   * Defines how comparable values (numbers, dates) are matched: exact value, range, or either.
   *
   * @return the comparison match mode
   */
  ComparisonMatchMode comparisonMatchMode() default ComparisonMatchMode.EXACT_OR_RANGE;

  /**
   * Defines whether the lower bound of a range is inclusive or exclusive when range filtering is used.
   *
   * @return the minimum bound mode
   */
  RangeBoundMode minBoundMode() default RangeBoundMode.INCLUDES;

  /**
   * Defines whether the upper bound of a range is inclusive or exclusive when range filtering is used.
   *
   * @return the maximum bound mode
   */
  RangeBoundMode maxBoundMode() default RangeBoundMode.INCLUDES;

  /**
   * The name of the parameter in the filter DTO.
   * If empty, the name of the annotated field is used.
   *
   * @return the filter parameter name
   */
  String name() default "";

  /**
   * The name of the field in the entity that this filter field targets.
   * If empty, the name of the annotated field is used.
   *
   * <p>If the annotated field is an association, the target field is resolved relative to that association
   * (e.g., {@code @FilterField(targetField = "name")} on a {@code Category} field targets {@code category.name}).
   *
   * <p>If the annotated field is not an association, the target field is treated as an absolute path from the root entity
   * (e.g., {@code @FilterField(targetField = "category.name")} on a transient {@code String} field).
   *
   * @return the target field name
   */
  String targetField() default "";

}