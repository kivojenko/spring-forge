package com.kivojenko.spring.forge.jpa.controller;

import com.kivojenko.spring.forge.jpa.model.JpaEntityModel;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;

public final class JpaControllerGenerator {

    private static final ClassName REST_CONTROLLER =
            ClassName.get("org.springframework.web.bind.annotation", "RestController");
    private static final ClassName REQUEST_MAPPING =
            ClassName.get("org.springframework.web.bind.annotation", "RequestMapping");

    private static final ClassName ABSTRACT_CONTROLLER = ClassName.get(AbstractController.class);
    private static final ClassName HAS_NAME_CONTROLLER = ClassName.get(HasNameController.class);

    public static JavaFile generateFile(JpaEntityModel model) {
        return JavaFile.builder(model.controllerPackageName(), generate(model)).build();
    }

    public static TypeSpec generate(JpaEntityModel model) {
        var mappingAnnotation =
                AnnotationSpec.builder(REQUEST_MAPPING).addMember("value", "$S", model.controllerPath()).build();

        var superClassName = model.hasName() ? HAS_NAME_CONTROLLER : ABSTRACT_CONTROLLER;
        var superClass = ParameterizedTypeName.get(superClassName, model.entityType(), model.jpaId()
                .type(), model.repositoryType());

        return TypeSpec.classBuilder(model.controllerName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(REST_CONTROLLER)
                .addAnnotation(mappingAnnotation)
                .superclass(superClass)
                .addMethod(setIdMethod(model))
                .build();
    }

    private static MethodSpec setIdMethod(JpaEntityModel model) {
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
