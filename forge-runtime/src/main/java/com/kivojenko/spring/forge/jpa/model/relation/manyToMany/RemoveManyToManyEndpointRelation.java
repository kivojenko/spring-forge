package com.kivojenko.spring.forge.jpa.model.relation.manyToMany;

import com.kivojenko.spring.forge.jpa.model.relation.ServiceRepositoryEndpointRelation;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.DELETE_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.getterName;

/**
 * Represents a relation that generates a DELETE endpoint to remove (unlink) an entity from a Many-to-Many association.
 */
@SuperBuilder
public class RemoveManyToManyEndpointRelation extends ServiceRepositoryEndpointRelation {

  @Override
  protected ClassName mapping() {
    return DELETE_MAPPING;
  }

  protected String uri() {
    return super.uri() + "/{" + SUB_ID_PARAM_NAME + "}";
  }

  @Override
  protected String generatedMethodName() {
    return "removeRelationWith" + capitalize(fieldName);
  }

  @Override
  public MethodSpec getServiceMethod() {
    var builder = MethodSpec.methodBuilder((generatedMethodName())).addModifiers(Modifier.PUBLIC).returns(void.class);

    addFindBase(builder);
    addFindSub(builder);

    return builder
        .addStatement("hooks.forEach(hook -> hook.beforeDelete($L, $L))", BASE_VAR_NAME, SUB_VAR_NAME)
        .addStatement(
            "$L.$L().removeIf(e -> e.$L() == $L)",
            BASE_VAR_NAME,
            getterName(fieldName),
            getterName("id"),
            SUB_ID_PARAM_NAME
        )
        .addStatement("hooks.forEach(hook -> hook.afterDelete($L, $L))", BASE_VAR_NAME, SUB_VAR_NAME)
        .build();
  }

}
