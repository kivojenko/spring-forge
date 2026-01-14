package com.kivojenko.spring.forge.jpa.model.relation;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.generator.MethodGenerator.GET_MAPPING;

/**
 * Represents a relation that generates a GET endpoint to read associated entities.
 */
@SuperBuilder
public class ReadEndpointRelation extends EndpointRelation {
    @Override
    public MethodSpec getControllerMethod() {
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(GET_MAPPING).addMember("value", "$S", "/{id}/" + path).build())
                .returns(ParameterizedTypeName.get(ClassName.get(Iterable.class), targetEntityModel.entityType()))
                .addParameter(entityModel.jpaId().type(), "id")
                .addStatement("return getById(id).$L()", methodName)
                .build();
    }

}

