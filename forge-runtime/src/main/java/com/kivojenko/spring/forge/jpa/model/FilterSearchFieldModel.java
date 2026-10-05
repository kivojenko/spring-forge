package com.kivojenko.spring.forge.jpa.model;

import com.kivojenko.spring.forge.annotation.filter.FilterSearchField;
import com.kivojenko.spring.forge.annotation.filter.StringMatchMode;
import lombok.Builder;
import lombok.Data;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

@Data
@Builder
public class FilterSearchFieldModel {
  VariableElement element;
  TypeElement typeElement;
  FilterSearchField annotation;
  String name;
  StringMatchMode stringMatchMode;
  String targetField;
  String targetPath;
  String targetFieldName;
  boolean originalIterable;
  boolean originalSingleEntity;
  boolean originalEmbedded;
  boolean scalarCollection;

  public String getName() {
    if (name != null && !name.isEmpty()) {
      return name;
    }
    if (annotation != null && !annotation.name().isEmpty()) {
      return annotation.name();
    }
    return "search";
  }

  public String getTargetFieldName() {
    if (targetFieldName != null) {
      return targetFieldName;
    }
    String fieldName = element != null ? element.getSimpleName().toString() : "";
    if (scalarCollection && (targetField == null || targetField.isEmpty())) {
      return fieldName + ".any()";
    }
    return resolvedPath(fieldName);
  }

  private String resolvedPath(String fieldName) {
    if (targetField == null || targetField.isEmpty()) {
      return fieldName;
    }
    var target = targetPath == null || targetPath.isEmpty() ? targetField : targetPath;
    if (originalIterable) {
      return fieldName + ".any()." + target;
    }
    if (originalSingleEntity || originalEmbedded) {
      return fieldName + "." + target;
    }
    return target;
  }
}
