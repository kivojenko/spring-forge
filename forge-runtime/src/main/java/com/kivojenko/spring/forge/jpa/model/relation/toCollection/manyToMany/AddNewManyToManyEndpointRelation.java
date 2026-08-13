package com.kivojenko.spring.forge.jpa.model.relation.toCollection.manyToMany;

import com.kivojenko.spring.forge.jpa.model.relation.ServiceRepositoryEndpointRelation;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;
import static com.kivojenko.spring.forge.jpa.utils.HttpStatusValue.CREATED;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.getterName;

/**
 * Generates a POST to create and link a new target entity in a Many-to-Many collection.
 * Example: POST /entities/{id}/categories with body of the target type creates and adds the category.
 */
@SuperBuilder
public class AddNewManyToManyEndpointRelation extends ServiceRepositoryEndpointRelation {

  @Override
  protected String generatedMethodName() {
    return "addNew" + capitalize(getFieldName());
  }

  @Override
  protected ClassName mapping() {
    return POST_MAPPING;
  }

  @Override
  protected com.kivojenko.spring.forge.jpa.utils.HttpStatusValue httpStatus() {
    return CREATED;
  }

  @Override
  public MethodSpec getControllerMethod() {
    var subParam = ParameterSpec
        .builder(targetEntityModel.getEntityType(), SUB_VAR_NAME)
        .addAnnotation(REQUEST_BODY)
        .build();

    return MethodSpec
        .methodBuilder(generatedMethodName())
        .addJavadoc("Creates and links a new {@link $T} to a {@link $T} entity.\n", targetEntityModel.getEntityType(), entityModel.getEntityType())
        .addJavadoc("@param $L the ID of the {@link $T} entity\n", baseIdParamName(), entityModel.getEntityType())
        .addJavadoc("@param $L the new {@link $T} entity to create and link\n", SUB_VAR_NAME, targetEntityModel.getEntityType())
        .addJavadoc("@return the newly created and linked {@link $T} entity\n", targetEntityModel.getEntityType())
        .addAnnotation(annotation(mapping()))
        .addAnnotation(responseStatus(CREATED))
        .addModifiers(Modifier.PUBLIC)
        .returns(targetEntityModel.getEntityType())
        .addParameter(baseParamSpec(true))
        .addParameter(subParam)
        .addStatement("return service.$L($L, $L)", generatedMethodName(), baseIdParamName(), SUB_VAR_NAME)
        .build();
  }

  @Override
  public MethodSpec getServiceMethod() {
    var subParam = ParameterSpec.builder(targetEntityModel.getEntityType(), SUB_VAR_NAME).build();

    var builder = MethodSpec
        .methodBuilder(generatedMethodName())
        .addJavadoc("Creates and links a new {@link $T} to a {@link $T} entity.\n", targetEntityModel.getEntityType(), entityModel.getEntityType())
        .addJavadoc("@param $L the ID of the {@link $T} entity\n", baseIdParamName(), entityModel.getEntityType())
        .addJavadoc("@param $L the new {@link $T} entity to create and link\n", SUB_VAR_NAME, targetEntityModel.getEntityType())
        .addJavadoc("@return the newly created and linked {@link $T} entity\n", targetEntityModel.getEntityType())
        .returns(targetEntityModel.getEntityType())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(TRANSACTIONAL);

    addFindBase(builder);

    return builder
        .addParameter(subParam)
        .addStatement("var $L = $L.save($L)", UPDATED_SUB_VAR_NAME, getTargetRepositorygetFieldName(), SUB_VAR_NAME)
        .beginControlFlow("if (!$L.$L().contains($L))", BASE_VAR_NAME, getterName(getFieldName()), UPDATED_SUB_VAR_NAME)
        .addStatement("$L.$L().add($L)", BASE_VAR_NAME, getterName(getFieldName()), UPDATED_SUB_VAR_NAME)
        .endControlFlow()
        .addStatement("repository.save($L)", BASE_VAR_NAME)
        .addStatement("return $L", UPDATED_SUB_VAR_NAME)
        .build();
  }
}
