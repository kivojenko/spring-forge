package com.kivojenko.spring.forge.annotation.filter;

/**
 * Defines how the filters of one family are combined with each other.
 */
public enum FamilyMatchMode {
  /** Any member may match — the family's filters are combined with {@code OR}. */
  OR,
  /** Every supplied member must match — the family's filters are combined with {@code AND}. */
  AND,
  /**
   * At least one of the family's target fields must hold a value, whether or not any of its parameters
   * is sent: the members' {@code isNotNull()} checks ({@code isNotEmpty()} for collections) are OR-ed and
   * required, and each supplied member's own predicate is {@code AND}-ed on top.
   *
   * <p>Unlike {@link #OR} and {@link #AND}, this constrains the query even when no member is sent.
   */
  EXISTS
}
