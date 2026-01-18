package com.kivojenko.spring.forge.jpa.generator;

import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.jspecify.annotations.NonNull;

import javax.lang.model.element.Modifier;

/**
 * Utility for generating JavaPoet method specifications and common naming conventions.
 */
public class MethodGenerator {
    public static final ClassName GET_MAPPING =
            ClassName.get("org.springframework.web.bind" + ".annotation", "GetMapping");
    public static final ClassName POST_MAPPING =
            ClassName.get("org.springframework.web.bind" + ".annotation", "PostMapping");
    public static final ClassName PUT_MAPPING =
            ClassName.get("org.springframework.web.bind" + ".annotation", "PutMapping");
    public static final ClassName DELETE_MAPPING =
            ClassName.get("org.springframework.web.bind" + ".annotation", "DeleteMapping");

    /**
     * Generates a protected {@code setId} method that overrides the base controller/service method.
     *
     * @param model the entity model
     * @return the method specification
     */
    public static MethodSpec getSetIdMethod(JpaEntityModel model) {
        return MethodSpec.methodBuilder("setId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.VOID)
                .addParameter(model.entityType(), "entity")
                .addParameter(model.jpaId().type(), "id")
                .addStatement("entity.$L(id)", setterName(model.jpaId().name()))
                .build();
    }

    /**
     * Gets the standard getter name for the entity's ID.
     *
     * @param model the entity model
     * @return the getter name (e.g., "getId")
     */
    public static String getterName(JpaEntityModel model) {
        return getterName(model.jpaId().name());
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
     * Gets the standard setter name for the entity's ID.
     *
     * @param model the entity model
     * @return the setter name (e.g., "setId")
     */
    public static String setterName(JpaEntityModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Model must not be null");
        }

        return setterName(model.jpaId().name());
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

    /**
     * Capitalizes the first character of a string.
     *
     * @param fieldName the string to capitalize
     * @return the capitalized string
     */
    public static @NonNull String capitalize(String fieldName) {
        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }
}
