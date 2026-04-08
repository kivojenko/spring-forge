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

}