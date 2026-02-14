package com.kivojenko.spring.forge.jpa.model.relation;

import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.AUTOWIRED;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.PATH_VARIABLE;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.decapitalize;

/**
 * Abstract base class for defining extra endpoints related to entity associations or methods.
 */
@Data
@SuperBuilder
public abstract class EndpointRelation {

  protected static final String BASE_ID_PARAM_NAME = "baseId";
  protected static final String BASE_VAR_NAME = "base";
  protected static final String UPDATED_BASE_VAR_NAME = "updatedBase";

  protected static final String SUB_ID_PARAM_NAME = "subId";
  protected static final String SUB_VAR_NAME = "sub";
  protected static final String UPDATED_SUB_VAR_NAME = "updatedSub";

  protected ParameterSpec baseParamSpec() {
    return baseParamSpec(false);
  }

  protected ParameterSpec baseParamSpec(boolean pathVariable) {
    var param = ParameterSpec.builder(entityModel.getJpaId().type(), BASE_ID_PARAM_NAME);
    if (pathVariable) {
      param.addAnnotation(PATH_VARIABLE);
    }
    return param.build();
  }

  protected ParameterSpec subParamSpec(boolean pathVariable) {
    var param = ParameterSpec.builder(targetEntityModel.getJpaId().type(), SUB_ID_PARAM_NAME);
    if (pathVariable) {
      param.addAnnotation(PATH_VARIABLE);
    }
    return param.build();
  }

  protected String path;

  protected String uri() {
    return "/{" + BASE_ID_PARAM_NAME + "}/" + path;
  }
  
  @Getter(lazy = true)
  private final String fieldName = fieldName();
  
  private String fieldName() {
    if (field == null) return "";
    return field.getSimpleName().toString();
  }

  protected abstract String generatedMethodName();

  protected VariableElement field;

  protected JpaEntityModel entityModel;

  protected JpaEntityModel targetEntityModel;

  public FieldSpec getControllerField() {
    return null;
  }

  public FieldSpec getServiceField() {
    return null;
  }

  public MethodSpec getControllerMethod() {
    return null;
  }

  public MethodSpec getServiceMethod() {
    return null;
  }

  protected String getTargetRepositorygetFieldName() {
    return decapitalize(targetEntityModel.getRepositoryName());
  }

  protected FieldSpec getTargetRepositoryFieldSpec() {
    return FieldSpec
        .builder(targetEntityModel.getRepositoryType(), getTargetRepositorygetFieldName())
        .addModifiers(Modifier.PRIVATE)
        .addAnnotation(AnnotationSpec.builder(AUTOWIRED).build())
        .build();
  }

  /**
   * Returns an annotation specification for the given mapping.
   *
   * @param mapping the mapping class name
   * @return the annotation specification
   */
  protected AnnotationSpec annotation(ClassName mapping) {
    return AnnotationSpec.builder(mapping).addMember("value", "$S", uri()).build();
  }

  protected void addFindBase(MethodSpec.Builder methodSpec) {
    addFindBase(methodSpec, false);
  }

  /**
   * Adds a statement to find the base entity by its ID to the given method builder.
   *
   * @param methodSpec the method builder
   */
  protected void addFindBase(MethodSpec.Builder methodSpec, boolean pathVariable) {
    methodSpec
        .addParameter(baseParamSpec(pathVariable))
        .addStatement("var $L = getById($L)", BASE_VAR_NAME, BASE_ID_PARAM_NAME);
  }

  /**
   * Adds the service method and any required fields to the given type builder.
   *
   * @param spec the type builder
   */
  public void addMethod(TypeSpec.Builder spec) {
    var serviceMethod = getServiceMethod();
    if (serviceMethod != null) spec.addMethod(serviceMethod);

    var field = getServiceField();
    if (field != null && spec.fieldSpecs.stream().noneMatch(s -> s.type.equals(field.type))) {
      spec.addField(field);
    }
  }

  /**
   * Adds the controller endpoint and any required fields to the given type builder.
   *
   * @param spec the type builder
   */
  public void addEndpoint(TypeSpec.Builder spec) {
    var method = getControllerMethod();
    if (method != null) {
      spec.addMethod(method);
    }

    var field = getControllerField();
    if (field != null && spec.fieldSpecs.stream().noneMatch(s -> s.type.equals(field.type))) {
      spec.addField(field);
    }
  }
}