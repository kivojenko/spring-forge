package com.kivojenko.spring.forge.jpa.utils;

/**
 * Utility for string manipulations.
 */
public final class StringUtils {
  /**
   * Capitalizes the first character of a string.
   *
   * @param s the string to capitalize
   * @return the capitalized string
   */
  public static String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    int cp = s.codePointAt(0);
    int len = Character.charCount(cp);
    return new StringBuilder(s.length()).appendCodePoint(Character.toTitleCase(cp)).append(s.substring(len)).toString();
  }

  /**
   * Decapitalizes the first character of a string.
   *
   * @param s the string to decapitalize
   * @return the decapitalized string
   */
  public static String decapitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return Character.toLowerCase(s.charAt(0)) + s.substring(1);
  }

  public static String pluralize(String s) {
    if (s == null || s.isEmpty()) return s;

    if (s.endsWith("s")) return decapitalize(s);
    if (s.endsWith("y")) {
      return decapitalize(s).substring(0, s.length() - 1) + "ies";
    }
    return decapitalize(s) + "s";
  }

  /**
   * Gets the standard getter name for a field.
   *
   * @param fieldName the name of the field
   * @return the getter name
   */
  public static String getterName(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      throw new IllegalArgumentException("Field name must not be blank");
    }

    return "get" + capitalize(fieldName);
  }

  /**
   * Gets the standard setter name for a field.
   *
   * @param fieldName the name of the field
   * @return the setter name
   */
  public static String setterName(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      throw new IllegalArgumentException("Field name must not be blank");
    }

    return "set" + capitalize(fieldName);
  }
}
