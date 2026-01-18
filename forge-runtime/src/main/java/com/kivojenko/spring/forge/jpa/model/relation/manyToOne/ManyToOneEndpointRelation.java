package com.kivojenko.spring.forge.jpa.model.relation.manyToOne;

import com.kivojenko.spring.forge.jpa.model.relation.ServiceRepositoryEndpointRelation;
import com.squareup.javapoet.MethodSpec;
import jakarta.persistence.EntityNotFoundException;
import lombok.experimental.SuperBuilder;

import static com.kivojenko.spring.forge.jpa.generator.MethodGenerator.getterName;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.decapitalize;


/**
 * Base class for Many-to-One endpoint relations.
 */
@SuperBuilder
public abstract class ManyToOneEndpointRelation extends ServiceRepositoryEndpointRelation {
    protected void addFindSub(MethodSpec.Builder methodSpec) {
        methodSpec.addParameter(subParamSpec()).addStatement(
                "var $L = $L.findById($L).orElseThrow($T::new)",
                SUB_VAR_NAME,
                decapitalize(targetEntityModel.repositoryName()),
                SUB_ID_PARAM_NAME,
                EntityNotFoundException.class
        ).beginControlFlow(
                "if (!$N.$L().$L().equals($L))",
                BASE_VAR_NAME,
                getterName(fieldName),
                getterName("id"),
                SUB_ID_PARAM_NAME
        ).addStatement(
                "throw new $T($S)",
                IllegalStateException.class,
                entityModel.entityType().simpleName() +
                        " is not associated with the given " +
                        targetEntityModel.entityType().simpleName()
        ).endControlFlow();
    }
}
