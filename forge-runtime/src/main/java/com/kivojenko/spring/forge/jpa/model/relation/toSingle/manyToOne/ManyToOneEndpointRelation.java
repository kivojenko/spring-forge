package com.kivojenko.spring.forge.jpa.model.relation.toSingle.manyToOne;

import com.kivojenko.spring.forge.jpa.model.relation.ServiceRepositoryEndpointRelation;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import static com.kivojenko.spring.forge.jpa.utils.StringUtils.getterName;

/**
 * Base class for Many-to-One endpoint relations.
 */
@SuperBuilder
public abstract class ManyToOneEndpointRelation extends ServiceRepositoryEndpointRelation {

  @Override
  protected void checkFoundSub(MethodSpec.Builder methodSpec) {
    methodSpec.beginControlFlow(
        "if ($N.$L() != null && !$N.$L().$L().equals($L))",
        BASE_VAR_NAME,
        getterName(getFieldName()),
        BASE_VAR_NAME,
        getterName(getFieldName()),
        getterName("id"),
        SUB_ID_PARAM_NAME
    ).addStatement(
        "throw new $T($S)",
        IllegalStateException.class,
        entityModel.getEntityType().simpleName() +
            " is not associated with the given " +
            targetEntityModel.getEntityType().simpleName()
    ).endControlFlow();
  }
}
