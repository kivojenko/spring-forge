package com.kivojenko.spring.forge.jpa.model.relation;

import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.*;
import lombok.Data;
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

  /**
   * Returns a parameter specification for the base entity ID.
   *
   * @return the parameter specification
   */
  protected ParameterSpec baseParamSpec() {
    return ParameterSpec
        .builder(entityModel.getJpaId().type(), BASE_ID_PARAM_NAME)
        .addAnnotation(PATH_VARIABLE)
        .build();
  }

  /**
   * Returns a parameter specification for the sub-entity ID.
   *
   * @return the parameter specification
   */
  protected ParameterSpec subParamSpec() {
    return ParameterSpec
        .builder(targetEntityModel.getJpaId().type(), SUB_ID_PARAM_NAME)
        .addAnnotation(PATH_VARIABLE)
        .build();
  }

  /**
   * The relative path for the endpoint.
   */
  protected String path;

  /**
   * Returns the URI for the endpoint.
   *
   * @return the URI
   */
  protected String uri() {
    return "/{" + BASE_ID_PARAM_NAME + "}/" + path;
  }

  /**
   * The name of the method that provides the data for this relation.
   */
  protected String methodName;

  /**
   * The name of the field this relation is based on, if applicable.
   */
  protected String fieldName;

  /**
   * The element representing the field this relation is based on.
   */
  protected VariableElement field;

  /**
   * The model of the entity that owns this relation.
   */
  protected JpaEntityModel entityModel;

  /**
   * The model of the target entity of this relation.
   */
  protected JpaEntityModel targetEntityModel;

  /**
   * Returns the field specification to be added to the generated controller, if any.
   *
   * @return the field specification or null
   */
  public FieldSpec getControllerField() {
    return null;
  }

  /**
   * Returns the field specification to be added to the generated service, if any.
   *
   * @return the field specification or null
   */
  public FieldSpec getServiceField() {
    return null;
  }

  /**
   * Returns the method specification for the generated controller.
   *
   * @return the method specification or null
   */
  public MethodSpec getControllerMethod() {
    return null;
  }

  /**
   * Returns the method specification for the generated service.
   *
   * @return the method specification or null
   */
  public MethodSpec getServiceMethod() {
    return null;
  }

  /**
   * Returns the name of the target repository field.
   *
   * @return the field name
   */
  protected String getTargetRepositoryFieldName() {
    return decapitalize(targetEntityModel.getRepositoryName());
  }

  /**
   * Returns the field specification for the target repository.
   *
   * @return the field specification
   */
  protected FieldSpec getTargetRepositoryFieldSpec() {
    return FieldSpec
        .builder(targetEntityModel.getRepositoryType(), getTargetRepositoryFieldName())
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

  /**
   * Adds a statement to find the base entity by its ID to the given method builder.
   *
   * @param methodSpec the method builder
   * @return the method builder
   */
  protected MethodSpec.Builder addFindBase(MethodSpec.Builder methodSpec) {
    return methodSpec.addStatement("var $L = getById($L)", BASE_VAR_NAME, BASE_ID_PARAM_NAME);
  }

  /**
   * Adds the service method and any required fields to the given type builder.
   *
   * @param spec the type builder
   * @return the type builder
   */
  public TypeSpec.Builder addMethod(TypeSpec.Builder spec) {
    var serviceMethod = getServiceMethod();
    if (serviceMethod != null) spec.addMethod(serviceMethod);

    var field = getServiceField();
    if (field != null && spec.fieldSpecs.stream().noneMatch(s -> s.type.equals(field.type))) spec.addField(field);
    return spec;
  }

  /**
   * Adds the controller endpoint and any required fields to the given type builder.
   *
   * @param spec the type builder
   * @return the type builder
   */
  public TypeSpec.Builder addEndpoint(TypeSpec.Builder spec) {
    var method = getControllerMethod();
    if (method != null) spec.addMethod(method);

    var field = getControllerField();
    if (field != null && spec.fieldSpecs.stream().noneMatch(s -> s.type.equals(field.type))) spec.addField(field);
    return spec;
  }
}