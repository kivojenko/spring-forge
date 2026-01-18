package com.kivojenko.spring.forge.jpa.model.relation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import jakarta.persistence.EntityNotFoundException;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

/**
 * Base class for endpoint relations that involve both Service and Repository layers.
 * Provides default implementations for generating controller and service methods.
 */
@SuperBuilder
public abstract class ServiceRepositoryEndpointRelation extends EndpointRelation {
    protected static final ClassName TRANSACTIONAL = ClassName.get(
            "org.springframework.transaction.annotation",
            "Transactional"
    );

    @Override
    public FieldSpec getControllerField() {
        return entityModel.requirements().wantsService() ? null : getTargetRepositoryFieldSpec();
    }

    @Override
    public FieldSpec getServiceField() {
        return entityModel.requirements().wantsService() ? getTargetRepositoryFieldSpec() : null;
    }

    @Override
    public MethodSpec getControllerMethod() {
        var method = entityModel.requirements().wantsService() ? getServiceMethodSpec() : getRepositoryMethodSpec();
        return method.addAnnotation(annotation(mapping())).addAnnotation(TRANSACTIONAL).build();
    }

    @Override
    public MethodSpec getServiceMethod() {
        return entityModel.requirements().wantsService() ? getRepositoryMethodSpec().build() : null;
    }

    protected abstract ClassName mapping();

    protected abstract String generatedMethodName();

    protected MethodSpec.Builder getServiceMethodSpec() {
        return MethodSpec
                .methodBuilder((generatedMethodName()))
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Long.class, BASE_ID_PARAM_NAME)
                .addParameter(Long.class, SUB_ID_PARAM_NAME)
                .addException(ClassName.get(EntityNotFoundException.class))
                .addStatement("service.$L($L, $L)", generatedMethodName(), BASE_ID_PARAM_NAME, SUB_ID_PARAM_NAME);
    }

    protected abstract MethodSpec.Builder getRepositoryMethodSpec();
}
