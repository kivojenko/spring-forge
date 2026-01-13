package com.kivojenko.spring.forge.jpa.model.relation;

import com.kivojenko.spring.forge.jpa.model.model.JpaEntityModel;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class EndpointRelation {
    protected String path;
    protected String methodName;
    protected String fieldName;

    protected JpaEntityModel entityModel;
    protected JpaEntityModel targetEntityModel;

    public FieldSpec getControllerField() {
        return null;
    }

    public FieldSpec getServiceField() {
        return null;
    }

    public MethodSpec getControllerMethod() {
        return null;
    }

    public MethodSpec getServiceMethod() {
        return null;
    }
}