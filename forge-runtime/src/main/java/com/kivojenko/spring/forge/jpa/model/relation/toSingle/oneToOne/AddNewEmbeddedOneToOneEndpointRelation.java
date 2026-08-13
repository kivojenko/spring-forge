package com.kivojenko.spring.forge.jpa.model.relation.toSingle.oneToOne;

import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.POST_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.REQUEST_BODY;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.TRANSACTIONAL;
import static com.kivojenko.spring.forge.jpa.utils.HttpStatusValue.CREATED;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.getterName;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.setterName;

/**
 * Generates a POST to set or replace an {@code @Embedded} value object on a One-to-One field.
 * Example: POST /entities/{id}/address with body of the embeddable type sets the address.
 */
@SuperBuilder
public class AddNewEmbeddedOneToOneEndpointRelation extends EndpointRelation {

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
            "Sets a new {@link $T} as the {@link $T}.$L value.\\n",
            targetEntityModel.getEntityType(), entityModel.getEntityType(), getFieldName()
        )
        .addJavadoc("@param $L the ID of the {@link $T} entity\\n", baseIdParamName(), entityModel.getEntityType())
        .addJavadoc("@param $L the new {@link $T} value to set\\n", SUB_VAR_NAME, targetEntityModel.getEntityType())
        .addJavadoc("@return the newly set {@link $T} value\\n", targetEntityModel.getEntityType())
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
            "Sets a new {@link $T} as the {@link $T}.$L value.\\n",
            targetEntityModel.getEntityType(), entityModel.getEntityType(), getFieldName()
        )
        .addJavadoc("@param $L the ID of the {@link $T} entity\\n", baseIdParamName(), entityModel.getEntityType())
        .addJavadoc("@param $L the new {@link $T} value to set\\n", SUB_VAR_NAME, targetEntityModel.getEntityType())
        .addJavadoc("@return the newly set {@link $T} value\\n", targetEntityModel.getEntityType())
        .returns(targetEntityModel.getEntityType())
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(TRANSACTIONAL);

    addFindBase(builder);

    return builder
        .addParameter(subParam)
        .addStatement("$L.$L($L)", BASE_VAR_NAME, setterName(getFieldName()), SUB_VAR_NAME)
        .addStatement("var $L = repository.save($L)", UPDATED_BASE_VAR_NAME, BASE_VAR_NAME)
        .addStatement("return $L.$L()", UPDATED_BASE_VAR_NAME, getterName(getFieldName()))
        .build();
  }
}
