package com.kivojenko.spring.forge.annotation;

/**
 * Return type of the generated list endpoint ({@code GET /entities}).
 */
public enum ListType {
    /**
     * Returns a {@code Page} and accepts {@code page}, {@code size} and {@code sort} parameters.
     */
    PAGE,

    /**
     * Returns a plain {@code List} of all matching entities and accepts only the {@code sort} parameter.
     */
    LIST
}
