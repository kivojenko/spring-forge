package com.kivojenko.spring.forge.jpa.model.relation.oneToMany;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
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
public class ReadOneToManyEndpointRelation extends EndpointRelation {
    @Override
    public MethodSpec getControllerMethod() {
        return MethodSpec
                .methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(annotation(GET_MAPPING))
                .returns(ParameterizedTypeName.get(ClassName.get(Iterable.class), targetEntityModel.entityType()))
                .addParameter(baseParamSpec())
                .addStatement("return getById($L).$L()", BASE_ID_PARAM_NAME, methodName)
                .build();
    }

}

