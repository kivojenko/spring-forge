package com.kivojenko.spring.forge.annotation.filter;

/**
 * Defines how comparable values (numbers, dates, etc.) are compared when building filters.
 * <p>
 * Modes:
 * <ul>
 *   <li><b>EXACT</b> — match items where the field equals the provided value.</li>
 *   <li><b>RANGE</b> — match items where the field falls within a provided range.</li>
 *   <li><b>EXACT_OR_RANGE</b> — allow either exact value or range-based matching.</li>
 * </ul>
 */
public enum ComparisonMatchMode {
  /** Match by exact value only. */
  EXACT,
  /** Match by a range only. */
  RANGE,
  /** Allow either exact or range matching. */
  EXACT_OR_RANGE
}
