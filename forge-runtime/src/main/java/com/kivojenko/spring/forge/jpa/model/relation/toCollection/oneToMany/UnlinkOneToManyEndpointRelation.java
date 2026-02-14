package com.kivojenko.spring.forge.jpa.model.relation.toCollection.oneToMany;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.DELETE_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.TRANSACTIONAL;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.*;

/**
 * Represents a relation that generates a DELETE endpoint to remove an entity from a OneToMany association.
 */
@SuperBuilder
public class UnlinkOneToManyEndpointRelation extends OneToManyEndpointRelation {
  @Override
  protected ClassName mapping() {
    return DELETE_MAPPING;
  }

  @Override
  protected String uri() {
    return super.uri() + "/{" + SUB_ID_PARAM_NAME + "}";
  }

  @Override
  protected String generatedMethodName() {
    return "remove" + capitalize(singularize(getFieldName()));
  }

  @Override
  public MethodSpec getServiceMethod() {
    var builder = MethodSpec
        .methodBuilder(generatedMethodName())
        .addAnnotation(TRANSACTIONAL)
        .addModifiers(Modifier.PUBLIC)
        .returns(void.class);
    addFindBase(builder);
    addFindSub(builder);
    return builder
        .addStatement("hooks.forEach(hook -> hook.beforeDelete($L, $L))", BASE_VAR_NAME, SUB_VAR_NAME)
        .addStatement("$N.$L(null)", SUB_VAR_NAME, setterName(mappedBy))
        .addStatement("hooks.forEach(hook -> hook.afterDelete($L, $L))", BASE_VAR_NAME, SUB_VAR_NAME)
        .build();
  }
}
