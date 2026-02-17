package com.kivojenko.spring.forge.jpa.model.relation.toSingle.manyToOne;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.GET_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.getterName;

/**
 * Represents a relation that generates a GET endpoint to read a Many-to-One associated entity.
 */
@SuperBuilder
public class ReadManyToOneEndpointRelation extends EndpointRelation {
  protected String generatedMethodName() {
    return getterName(getFieldName());
  }

  @Override
  public MethodSpec getControllerMethod() {
    return MethodSpec
        .methodBuilder(generatedMethodName())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(annotation(GET_MAPPING))
        .returns(targetEntityModel.getEntityType())
        .addParameter(baseParamSpec(true))
        .addStatement("return getById($L).$L()", BASE_ID_PARAM_NAME, getterName(getFieldName()))
        .build();
  }

}

