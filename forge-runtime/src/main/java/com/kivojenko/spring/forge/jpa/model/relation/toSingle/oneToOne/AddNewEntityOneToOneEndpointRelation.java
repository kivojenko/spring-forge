package com.kivojenko.spring.forge.jpa.model.relation.toSingle.oneToOne;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import jakarta.persistence.OneToOne;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.POST_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.REQUEST_BODY;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.TRANSACTIONAL;
import static com.kivojenko.spring.forge.jpa.utils.HttpStatusValue.CREATED;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.*;

/**
 * Generates a POST to create and link a new target entity on a One-to-One field.
 * Example: POST /entities/{id}/profile with body of the target type creates and links the profile.
 */
@SuperBuilder
public class AddNewEntityOneToOneEndpointRelation extends EndpointRelation {

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

    // Decide owner side using @OneToOne(mappedBy)
    var oneToOne = field.getAnnotation(OneToOne.class);
    String mappedBy = oneToOne != null ? oneToOne.mappedBy() : "";

    if (mappedBy != null && !mappedBy.isBlank()) {
      // Base is inverse side; owning side is on target. Set back-reference and save target.
      return builder
          .addParameter(subParam)
          .addStatement("$L.$L($L)", SUB_VAR_NAME, setterName(mappedBy), BASE_VAR_NAME)
          .addStatement("var $L = $L.save($L)", UPDATED_SUB_VAR_NAME, getTargetRepositorygetFieldName(), SUB_VAR_NAME)
          .addStatement("return $L", UPDATED_SUB_VAR_NAME)
          .build();
    } else {
      // Base is owning side; save target, set on base, and persist base.
      return builder
          .addParameter(subParam)
          .addStatement("var $L = $L.save($L)", UPDATED_SUB_VAR_NAME, getTargetRepositorygetFieldName(), SUB_VAR_NAME)
          .addStatement("$L.$L($L)", BASE_VAR_NAME, setterName(getFieldName()), UPDATED_SUB_VAR_NAME)
          .addStatement("var $L = repository.save($L)", UPDATED_BASE_VAR_NAME, BASE_VAR_NAME)
          .addStatement("return $L.$L()", UPDATED_BASE_VAR_NAME, getterName(getFieldName()))
          .build();
    }
  }

  @Override
  public com.squareup.javapoet.FieldSpec getServiceField() {
    // Need target repository for saving the created sub-entity
    return getTargetRepositoryFieldSpec();
  }
}
