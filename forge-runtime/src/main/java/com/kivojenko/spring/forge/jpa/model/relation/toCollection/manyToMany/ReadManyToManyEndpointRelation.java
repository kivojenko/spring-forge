package com.kivojenko.spring.forge.jpa.model.relation.toCollection.manyToMany;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.GET_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.getterName;

/**
 * Represents a relation that generates a GET endpoint to read a Many-to-Many associated entity.
 */
@SuperBuilder
public class ReadManyToManyEndpointRelation extends EndpointRelation {
  protected String generatedMethodName() {
    return getterName(getFieldName());
  }

  @Override
  public MethodSpec getControllerMethod() {
    return MethodSpec
        .methodBuilder(generatedMethodName())
        .addJavadoc("Retrieves the {@link $T} associated with the {@link $T} by its ID.\n", targetEntityModel.getEntityType(), entityModel.getEntityType())
        .addJavadoc("@param $L the ID of the {@link $T} entity\n", BASE_ID_PARAM_NAME, entityModel.getEntityType())
        .addJavadoc("@return the associated {@link $T} entities\n", targetEntityModel.getEntityType())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(annotation(GET_MAPPING))
        .returns(TypeName.get(field.asType()))
        .addParameter(baseParamSpec(true))
        .addStatement("return getById($L).$L()", BASE_ID_PARAM_NAME, getterName(getFieldName()))
        .build();
  }

}

