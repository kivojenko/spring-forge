package com.kivojenko.spring.forge.jpa.model.relation;

import com.kivojenko.spring.forge.jpa.model.model.JpaEntityModel;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Abstract base class for defining extra endpoints related to entity associations or methods.
 */
@Data
@SuperBuilder
public abstract class EndpointRelation {
    /**
     * The relative path for the endpoint.
     */
    protected String path;

    /**
     * The name of the method that provides the data for this relation.
     */
    protected String methodName;

    /**
     * The name of the field this relation is based on, if applicable.
     */
    protected String fieldName;

    /**
     * The model of the entity that owns this relation.
     */
    protected JpaEntityModel entityModel;

    /**
     * The model of the target entity of this relation.
     */
    protected JpaEntityModel targetEntityModel;

    /**
     * Returns the field specification to be added to the generated controller, if any.
     * @return the field specification or null
     */
    public FieldSpec getControllerField() {
        return null;
    }

    /**
     * Returns the field specification to be added to the generated service, if any.
     * @return the field specification or null
     */
    public FieldSpec getServiceField() {
        return null;
    }

    /**
     * Returns the method specification for the generated controller.
     * @return the method specification or null
     */
    public MethodSpec getControllerMethod() {
        return null;
    }

    /**
     * Returns the method specification for the generated service.
     * @return the method specification or null
     */
    public MethodSpec getServiceMethod() {
        return null;
    }
}