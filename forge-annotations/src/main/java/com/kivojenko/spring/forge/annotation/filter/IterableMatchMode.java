package com.kivojenko.spring.forge.annotation.filter;

/**
 * Defines how a collection/iterable field should satisfy provided filter values.
 */
public enum IterableMatchMode {
  /** At least one element must match. */
  ANY,
  /** All specified elements must be present/match. */
  ALL
}
