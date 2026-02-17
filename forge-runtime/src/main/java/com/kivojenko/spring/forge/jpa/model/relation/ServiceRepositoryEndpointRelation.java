package com.kivojenko.spring.forge.jpa.model.relation;

import com.kivojenko.spring.forge.jpa.utils.HttpStatusValue;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityNotFoundException;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.StringUtils.decapitalize;

/**
 * Base class for endpoint relations that involve both Service and Repository layers.
 * Provides default implementations for generating controller and service methods.
 */
@SuperBuilder
public abstract class ServiceRepositoryEndpointRelation extends EndpointRelation {

  protected abstract ClassName mapping();

  @Nullable
  protected HttpStatusValue httpStatus() {
    return null;
  }

  @Override
  public FieldSpec getServiceField() {
    return getTargetRepositoryFieldSpec();
  }

  @Override
  public MethodSpec getControllerMethod() {
    var spec = MethodSpec
        .methodBuilder(generatedMethodName())
        .returns(void.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(baseParamSpec(true))
        .addParameter(subParamSpec(true))
        .addStatement("service.$L($L, $L)", generatedMethodName(), BASE_ID_PARAM_NAME, SUB_ID_PARAM_NAME)
        .addAnnotation(annotation(mapping()));

    var httpStatus = httpStatus();
    if (httpStatus != null) {
      spec.addAnnotation(responseStatus(httpStatus));
    }
    return spec.build();
  }

  protected void addFindSub(MethodSpec.Builder methodSpec) {
    addFindSub(methodSpec, false);
  }

  protected void addFindSub(MethodSpec.Builder methodSpec, boolean pathVariable) {
    methodSpec.addParameter(subParamSpec(pathVariable)).addStatement(
        "var $L = $L.findById($L).orElseThrow($T::new)",
        SUB_VAR_NAME,
        decapitalize(targetEntityModel.getRepositoryName()),
        SUB_ID_PARAM_NAME,
        EntityNotFoundException.class
    );
    checkFoundSub(methodSpec);
  }

  protected void checkFoundSub(MethodSpec.Builder methodSpec) {
  }

}
