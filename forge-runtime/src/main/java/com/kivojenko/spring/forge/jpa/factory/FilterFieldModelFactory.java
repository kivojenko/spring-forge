package com.kivojenko.spring.forge.jpa.factory;

import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.jpa.model.FilterFieldModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

public class FilterFieldModelFactory {
  private record DiscriminatorInfo(String name, DiscriminatorType type) {}

  public static List<FilterFieldModel> resolve(TypeElement entity, ProcessingEnvironment env) {
    var filterFields = new ArrayList<FilterFieldModel>();
    if (entity == null)
      return filterFields;

    var typeUtils = env.getTypeUtils();
    var elementUtils = env.getElementUtils();
    var iterableElement = elementUtils.getTypeElement("java.lang.Iterable");
    var entityAnnotation = elementUtils.getTypeElement("jakarta.persistence.Entity");

    TypeElement current = entity;
    while (current != null) {
      var fields = ElementFilter.fieldsIn(current.getEnclosedElements());

      for (var field : fields) {
        var annotation = field.getAnnotation(FilterField.class);

        if (annotation == null)
          continue;

        // Skip if a field with the same exposed name was already added — we keep the first one for DTO uniqueness
        var fieldName = annotation.name().isEmpty() ? field.getSimpleName().toString() : annotation.name();
        if (filterFields.stream().anyMatch(f -> fieldName.equals(f.getName()))) {
          continue;
        }

        var type = field.asType();
        boolean isIterable = typeUtils.isAssignable(typeUtils.erasure(field.asType()), iterableElement.asType());

        var isJavaTransient = field.getModifiers().contains(Modifier.TRANSIENT);
        var isJpaTransient = field.getAnnotation(Transient.class) != null;
        var isBeansTransient = field.getAnnotation(java.beans.Transient.class) != null;
        if ((isJavaTransient || isJpaTransient || isBeansTransient) && annotation.targetField().isEmpty()) {
          throw new IllegalStateException(
              "@FilterField is not allowed on transient field: " + field.getSimpleName() + " in "
                  + current.getQualifiedName());
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
                             .required(annotation.required())
                             .orNull(annotation.orNull())
                             .build());
      }

      TypeMirror superclass = current.getSuperclass();
      if (superclass.getKind() == TypeKind.DECLARED) {
        current = (TypeElement) typeUtils.asElement(superclass);
        if (current.getAnnotation(Entity.class) == null && current.getAnnotation(MappedSuperclass.class) == null) {
          current = null;
        }
      } else {
        current = null;
      }
    }

    addDiscriminatorField(entity, filterFields, env);

    return filterFields;
  }

  /**
   * Resolve all filter field mappings including duplicates by exposed name (order preserved).
   * This is useful for generating fallback predicates where we want to try alternative mappings
   * when two or more fields share the same exposed filter name.
   */
  public static List<FilterFieldModel> resolveAll(TypeElement entity, ProcessingEnvironment env) {
    var filterFields = new ArrayList<FilterFieldModel>();
    if (entity == null)
      return filterFields;

    var typeUtils = env.getTypeUtils();
    var elementUtils = env.getElementUtils();
    var iterableElement = elementUtils.getTypeElement("java.lang.Iterable");
    var entityAnnotation = elementUtils.getTypeElement("jakarta.persistence.Entity");

    TypeElement current = entity;
    while (current != null) {
      var fields = ElementFilter.fieldsIn(current.getEnclosedElements());

      for (var field : fields) {
        var annotation = field.getAnnotation(FilterField.class);

        if (annotation == null)
          continue;

        var type = field.asType();
        boolean isIterable = typeUtils.isAssignable(typeUtils.erasure(field.asType()), iterableElement.asType());

        var isJavaTransient = field.getModifiers().contains(Modifier.TRANSIENT);
        var isJpaTransient = field.getAnnotation(Transient.class) != null;
        var isBeansTransient = field.getAnnotation(java.beans.Transient.class) != null;
        if ((isJavaTransient || isJpaTransient || isBeansTransient) && annotation.targetField().isEmpty()) {
          throw new IllegalStateException(
              "@FilterField is not allowed on transient field: " + field.getSimpleName() + " in "
                  + current.getQualifiedName());
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
                             .required(annotation.required())
                             .orNull(annotation.orNull())
                             .build());
      }

      TypeMirror superclass = current.getSuperclass();
      if (superclass.getKind() == TypeKind.DECLARED) {
        current = (TypeElement) typeUtils.asElement(superclass);
        if (current.getAnnotation(Entity.class) == null && current.getAnnotation(MappedSuperclass.class) == null) {
          current = null;
        }
      } else {
        current = null;
      }
    }

    // Do not synthesize discriminator mapping here to keep raw duplicates only; the primary resolver will add it.
    return filterFields;
  }

