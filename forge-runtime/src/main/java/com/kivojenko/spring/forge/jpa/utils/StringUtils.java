package com.kivojenko.spring.forge.jpa.utils;

/**
 * Utility for string manipulations.
 */
public class StringUtils {
    /**
     * Capitalizes the first character of a string.
     * @param s the string to capitalize
     * @return the capitalized string
     */
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        int cp = s.codePointAt(0);
        int len = Character.charCount(cp);
        return new StringBuilder(s.length())
                .appendCodePoint(Character.toTitleCase(cp))
                .append(s.substring(len))
                .toString();
    }

    /**
     * Decapitalizes the first character of a string.
     * @param s the string to decapitalize
     * @return the decapitalized string
     */
    public static String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
