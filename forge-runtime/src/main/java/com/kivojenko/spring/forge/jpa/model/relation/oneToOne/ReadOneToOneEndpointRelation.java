package com.kivojenko.spring.forge.jpa.model.relation.oneToOne;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.GET_MAPPING;

/**
 * Represents a relation that generates a GET endpoint to read a One-to-One associated entity.
 */
@SuperBuilder
public class ReadOneToOneEndpointRelation extends EndpointRelation {
    @Override
    public MethodSpec getControllerMethod() {
        return MethodSpec
                .methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(annotation(GET_MAPPING))
                .returns(targetEntityModel.getEntityType())
                .addParameter(baseParamSpec())
                .addStatement("return getById($L).$L()", BASE_ID_PARAM_NAME, methodName)
                .build();
    }

}

