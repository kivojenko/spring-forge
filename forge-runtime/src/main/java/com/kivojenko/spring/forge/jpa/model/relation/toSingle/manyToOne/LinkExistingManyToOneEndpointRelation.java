package com.kivojenko.spring.forge.jpa.model.relation.toSingle.manyToOne;

import com.kivojenko.spring.forge.jpa.utils.StringUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.PUT_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.TRANSACTIONAL;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;


@SuperBuilder
public class LinkExistingManyToOneEndpointRelation extends ManyToOneEndpointRelation {
  @Override
  protected String generatedMethodName() {
    return "addExisting" + capitalize(getFieldName());
  }

  @Override
  protected String uri() {
    return super.uri() + "/{" + SUB_ID_PARAM_NAME + "}";
  }

  @Override
  protected ClassName mapping() {
    return PUT_MAPPING;
  }

  @Override
  public MethodSpec getServiceMethod() {
    var builder = MethodSpec
        .methodBuilder(generatedMethodName())
        .addJavadoc("Links an existing {@link $T} entity as the association for a {@link $T} entity.\n", targetEntityModel.getEntityType(), entityModel.getEntityType())
        .addJavadoc("@param $L the ID of the {@link $T} entity\n", BASE_ID_PARAM_NAME, entityModel.getEntityType())
        .addJavadoc("@param $L the ID of the {@link $T} entity to link\n", SUB_ID_PARAM_NAME, targetEntityModel.getEntityType())
        .addJavadoc("@return the linked {@link $T} entity\n", targetEntityModel.getEntityType())
        .addAnnotation(TRANSACTIONAL)
        .addModifiers(Modifier.PUBLIC)
        .returns(targetEntityModel.getEntityType());

    addFindBase(builder);
    addFindSub(builder);

    return builder
        .addStatement("hooks.forEach(hook -> hook.beforeAdd($L, $L));", BASE_VAR_NAME, SUB_VAR_NAME)
        .addStatement("$L.$L($L)", BASE_VAR_NAME, StringUtils.setterName(getFieldName()), SUB_VAR_NAME)
        .addStatement("var $L = repository.save($L)", UPDATED_BASE_VAR_NAME, BASE_VAR_NAME)
        .addStatement(
            "hooks.forEach(hook -> hook.afterAdd($L, $L.$L()));",
            UPDATED_BASE_VAR_NAME,
            UPDATED_BASE_VAR_NAME,
            StringUtils.getterName(getFieldName())
        )
        .addStatement("return $L.$L()", UPDATED_BASE_VAR_NAME, StringUtils.getterName(getFieldName()))
        .build();
  }
}

