package com.kivojenko.spring.forge.jpa.factory;

import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.jpa.model.FilterFieldModel;
import com.squareup.javapoet.TypeName;
import jakarta.persistence.Transient;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;

public class FilterFieldModelFactory {
  public static List<FilterFieldModel> resolve(TypeElement entity, ProcessingEnvironment env) {
    var filterFields = new ArrayList<FilterFieldModel>();
    if (entity == null)
      return filterFields;

    var fields = ElementFilter.fieldsIn(entity.getEnclosedElements());
    var typeUtils = env.getTypeUtils();
    var elementUtils = env.getElementUtils();
    var iterableElement = elementUtils.getTypeElement("java.lang.Iterable");
    var entityAnnotation = elementUtils.getTypeElement("jakarta.persistence.Entity");

    for (var field : fields) {
      var type = field.asType();
      boolean isIterable = typeUtils.isAssignable(typeUtils.erasure(field.asType()), iterableElement.asType());

      var annotation = field.getAnnotation(FilterField.class);

      if (annotation == null)
        continue;

      var isJavaTransient = field.getModifiers().contains(Modifier.TRANSIENT);
      var isJpaTransient = field.getAnnotation(Transient.class) != null;
      var isBeansTransient = field.getAnnotation(java.beans.Transient.class) != null;
      if ((isJavaTransient || isJpaTransient || isBeansTransient) && annotation.targetField().isEmpty()) {
        throw new IllegalStateException(
            "@FilterField is not allowed on transient field: " + field.getSimpleName() + " in "
                + entity.getQualifiedName());
      }

      var entityCandidate =
          isIterable && type instanceof DeclaredType declared && !declared.getTypeArguments().isEmpty() ?
          declared.getTypeArguments().getFirst() :
          type;

      var typeElement = typeUtils.asElement(entityCandidate);
      var singleEntity = !isIterable && typeElement != null && typeElement.getAnnotationMirrors()
          .stream()
          .anyMatch(a -> typeUtils.isSameType(a.getAnnotationType(), entityAnnotation.asType()));

      var targetField = annotation.targetField();
      var filterType = type;
      var filterTypeName = TypeName.get(type);
      var originalIterable = isIterable;
      var originalSingleEntity = singleEntity;

      if (!targetField.isEmpty() && (singleEntity || isIterable)) {
        filterType = resolveTargetFieldType(entityCandidate, targetField, env);
        filterTypeName = TypeName.get(filterType);
        isIterable = false;
        singleEntity = false;
      }

      filterFields.add(FilterFieldModel.builder()
                           .element(field)
                           .type(filterType)
                           .typeElement((TypeElement) typeElement)
                           .typeName(filterTypeName)
                           .annotation(annotation)
                           .iterable(isIterable)
                           .singleEntity(singleEntity)
                           .originalIterable(originalIterable)
                           .originalSingleEntity(originalSingleEntity)
                           .entityCandidate(entityCandidate)
                           .env(env)
                           .targetField(targetField)
                           .build());
    }

    return filterFields;
  }

  private static TypeMirror resolveTargetFieldType(TypeMirror startType, String path, ProcessingEnvironment env) {
    var currentType = startType;
    for (String part : path.split("\\.")) {
      var element = env.getTypeUtils().asElement(currentType);
      if (!(element instanceof TypeElement typeElement))
        return currentType;
      var field = typeElement.getEnclosedElements()
          .stream()
          .filter(e -> e.getKind() == ElementKind.FIELD && e.getSimpleName().toString().equals(part))
          .findFirst();
      if (field.isPresent()) {
        currentType = field.get().asType();
      } else {
        return currentType;
      }
    }
    return currentType;
  }
}
