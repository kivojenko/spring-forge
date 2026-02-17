package com.kivojenko.spring.forge.jpa.model.relation.toCollection.manyToMany;

import com.kivojenko.spring.forge.jpa.model.relation.ServiceRepositoryEndpointRelation;
import com.kivojenko.spring.forge.jpa.utils.HttpStatusValue;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.PUT_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.TRANSACTIONAL;
import static com.kivojenko.spring.forge.jpa.utils.HttpStatusValue.NO_CONTENT;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.getterName;

/**
 * Represents a relation that generates a PUT endpoint to link an existing entity to a Many-to-Many association.
 */
@SuperBuilder
public class LinkExistingManyToManyEndpointRelation extends ServiceRepositoryEndpointRelation {

  @Override
  protected String generatedMethodName() {
    return "addExisting" + capitalize(getFieldName());
  }

  @Override
  protected HttpStatusValue httpStatus() {
    return NO_CONTENT;
  }

  @Override
  protected String uri() {
    return super.uri() + "/{" + SUB_ID_PARAM_NAME + "}";
  }

  @Override
  protected ClassName mapping() {
    return PUT_MAPPING;
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
        .addStatement("hooks.forEach(hook -> hook.beforeAdd($L, $L));", BASE_VAR_NAME, SUB_VAR_NAME)
        .beginControlFlow("if ($L.$L().contains($L))", BASE_VAR_NAME, getterName(getFieldName()), SUB_VAR_NAME)
        .addStatement("return")
        .endControlFlow()
        .addStatement("$L.$L().add($L)", BASE_VAR_NAME, getterName(getFieldName()), SUB_VAR_NAME)
        .addStatement("var $L = repository.save($L)", UPDATED_BASE_VAR_NAME, BASE_VAR_NAME)
        .addStatement(
            "hooks.forEach(hook -> hook.afterAdd($L, $L.$L()));",
            UPDATED_BASE_VAR_NAME,
            UPDATED_BASE_VAR_NAME,
            getterName(getFieldName())
        )
        .build();
  }
}

