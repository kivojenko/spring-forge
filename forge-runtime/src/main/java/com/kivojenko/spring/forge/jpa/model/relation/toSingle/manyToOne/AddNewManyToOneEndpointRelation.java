package com.kivojenko.spring.forge.jpa.model.relation.toSingle.manyToOne;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.POST_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.REQUEST_BODY;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.TRANSACTIONAL;
import static com.kivojenko.spring.forge.jpa.utils.HttpStatusValue.CREATED;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.getterName;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.setterName;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import javax.lang.model.element.Modifier;
import lombok.experimental.SuperBuilder;

/**
 * Generates a POST to create and link a new target entity on a Many-to-One field.
 * Example: POST /entities/{id}/author with body of the target type creates and links the author.
 */
@SuperBuilder
public class AddNewManyToOneEndpointRelation extends ManyToOneEndpointRelation {

  @Override
  protected com.squareup.javapoet.ClassName mapping() {
    return POST_MAPPING;
  }

  @Override
  protected String generatedMethodName() {
    return "addNew" + capitalize(getFieldName());
  }

  @Override
  public MethodSpec getControllerMethod() {
    var subParam = ParameterSpec
        .builder(targetEntityModel.getEntityType(), SUB_VAR_NAME)
        .addAnnotation(REQUEST_BODY)
        .build();

    return MethodSpec
        .methodBuilder(generatedMethodName())
        .addJavadoc(
            "Creates and links a new {@link $T} as the {@link $T}.$L value.\n",
            targetEntityModel.getEntityType(), entityModel.getEntityType(), getFieldName()
        )
        .addJavadoc("@param $L the ID of the {@link $T} entity\n", baseIdParamName(), entityModel.getEntityType())
        .addJavadoc("@param $L the new {@link $T} to create and link\n", SUB_VAR_NAME, targetEntityModel.getEntityType())
        .addJavadoc("@return the newly created and linked {@link $T}\n", targetEntityModel.getEntityType())
        .addAnnotation(annotation(POST_MAPPING))
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
        .addJavadoc(
            "Creates and links a new {@link $T} as the {@link $T}.$L value.\n",
            targetEntityModel.getEntityType(), entityModel.getEntityType(), getFieldName()
        )
        .addJavadoc("@param $L the ID of the {@link $T} entity\n", baseIdParamName(), entityModel.getEntityType())
        .addJavadoc("@param $L the new {@link $T} to create and link\n", SUB_VAR_NAME, targetEntityModel.getEntityType())
        .addJavadoc("@return the newly created and linked {@link $T}\n", targetEntityModel.getEntityType())
        .returns(targetEntityModel.getEntityType())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(TRANSACTIONAL);

    addFindBase(builder);

    return builder
        .addParameter(subParam)
        .addStatement("var $L = $L.save($L)", UPDATED_SUB_VAR_NAME, getTargetRepositorygetFieldName(), SUB_VAR_NAME)
        .addStatement("$L.$L($L)", BASE_VAR_NAME, setterName(getFieldName()), UPDATED_SUB_VAR_NAME)
        .addStatement("var $L = repository.save($L)", UPDATED_BASE_VAR_NAME, BASE_VAR_NAME)
        .addStatement("return $L.$L()", UPDATED_BASE_VAR_NAME, getterName(getFieldName()))
        .build();
  }

  @Override
  public com.squareup.javapoet.FieldSpec getServiceField() {
    return getTargetRepositoryFieldSpec();
  }
}
