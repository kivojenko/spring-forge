package com.kivojenko.spring.forge.jpa.utils;

public class StringUtils {
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        int cp = s.codePointAt(0);
        int len = Character.charCount(cp);
        return new StringBuilder(s.length())
                .appendCodePoint(Character.toTitleCase(cp))
                .append(s.substring(len))
                .toString();
    }

    public static String decapitalize(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

}
