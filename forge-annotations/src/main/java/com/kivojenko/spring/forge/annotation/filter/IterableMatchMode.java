package com.kivojenko.spring.forge.annotation.filter;

/**
 * Defines how a collection/iterable field should satisfy provided filter values.
 */
public enum IterableMatchMode {
  /** At least one element must match. */
  ANY,
  /** All specified elements must be present/match. */
  ALL,
  /** Match based on collection size/amount. */
  AMOUNT,
  /**
   * Lets each request choose between {@link #ANY} and {@link #ALL}. The parameter takes a set of values, like
   * {@link #ALL}, and the filter DTO gets an extra {@code String} parameter named {@code <name>MatchMode}:
   * {@code ALL} (or {@code AND}) requires every value to be matched, {@code ANY} (or {@code OR}) at least one
   * of them. The value is case-insensitive; when it is missing or not recognised,
   * {@link FilterField#defaultIterableMatchMode()} applies.
   *
   * <p>Applies to collection associations — by id ({@code ?tags=1,2&tagsMatchMode=ALL}) or through a
   * {@code targetField} of the element ({@code ?spices=1,2&spicesMatchMode=ANY}).
   */
  CHOOSE
}
