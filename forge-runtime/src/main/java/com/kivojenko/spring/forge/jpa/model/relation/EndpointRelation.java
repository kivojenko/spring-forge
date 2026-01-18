package com.kivojenko.spring.forge.jpa.model.relation;

import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.*;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.StringUtils.decapitalize;

/**
 * Abstract base class for defining extra endpoints related to entity associations or methods.
 */
@Data
@SuperBuilder
public abstract class EndpointRelation {
    protected static final ClassName AUTOWIRED = ClassName.get(
            "org.springframework.beans.factory.annotation",
            "Autowired"
    );
    protected static final ClassName PATH_VARIABLE = ClassName.get(
            "org.springframework.web.bind.annotation",
            "PathVariable"
    );


    protected static final String BASE_ID_PARAM_NAME = "baseId";
    protected static final String BASE_VAR_NAME = "base";
    protected static final String SUB_ID_PARAM_NAME = "subId";
    protected static final String SUB_VAR_NAME = "sub";

    protected ParameterSpec baseParamSpec() {
        return ParameterSpec
                .builder(entityModel.jpaId().type(), BASE_ID_PARAM_NAME)
                .addAnnotation(PATH_VARIABLE)
                .build();
    }

    protected ParameterSpec subParamSpec() {
        return ParameterSpec
                .builder(targetEntityModel.jpaId().type(), SUB_ID_PARAM_NAME)
                .addAnnotation(PATH_VARIABLE)
                .build();
    }

    /**
     * The relative path for the endpoint.
     */
    protected String path;

    protected String uri() {
        return "/{" + BASE_ID_PARAM_NAME + "}/" + path;
    }

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
     *
     * @return the field specification or null
     */
    public FieldSpec getControllerField() {
        return null;
    }

    /**
     * Returns the field specification to be added to the generated service, if any.
     *
     * @return the field specification or null
     */
    public FieldSpec getServiceField() {
        return null;
    }

    /**
     * Returns the method specification for the generated controller.
     *
     * @return the method specification or null
     */
    public MethodSpec getControllerMethod() {
        return null;
    }

    /**
     * Returns the method specification for the generated service.
     *
     * @return the method specification or null
     */
    public MethodSpec getServiceMethod() {
        return null;
    }

    protected FieldSpec getTargetRepositoryFieldSpec() {
        return FieldSpec
                .builder(targetEntityModel.repositoryType(), decapitalize(targetEntityModel.repositoryName()))
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(AUTOWIRED).build())
                .build();
    }

    protected AnnotationSpec annotation(ClassName mapping) {
        return AnnotationSpec.builder(mapping).addMember("value", "$S", uri()).build();
    }

    protected MethodSpec.Builder addFindBase(MethodSpec.Builder methodSpec) {
        return methodSpec.addStatement("var $L = getById($L)", BASE_VAR_NAME, BASE_ID_PARAM_NAME);
    }
}