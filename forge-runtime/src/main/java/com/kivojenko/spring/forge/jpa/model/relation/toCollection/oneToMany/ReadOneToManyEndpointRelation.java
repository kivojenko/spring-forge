package com.kivojenko.spring.forge.jpa.model.relation.toCollection.oneToMany;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.GET_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.ITERABLE;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;

@SuperBuilder
public class ReadOneToManyEndpointRelation extends EndpointRelation {
  private final String methodName;

  protected String generatedMethodName() {
    return methodName != null ? methodName : "get" + capitalize(getFieldName());
  }

  @Override
  public MethodSpec getControllerMethod() {
    return MethodSpec
        .methodBuilder(generatedMethodName())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(annotation(GET_MAPPING))
        .returns(ParameterizedTypeName.get(ITERABLE, targetEntityModel.getEntityType()))
        .addParameter(baseParamSpec(true))
        .addStatement("return getById($L).$L()", BASE_ID_PARAM_NAME, generatedMethodName())
        .build();
  }

}

