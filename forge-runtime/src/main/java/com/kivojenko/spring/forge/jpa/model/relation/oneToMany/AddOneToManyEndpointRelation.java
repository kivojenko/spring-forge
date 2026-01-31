package com.kivojenko.spring.forge.jpa.model.relation.oneToMany;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.setterName;

@SuperBuilder
public class AddOneToManyEndpointRelation extends OneToManyEndpointRelation {

  @Override
  protected ClassName mapping() {
    return POST_MAPPING;
  }

  @Override
  protected String generatedMethodName() {
    return "addNew" + capitalize(targetEntityModel.getEntityType().simpleName());
  }

  @Override
  public MethodSpec getControllerMethod() {
    var subParam = ParameterSpec
        .builder(targetEntityModel.getEntityType(), SUB_VAR_NAME)
        .addAnnotation(REQUEST_BODY)
        .build();

    return MethodSpec
        .methodBuilder((generatedMethodName()))
        .returns(targetEntityModel.getEntityType())
        .addModifiers(Modifier.PUBLIC)
        .addParameter(Long.class, BASE_ID_PARAM_NAME)
        .addParameter(subParam)
        .addStatement("return service.$L($L, $L)", generatedMethodName(), BASE_ID_PARAM_NAME, SUB_VAR_NAME)
        .addAnnotation(annotation(mapping()))
        .addAnnotation(TRANSACTIONAL)
        .build();
  }

  @Override
  public MethodSpec getServiceMethod() {
    var subParam = ParameterSpec.builder(targetEntityModel.getEntityType(), SUB_VAR_NAME).build();

    var builder = MethodSpec
        .methodBuilder((generatedMethodName()))
        .returns(targetEntityModel.getEntityType())
        .addModifiers(Modifier.PUBLIC)
        .addParameter(baseParamSpec())
        .addParameter(subParam);

    addFindBase(builder);
    return builder
        .addStatement("hooks.forEach(hook -> hook.beforeAdd($L, $L));", BASE_VAR_NAME, SUB_VAR_NAME)
        .addStatement("$L.$L($L)", SUB_VAR_NAME, setterName(mappedBy), BASE_VAR_NAME)
        .addStatement("var $L = $L.save($L)", UPDATED_SUB_VAR_NAME, getTargetRepositoryFieldName(), SUB_VAR_NAME)
        .addStatement("hooks.forEach(hook -> hook.afterAdd($L, $L));", BASE_VAR_NAME, UPDATED_SUB_VAR_NAME)
        .addStatement("return $L", UPDATED_SUB_VAR_NAME)
        .build();
  }
}

