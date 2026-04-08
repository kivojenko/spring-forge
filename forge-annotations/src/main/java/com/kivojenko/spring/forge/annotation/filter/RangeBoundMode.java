package com.kivojenko.spring.forge.annotation.filter;

/**
 * Indicates whether a range boundary (minimum or maximum) is inclusive or exclusive.
 */
public enum RangeBoundMode {
  /** Boundary value is included in the range (>= or <=). */
  INCLUDES,
  /** Boundary value is excluded from the range (> or <). */
  EXCLUDES
}