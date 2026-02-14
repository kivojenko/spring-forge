package com.kivojenko.spring.forge.jpa.model.relation.oneToOne;

import com.kivojenko.spring.forge.jpa.model.relation.ServiceRepositoryEndpointRelation;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.DELETE_MAPPING;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.TRANSACTIONAL;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.*;

/**
 * Represents a relation that generates a DELETE endpoint to remove (unlink) an entity from a One-to-One association.
 */
@SuperBuilder
public class RemoveOneToOneEndpointRelation extends ServiceRepositoryEndpointRelation {

  @Override
  protected ClassName mapping() {
    return DELETE_MAPPING;
  }

  protected String uri() {
    return super.uri();
  }

  @Override
  protected String generatedMethodName() {
    return "remove" + capitalize(fieldName);
  }

  @Override
  public MethodSpec getControllerMethod() {
    return MethodSpec
        .methodBuilder((generatedMethodName()))
        .returns(void.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(baseParamSpec(true))
        .addStatement("service.$L($L)", generatedMethodName(), BASE_ID_PARAM_NAME)
        .addAnnotation(annotation(mapping()))
        .addAnnotation(TRANSACTIONAL)
        .build();
  }

  @Override
  public MethodSpec getServiceMethod() {
    var builder = MethodSpec.methodBuilder((generatedMethodName())).addModifiers(Modifier.PUBLIC).returns(void.class);

    addFindBase(builder);
    return builder
        .addStatement("var $L = $L.$L()", SUB_VAR_NAME, BASE_VAR_NAME, getterName(fieldName))
        .beginControlFlow("if ($L == null)", SUB_VAR_NAME)
        .addStatement(
            "throw new $T($S)",
            IllegalStateException.class,
            entityModel.getEntityType().simpleName() +
                " is not associated with the any " +
                targetEntityModel.getEntityType().simpleName()
        )
        .endControlFlow()
        .addStatement("hooks.forEach(hook -> hook.beforeDelete($L, $L))", BASE_VAR_NAME, SUB_VAR_NAME)
        .addStatement("$L.$L(null)", BASE_VAR_NAME, setterName(fieldName))
        .addStatement("hooks.forEach(hook -> hook.afterDelete($L, $L))", BASE_VAR_NAME, SUB_VAR_NAME)
        .build();
  }

}