  private static void addDiscriminatorField(TypeElement entity, List<FilterFieldModel> filterFields, ProcessingEnvironment env) {
    var info = getDiscriminatorInfo(entity);
    if (info == null) {
      return;
    }

    var name = com.kivojenko.spring.forge.jpa.utils.StringUtils.decapitalize(
        com.kivojenko.spring.forge.jpa.utils.StringUtils.toCamelCase(info.name));

    boolean alreadyHas = filterFields.stream().anyMatch(f -> name.equals(f.getName()));
    if (alreadyHas) {
      return;
    }

    TypeMirror itemType;
    TypeName itemTypeName;
    switch (info.type) {
      case INTEGER -> {
        itemType = env.getElementUtils().getTypeElement("java.lang.Integer").asType();
        itemTypeName = TypeName.get(Integer.class);
      }
      case CHAR -> {
        itemType = env.getElementUtils().getTypeElement("java.lang.Character").asType();
        itemTypeName = TypeName.get(Character.class);
      }
      default -> {
        itemType = env.getElementUtils().getTypeElement("java.lang.String").asType();
        itemTypeName = TypeName.get(String.class);
      }
    }

    var listType = env.getTypeUtils().getDeclaredType(env.getElementUtils().getTypeElement("java.util.List"), itemType);
    filterFields.add(FilterFieldModel.builder()
                         .name(name)
                         .targetFieldName(name)
                         .discriminator(true)
                         .discriminatorType(info.type)
                         .discriminatorMapping(resolveDiscriminatorMapping(entity))
                         .type(listType)
                         .typeName(ParameterizedTypeName.get(ClassName.get(List.class), itemTypeName))
                         .annotation(createDefaultFilterField())
                         .env(env)
                         .build());
  }

  private static Map<String, ClassName> resolveDiscriminatorMapping(TypeElement entity) {
    var mapping = new HashMap<String, ClassName>();
    var roundEnv = JpaEntityModelFactory.getRoundEnv();
    if (roundEnv == null) {
      return mapping;
    }

    for (var element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
      if (element instanceof TypeElement typeElement && isSubclassOf(typeElement, entity)) {
        var dv = typeElement.getAnnotation(DiscriminatorValue.class);
        var value = dv != null ? dv.value() : typeElement.getSimpleName().toString();
        mapping.put(value, ClassName.get(typeElement));
      }
    }
    // Also include the base class itself if it's not abstract
    if (!entity.getModifiers().contains(Modifier.ABSTRACT)) {
      var dv = entity.getAnnotation(DiscriminatorValue.class);
      var value = dv != null ? dv.value() : entity.getSimpleName().toString();
      mapping.put(value, ClassName.get(entity));
    }
    return mapping;
  }

  private static boolean isSubclassOf(TypeElement child, TypeElement parent) {
    var current = child.getSuperclass();
    while (current.getKind() == TypeKind.DECLARED) {
      var element = (TypeElement) ((DeclaredType) current).asElement();
      if (element.equals(parent)) {
        return true;
      }
      current = element.getSuperclass();
    }
    return false;
  }

  private static FilterField createDefaultFilterField() {
    return (FilterField) Proxy.newProxyInstance(
        FilterField.class.getClassLoader(),
        new Class[] {FilterField.class},
        (proxy, method, args) -> method.getDefaultValue());
  }

  private static DiscriminatorInfo getDiscriminatorInfo(TypeElement entity) {
    TypeElement current = entity;
    while (current != null) {
      if (current.getAnnotation(Inheritance.class) != null) {
        var dc = current.getAnnotation(DiscriminatorColumn.class);
        if (dc != null) {
          return new DiscriminatorInfo(dc.name(), dc.discriminatorType());
        }
        return new DiscriminatorInfo("DTYPE", DiscriminatorType.STRING);
      }
      var superType = current.getSuperclass();
      if (superType.getKind() == TypeKind.DECLARED) {
        current = (TypeElement) ((DeclaredType) superType).asElement();
      } else {
        current = null;
      }
    }
    return null;
  }

  private static TypeMirror resolveTargetFieldType(TypeMirror startType, String path, ProcessingEnvironment env) {
    var currentType = startType;
    for (String part : path.split("\\.")) {
      var element = env.getTypeUtils().asElement(currentType);
      if (!(element instanceof TypeElement typeElement)) {
        return currentType;
      }

      var field = findFieldInHierarchy(typeElement, part, env);
      if (field != null) {
        currentType = field.asType();
      } else {
        // If not found anywhere in the hierarchy, keep current type (best-effort, backward compatible)
        return currentType;
      }
    }
    return currentType;
  }

  private static javax.lang.model.element.VariableElement findFieldInHierarchy(
      TypeElement typeElement, String fieldName, ProcessingEnvironment env) {
    TypeElement current = typeElement;
    while (current != null) {
      var match = current.getEnclosedElements()
          .stream()
          .filter(e -> e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(fieldName))
          .findFirst();
      if (match.isPresent()) {
        return (javax.lang.model.element.VariableElement) match.get();
      }
      var superType = current.getSuperclass();
      if (superType.getKind() == TypeKind.DECLARED) {
        current = (TypeElement) ((DeclaredType) superType).asElement();
      } else {
        current = null;
      }
    }
    return null;
  }
}
