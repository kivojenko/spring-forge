package com.kivojenko.spring.forge.jpa.contract;

/**
 * Runtime support for filters declared with {@code IterableMatchMode.CHOOSE}: resolves the match mode a
 * request sent in its {@code <name>MatchMode} parameter. Called from generated filter classes.
 */
public final class IterableMatchModes {
  private IterableMatchModes() {}

  /**
   * Whether the supplied values must all be matched rather than any of them.
   *
   * @param requested  the {@code <name>MatchMode} request parameter — {@code ALL}/{@code AND} or
   *                   {@code ANY}/{@code OR}, case-insensitive; may be {@code null}
   * @param defaultAll the result when {@code requested} is missing or not recognised
   * @return {@code true} to match all values, {@code false} to match any of them
   */
  public static boolean matchAll(String requested, boolean defaultAll) {
    if (requested == null) {
      return defaultAll;
    }
    return switch (requested.trim().toUpperCase(java.util.Locale.ROOT)) {
      case "ALL", "AND" -> true;
      case "ANY", "OR" -> false;
      default -> defaultAll;
    };
  }
}
