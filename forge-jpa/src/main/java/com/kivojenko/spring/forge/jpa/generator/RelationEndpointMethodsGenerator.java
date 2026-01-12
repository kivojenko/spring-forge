package com.kivojenko.spring.forge.jpa.generator;

import com.kivojenko.spring.forge.jpa.model.EndpointRelation;
import com.kivojenko.spring.forge.jpa.model.JpaEntityModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;

import javax.lang.model.element.Modifier;


public final class RelationEndpointMethodsGenerator {
    private static final ClassName GET_MAPPING = ClassName.get("org.springframework.web.bind.annotation", "GetMapping");

    public static MethodSpec read(JpaEntityModel model, EndpointRelation r) {
        return MethodSpec.methodBuilder(r.getMethodName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(GET_MAPPING)
                        .addMember("value", "$S", "/{id}/" + r.path())
                        .build())
                .returns(ParameterizedTypeName.get(ClassName.get(Iterable.class), r.elementType()))
                .addParameter(model.jpaId().type(), "id")
                .addStatement("return getById(id).$L()", r.getMethodName())
                .build();
    }
}
