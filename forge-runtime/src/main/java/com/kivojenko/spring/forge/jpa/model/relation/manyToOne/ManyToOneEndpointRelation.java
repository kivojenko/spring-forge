package com.kivojenko.spring.forge.jpa.model.relation.manyToOne;

import com.kivojenko.spring.forge.jpa.model.relation.ServiceRepositoryEndpointRelation;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import jakarta.persistence.EntityNotFoundException;
import lombok.experimental.SuperBuilder;

import static com.kivojenko.spring.forge.jpa.utils.StringUtils.decapitalize;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.getterName;


/**
 * Base class for Many-to-One endpoint relations.
 */
@SuperBuilder
public abstract class ManyToOneEndpointRelation extends ServiceRepositoryEndpointRelation {
    /**
     * Adds a statement to find the sub-entity by its ID to the given method builder.
     *
     * @param methodSpec the method builder
     */
    protected void addFindSub(MethodSpec.Builder methodSpec) {
        methodSpec
                .addParameter(ParameterSpec.builder(targetEntityModel.getJpaId().type(), SUB_ID_PARAM_NAME).build())
                .addStatement(
                        "var $L = $L.findById($L).orElseThrow($T::new)",
                        SUB_VAR_NAME,
                        decapitalize(targetEntityModel.getRepositoryName()),
                        SUB_ID_PARAM_NAME,
                        EntityNotFoundException.class
                )
                .beginControlFlow(
                        "if (!$N.$L().$L().equals($L))",
                        BASE_VAR_NAME,
                        getterName(fieldName),
                        getterName("id"),
                        SUB_ID_PARAM_NAME
                )
                .addStatement(
                        "throw new $T($S)",
                        IllegalStateException.class,
                        entityModel.getEntityType().simpleName() +
                                " is not associated with the given " +
                                targetEntityModel.getEntityType().simpleName()
                )
                .endControlFlow();
    }
}
