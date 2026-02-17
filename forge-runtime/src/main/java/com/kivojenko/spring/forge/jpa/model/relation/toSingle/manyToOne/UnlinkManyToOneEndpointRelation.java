package com.kivojenko.spring.forge.jpa.model.relation.toSingle.manyToOne;

import com.kivojenko.spring.forge.jpa.utils.HttpStatusValue;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.TRANSACTIONAL;
import static com.kivojenko.spring.forge.jpa.utils.HttpStatusValue.NO_CONTENT;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.setterName;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.DELETE_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;

@SuperBuilder
public class UnlinkManyToOneEndpointRelation extends ManyToOneEndpointRelation {

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
    return "unlink" + capitalize(getFieldName());
  }

  @Override
  public MethodSpec getServiceMethod() {
    var builder = MethodSpec
        .methodBuilder(generatedMethodName())
        .addModifiers(Modifier.PUBLIC)
        .returns(void.class)
        .addAnnotation(TRANSACTIONAL);

    addFindBase(builder);
    addFindSub(builder);
    return builder
        .addStatement("hooks.forEach(hook -> hook.beforeDelete($L, $L))", BASE_VAR_NAME, SUB_VAR_NAME)
        .addStatement("$L.$L(null)", BASE_VAR_NAME, setterName(getFieldName()))
        .addStatement("hooks.forEach(hook -> hook.afterDelete($L, $L))", BASE_VAR_NAME, SUB_VAR_NAME)
        .build();
  }
}
