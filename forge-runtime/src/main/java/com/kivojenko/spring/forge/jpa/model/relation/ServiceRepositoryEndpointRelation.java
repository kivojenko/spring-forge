package com.kivojenko.spring.forge.jpa.model.relation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.TRANSACTIONAL;

/**
 * Base class for endpoint relations that involve both Service and Repository layers.
 * Provides default implementations for generating controller and service methods.
 */
@SuperBuilder
public abstract class ServiceRepositoryEndpointRelation extends EndpointRelation {

    @Override
    public FieldSpec getServiceField() {
        return getTargetRepositoryFieldSpec();
    }

    @Override
    public MethodSpec getControllerMethod() {
        return MethodSpec
                .methodBuilder((generatedMethodName()))
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Long.class, BASE_ID_PARAM_NAME)
                .addParameter(Long.class, SUB_ID_PARAM_NAME)
                .addStatement("service.$L($L, $L)", generatedMethodName(), BASE_ID_PARAM_NAME, SUB_ID_PARAM_NAME)
                .addAnnotation(annotation(mapping()))
                .addAnnotation(TRANSACTIONAL)
                .build();
    }

    protected abstract ClassName mapping();

    protected abstract String generatedMethodName();

}
