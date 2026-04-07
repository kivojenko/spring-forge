package com.kivojenko.spring.forge.annotation.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated date/time field should be used as a range filter field.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface DateRangeFilterField {
  RangeBoundMode minBoundMode() default RangeBoundMode.INCLUDES;
  RangeBoundMode maxBoundMode() default RangeBoundMode.INCLUDES;
}
