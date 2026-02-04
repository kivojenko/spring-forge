package com.kivojenko.spring.forge.jpa.factory;

import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.IterableFilterField;
import com.kivojenko.spring.forge.jpa.model.FilterFieldModel;
import com.kivojenko.spring.forge.jpa.utils.LoggingUtils;
import com.squareup.javapoet.TypeName;
import jakarta.persistence.Transient;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;


public class FilterFieldModelFactory {
  public static List<FilterFieldModel> resolve(TypeElement entity, ProcessingEnvironment env) {
    var filterFields = new ArrayList<FilterFieldModel>();
    if (entity == null) return filterFields;

    var fields = ElementFilter.fieldsIn(entity.getEnclosedElements());
    var typeUtils = env.getTypeUtils();
    var elementUtils = env.getElementUtils();
    var iterableElement = elementUtils.getTypeElement("java.lang.Iterable");
    var entityAnnotation = elementUtils.getTypeElement("jakarta.persistence.Entity");

    for (var field : fields) {
      var type = field.asType();
      boolean isIterable = typeUtils.isAssignable(typeUtils.erasure(field.asType()), iterableElement.asType());

      Annotation annotation = field.getAnnotation(FilterField.class);
      if (annotation == null) {
        annotation = field.getAnnotation(IterableFilterField.class);

        if (annotation != null && !isIterable) {
          LoggingUtils.error(env, field, "@FilterIterableField is only allowed on Iterable types");
        }
      }
      if (annotation == null) continue;

      var isJavaTransient = field.getModifiers().contains(Modifier.TRANSIENT);
      var isJpaTransient = field.getAnnotation(Transient.class) != null;
      var isBeansTransient = field.getAnnotation(java.beans.Transient.class) != null;
      if (isJavaTransient || isJpaTransient || isBeansTransient) {
        throw new IllegalStateException("@FilterField is not allowed on transient field: " +
            field.getSimpleName() +
            " in " +
            entity.getQualifiedName());
      }


      var entityCandidate = isIterable &&
          type instanceof DeclaredType declared &&
          !declared.getTypeArguments().isEmpty() ? declared.getTypeArguments().getFirst() : type;

      var typeElement = typeUtils.asElement(entityCandidate);
      var singleEntity = !isIterable &&
          typeElement != null &&
          typeElement
              .getAnnotationMirrors()
              .stream()
              .anyMatch(a -> typeUtils.isSameType(a.getAnnotationType(), entityAnnotation.asType()));

      filterFields.add(FilterFieldModel
          .builder()
          .element(field)
          .type(type)
          .typeElement((TypeElement) typeElement)
          .typeName(TypeName.get(type))
          .annotation(annotation)
          .iterable(isIterable)
          .singleEntity(singleEntity)
          .entityCandidate(entityCandidate)
          .env(env)
          .build());
    }

    return filterFields;
  }
}
