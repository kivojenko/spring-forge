package com.kivojenko.spring.forge.jpa.generator;

import com.kivojenko.spring.forge.jpa.model.model.JpaEntityModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.jspecify.annotations.NonNull;

import javax.lang.model.element.Modifier;

public class MethodGenerator {
    public static final ClassName GET_MAPPING =
            ClassName.get("org.springframework.web.bind" + ".annotation", "GetMapping");
    public static final ClassName POST_MAPPING =
            ClassName.get("org.springframework.web.bind" + ".annotation", "PostMapping");
    public static final ClassName PUT_MAPPING =
            ClassName.get("org.springframework.web.bind" + ".annotation", "PutMapping");
    public static final ClassName DELETE_MAPPING =
            ClassName.get("org.springframework.web.bind" + ".annotation", "DeleteMapping");

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

    public static String getterName(JpaEntityModel model) {
        return getterName(model.jpaId().name());
    }

    public static String getterName(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Field name must not be blank");
        }

        return "get" + capitalize(fieldName);
    }

    public static String setterName(JpaEntityModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Model must not be null");
        }

        return setterName(model.jpaId().name());
    }

    public static String setterName(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Field name must not be blank");
        }

        return "set" + capitalize(fieldName);
    }

    public static @NonNull String capitalize(String fieldName) {
        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

}
