package com.kivojenko.spring.forge.annotation.filter;

/**
 * Defines how a {@code String} filter is matched against its target.
 *
 * <p>The plain modes take a single value, so the filter parameter is a {@code String}. The {@code *_ANY}
 * modes take several values instead — the parameter becomes a {@code Set<String>} bound from a repeated
 * query parameter ({@code ?name=alfa&name=beta}) — and a row matches when <em>any</em> of the supplied
 * values matches, using the match of the corresponding single-value mode.
 */
public enum StringMatchMode {
  /** The target must equal the value. */
  EQUALS,
  /** The target must equal the value, ignoring case. */
  EQUALS_IGNORE_CASE,
  /** The target must contain the value. */
  CONTAINS,
  /** The target must contain the value, ignoring case. */
  CONTAINS_IGNORE_CASE,
  /** The target must start with the value. */
  STARTS_WITH,
  /** The target must end with the value. */
  ENDS_WITH,
  /** The target must equal at least one of the values. */
  EQUALS_ANY,
  /** The target must equal at least one of the values, ignoring case. */
  EQUALS_ANY_IGNORE_CASE,
  /** The target must contain at least one of the values. */
  CONTAINS_ANY,
  /** The target must contain at least one of the values, ignoring case. */
  CONTAINS_ANY_IGNORE_CASE,
  /** The target must start with at least one of the values. */
  STARTS_WITH_ANY,
  /** The target must end with at least one of the values. */
  ENDS_WITH_ANY;

  /**
   * The single-value mode this one applies to each supplied value — itself for a mode that already takes
   * one value.
   *
   * @return the match applied per value
   */
  public StringMatchMode singleValue() {
    return switch (this) {
      case EQUALS_ANY -> EQUALS;
      case EQUALS_ANY_IGNORE_CASE -> EQUALS_IGNORE_CASE;
      case CONTAINS_ANY -> CONTAINS;
      case CONTAINS_ANY_IGNORE_CASE -> CONTAINS_IGNORE_CASE;
      case STARTS_WITH_ANY -> STARTS_WITH;
      case ENDS_WITH_ANY -> ENDS_WITH;
      default -> this;
    };
  }

  /**
   * Whether this mode takes several values, so the generated filter parameter is a collection whose
   * elements are OR-combined.
   *
   * @return {@code true} for a {@code *_ANY} mode
   */
  public boolean isMultiValue() {
    return singleValue() != this;
  }
}
