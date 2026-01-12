package com.kivojenko.spring.forge.jpa.utils;

import com.kivojenko.spring.forge.jpa.model.JpaEntityModel;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;

public class MethodUtils {
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

    static String setterName(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Field name must not be blank");
        }

        return "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }
}
