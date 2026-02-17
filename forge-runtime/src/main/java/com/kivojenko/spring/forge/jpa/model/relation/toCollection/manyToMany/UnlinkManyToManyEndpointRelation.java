package com.kivojenko.spring.forge.jpa.model.relation.toCollection.manyToMany;

import com.kivojenko.spring.forge.jpa.model.relation.ServiceRepositoryEndpointRelation;
import com.kivojenko.spring.forge.jpa.utils.HttpStatusValue;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.DELETE_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.TRANSACTIONAL;
import static com.kivojenko.spring.forge.jpa.utils.HttpStatusValue.NO_CONTENT;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.*;

/**
 * Represents a relation that generates a DELETE endpoint to remove (unlink) an entity from a Many-to-Many association.
 */
@SuperBuilder
public class UnlinkManyToManyEndpointRelation extends ServiceRepositoryEndpointRelation {

  @Override
  protected ClassName mapping() {
    return DELETE_MAPPING;
  }

  @Override
  protected HttpStatusValue httpStatus() {
    return NO_CONTENT;
  }

  protected String uri() {
    return super.uri() + "/{" + SUB_ID_PARAM_NAME + "}";
  }

  @Override
  protected String generatedMethodName() {
    return "removeRelationWith" + capitalize(singularize(getFieldName()));
  }

  @Override
  public MethodSpec getServiceMethod() {
    var builder = MethodSpec
        .methodBuilder(generatedMethodName())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(TRANSACTIONAL)
        .returns(void.class);

    addFindBase(builder);
    addFindSub(builder);

    return builder
        .addStatement("hooks.forEach(hook -> hook.beforeDelete($L, $L))", BASE_VAR_NAME, SUB_VAR_NAME)
        .addStatement(
            "$L.$L().removeIf(e -> e.$L() == $L)",
            BASE_VAR_NAME,
            getterName(getFieldName()),
            getterName("id"),
            SUB_ID_PARAM_NAME
        )
        .addStatement("hooks.forEach(hook -> hook.afterDelete($L, $L))", BASE_VAR_NAME, SUB_VAR_NAME)
        .build();
  }

}
