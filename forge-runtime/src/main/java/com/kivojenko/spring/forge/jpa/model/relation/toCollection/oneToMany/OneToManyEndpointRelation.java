package com.kivojenko.spring.forge.jpa.model.relation.toCollection.oneToMany;

import com.kivojenko.spring.forge.jpa.model.relation.ServiceRepositoryEndpointRelation;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import static com.kivojenko.spring.forge.jpa.utils.StringUtils.getterName;


/**
 * Base class for One-to-Many endpoint relations.
 */
@SuperBuilder
public abstract class OneToManyEndpointRelation extends ServiceRepositoryEndpointRelation {
  protected String mappedBy;

  @Override
  protected void checkFoundSub(MethodSpec.Builder methodSpec) {
    methodSpec.beginControlFlow(
        "if ($N.$L() != null && !$N.$L().$L().equals($L))",
        SUB_VAR_NAME,
        getterName(mappedBy),
        SUB_VAR_NAME,
        getterName(mappedBy),
        entityModel.getGetterName(),
        BASE_ID_PARAM_NAME
    ).addStatement(
        "throw new $T($S)",
        IllegalStateException.class,
        entityModel.getEntityType().simpleName() +
            " is not associated with the given " +
            targetEntityModel.getEntityType().simpleName()
    ).endControlFlow();
  }
}
