package com.kivojenko.spring.forge.jpa.model.relation.toSingle;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.GET_MAPPING;

/**
 * Represents a relation that generates a GET endpoint exposing a single (non-collection) value
 * returned by an entity method annotated with {@link com.kivojenko.spring.forge.annotation.endpoint.WithGetEndpoint}.
 */
@SuperBuilder
public class ReadSingleMethodEndpointRelation extends EndpointRelation {
  private final String methodName;

  protected String generatedMethodName() {
    return methodName;
  }

  @Override
  public MethodSpec getControllerMethod() {
    return MethodSpec
        .methodBuilder(generatedMethodName())
        .addJavadoc("Retrieves the {@link $T} associated with the {@link $T} by its ID.\n", targetEntityModel.getEntityType(), entityModel.getEntityType())
        .addJavadoc("@param $L the ID of the {@link $T} entity\n", baseIdParamName(), entityModel.getEntityType())
        .addJavadoc("@return the associated {@link $T}\n", targetEntityModel.getEntityType())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(annotation(GET_MAPPING))
        .returns(targetEntityModel.getEntityType())
        .addParameter(baseParamSpec(true))
        .addStatement("return getById($L).$L()", baseIdParamName(), generatedMethodName())
        .build();
  }

}
