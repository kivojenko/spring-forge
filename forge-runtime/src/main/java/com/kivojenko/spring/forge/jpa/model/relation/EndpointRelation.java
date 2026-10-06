package com.kivojenko.spring.forge.jpa.model.relation;

import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.kivojenko.spring.forge.jpa.utils.HttpStatusValue;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.*;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.decapitalize;

/**
 * Abstract base class for defining extra endpoints related to entity associations or methods.
 */
@Data
@SuperBuilder
public abstract class EndpointRelation {

  protected String baseIdParamName() {
    return entityModel.getJpaId().name();
  }

  protected String subIdParamName() {
    if (targetEntityModel == null) {
      return "subId";
    }
    String name = targetEntityModel.getJpaId().name();
    if (name.equals(baseIdParamName())) {
      return decapitalize(targetEntityModel.getEntityType().simpleName()) + capitalize(name);
    }
    return name;
  }

  protected static final String BASE_VAR_NAME = "base";
  protected static final String UPDATED_BASE_VAR_NAME = "updatedBase";

  protected static final String SUB_VAR_NAME = "sub";
  protected static final String UPDATED_SUB_VAR_NAME = "updatedSub";

  protected ParameterSpec baseParamSpec() {
    return baseParamSpec(false);
  }

  protected ParameterSpec baseParamSpec(boolean pathVariable) {
    var param = ParameterSpec.builder(entityModel.getJpaId().type(), baseIdParamName());
    if (pathVariable) {
      param.addAnnotation(PATH_VARIABLE);
    }
    return param.build();
  }

  protected ParameterSpec subParamSpec(boolean pathVariable) {
    var param = ParameterSpec.builder(targetEntityModel.getJpaId().type(), subIdParamName());
    if (pathVariable) {
      param.addAnnotation(PATH_VARIABLE);
    }
    return param.build();
  }

  protected String path;

  protected String uri() {
    return "/{" + baseIdParamName() + "}/" + path;
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

  protected TypeName view;

  protected String[] sort;

  protected ParameterSpec sortParamSpec() {
    var param = ParameterSpec.builder(SORT, "sort");
    if (sort != null && sort.length > 0) {
      param.addAnnotation(buildSortDefaultAnnotation(sort));
    }
    return param.build();
  }

  public static AnnotationSpec buildSortDefaultAnnotation(String[] sortValues) {
    var builder = AnnotationSpec.builder(SORT_DEFAULT);
    applySortValues(builder, sortValues);
    return builder.build();
  }

  public static void applySortValues(AnnotationSpec.Builder builder, String[] sortValues) {
    if (sortValues == null || sortValues.length == 0) {
      return;
    }
    if (sortValues.length == 1) {
      String val = sortValues[0].trim();
      if ("desc".equalsIgnoreCase(val)) {
        builder.addMember("sort", "$S", "desc");
        builder.addMember("direction", "$T.DESC", SORT_DIRECTION);
      } else if ("asc".equalsIgnoreCase(val)) {
        builder.addMember("sort", "$S", "asc");
        builder.addMember("direction", "$T.ASC", SORT_DIRECTION);
      } else if (val.contains(",")) {
        String[] parts = val.split(",", 2);
        builder.addMember("sort", "$S", parts[0].trim());
        if ("desc".equalsIgnoreCase(parts[1].trim())) {
          builder.addMember("direction", "$T.DESC", SORT_DIRECTION);
        } else if ("asc".equalsIgnoreCase(parts[1].trim())) {
          builder.addMember("direction", "$T.ASC", SORT_DIRECTION);
        }
      } else {
        builder.addMember("sort", "$S", val);
      }
    } else {
      String last = sortValues[sortValues.length - 1].trim();
      boolean lastIsDirection = "desc".equalsIgnoreCase(last) || "asc".equalsIgnoreCase(last);
      int propCount = lastIsDirection ? sortValues.length - 1 : sortValues.length;
      for (int i = 0; i < propCount; i++) {
        String prop = sortValues[i].trim();
        if (prop.contains(",")) {
          String[] parts = prop.split(",", 2);
          builder.addMember("sort", "$S", parts[0].trim());
        } else {
          builder.addMember("sort", "$S", prop);
        }
      }
      if (lastIsDirection) {
        if ("desc".equalsIgnoreCase(last)) {
          builder.addMember("direction", "$T.DESC", SORT_DIRECTION);
        } else {
          builder.addMember("direction", "$T.ASC", SORT_DIRECTION);
        }
      }
    }
  }

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

  protected AnnotationSpec responseStatus(HttpStatusValue status) {
    return AnnotationSpec.builder(RESPONSE_STATUS).addMember("code", "$L.$L", HTTP_STATUS, status.toString()).build();
  }

  /** The view the endpoint writes with: its own, else the controller's. */
  protected TypeName effectiveView() {
    if (view != null) return view;
    return entityModel != null ? entityModel.getRequirements().controllerView() : null;
  }

  /**
   * The body of a collection read: through {@code readAssociation}, which loads what the view writes in a fixed
   * number of queries, when there is a view and {@code attribute} is a real attribute of the entity; otherwise
   * through the getter, as before.
   */
  protected CodeBlock readStatement(String getter, boolean attribute) {
    var effective = effectiveView();
    if (effective == null || !attribute) {
      return CodeBlock.of("return sort(getById($L).$L(), sort)", baseIdParamName(), getter);
    }
    return CodeBlock.of(
        "return sort(service.<$T>readAssociation($L, $S, $T.class), sort)",
        targetEntityModel.getEntityType(), baseIdParamName(), getFieldName(), effective
    );
  }

  protected AnnotationSpec jsonViewAnnotation() {
    return AnnotationSpec.builder(JSON_VIEW).addMember("value", "$T.class", view).build();
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
        .addStatement("var $L = getById($L)", BASE_VAR_NAME, baseIdParamName());
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
      if (view != null) {
        method = method.toBuilder().addAnnotation(jsonViewAnnotation()).build();
      } else if (entityModel != null && entityModel.getRequirements().controllerView() != null) {
        method = method.toBuilder().addAnnotation(
            AnnotationSpec.builder(JSON_VIEW)
                .addMember("value", "$T.class", entityModel.getRequirements().controllerView())
                .build()
        ).build();
      }
      spec.addMethod(method);
    }

    var field = getControllerField();
    if (field != null && spec.fieldSpecs.stream().noneMatch(s -> s.type.equals(field.type))) {
      spec.addField(field);
    }
  }
}