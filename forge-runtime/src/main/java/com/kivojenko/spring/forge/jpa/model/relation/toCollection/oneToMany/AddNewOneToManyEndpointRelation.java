package com.kivojenko.spring.forge.jpa.model.relation.toCollection.oneToMany;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;
import static com.kivojenko.spring.forge.jpa.utils.HttpStatusValue.CREATED;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.setterName;

/**
 * Represents a relation that generates a POST endpoint to add a new entity to a One-to-Many association.
 */
@SuperBuilder
public class AddNewOneToManyEndpointRelation extends OneToManyEndpointRelation {

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
        .methodBuilder(generatedMethodName())
        .addJavadoc("Creates and adds a new {@link $T} entity to a {@link $T} entity.\n", targetEntityModel.getEntityType(), entityModel.getEntityType())
        .addJavadoc("@param $L the ID of the {@link $T} entity\n", BASE_ID_PARAM_NAME, entityModel.getEntityType())
        .addJavadoc("@param $L the new {@link $T} entity to create and add\n", SUB_VAR_NAME, targetEntityModel.getEntityType())
        .addJavadoc("@return the newly created and added {@link $T} entity\n", targetEntityModel.getEntityType())
        .addAnnotation(annotation(mapping()))
        .addAnnotation(responseStatus(CREATED))
        .addModifiers(Modifier.PUBLIC)
        .returns(targetEntityModel.getEntityType())
        .addParameter(baseParamSpec(true))
        .addParameter(subParam)
        .addStatement("return service.$L($L, $L)", generatedMethodName(), BASE_ID_PARAM_NAME, SUB_VAR_NAME)
        .build();
  }

  @Override
  public MethodSpec getServiceMethod() {
    var subParam = ParameterSpec.builder(targetEntityModel.getEntityType(), SUB_VAR_NAME).build();

    var builder = MethodSpec
        .methodBuilder(generatedMethodName())
        .addJavadoc("Creates and adds a new {@link $T} entity to a {@link $T} entity.\n", targetEntityModel.getEntityType(), entityModel.getEntityType())
        .addJavadoc("@param $L the ID of the {@link $T} entity\n", BASE_ID_PARAM_NAME, entityModel.getEntityType())
        .addJavadoc("@param $L the new {@link $T} entity to create and add\n", SUB_VAR_NAME, targetEntityModel.getEntityType())
        .addJavadoc("@return the newly created and added {@link $T} entity\n", targetEntityModel.getEntityType())
        .returns(targetEntityModel.getEntityType())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(TRANSACTIONAL);

    addFindBase(builder);

    return builder
        .addParameter(subParam)
        .addStatement("hooks.forEach(hook -> hook.beforeAdd($L, $L));", BASE_VAR_NAME, SUB_VAR_NAME)
        .addStatement("$L.$L($L)", SUB_VAR_NAME, setterName(mappedBy), BASE_VAR_NAME)
        .addStatement("var $L = $L.save($L)", UPDATED_SUB_VAR_NAME, getTargetRepositorygetFieldName(), SUB_VAR_NAME)
        .addStatement("hooks.forEach(hook -> hook.afterAdd($L, $L));", BASE_VAR_NAME, UPDATED_SUB_VAR_NAME)
        .addStatement("return $L", UPDATED_SUB_VAR_NAME)
        .build();
  }
}

