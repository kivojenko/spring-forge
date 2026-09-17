package com.kivojenko.spring.forge.jpa.factory;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.BOOLEAN_TYPES;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.DATE_TYPES;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.NUMERIC_TYPES;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.STRING;

import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.IterableMatchMode;
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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

public class FilterFieldModelFactory {
  private record DiscriminatorInfo(String name, DiscriminatorType type) {}

  /**
   * A {@code targetField} resolved against the type it is relative to.
   *
   * @param type          the type of the leaf field (the element type when the leaf is a collection)
   * @param queryPath     the QueryDSL path, with {@code .any()} inserted for intermediate collections
   * @param collectionLeaf whether the leaf field is a collection
   */
  private record TargetPath(TypeMirror type, String queryPath, boolean collectionLeaf) {}

  public static List<FilterFieldModel> resolve(TypeElement entity, ProcessingEnvironment env) {
    var filterFields = collect(entity, env, true);
    addDiscriminatorField(entity, filterFields, env);
    return filterFields;
  }

  /**
   * Resolve all filter field mappings including duplicates by exposed name (order preserved).
   * This is useful for generating fallback predicates where we want to try alternative mappings
   * when two or more fields share the same exposed filter name.
   *
   * <p>The discriminator mapping is not synthesized here to keep raw duplicates only; the primary
   * resolver adds it.
   */
  public static List<FilterFieldModel> resolveAll(TypeElement entity, ProcessingEnvironment env) {
    return collect(entity, env, false);
  }

  private static List<FilterFieldModel> collect(TypeElement entity, ProcessingEnvironment env, boolean uniqueByName) {
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
        var annotations = field.getAnnotationsByType(FilterField.class);
        if (annotations.length == 0)
          continue;

        for (var annotation : annotations) {
          if (uniqueByName) {
            // Skip if a field with the same exposed name was already added — we keep the first one for DTO uniqueness
            var fieldName = FilterFieldModel.exposedName(field.getSimpleName().toString(), annotation);
            if (filterFields.stream().anyMatch(f -> fieldName.equals(f.getName()))) {
              continue;
            }
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
          var elementIsEntity = typeElement != null && typeElement.getAnnotationMirrors()
              .stream()
              .anyMatch(a -> typeUtils.isSameType(a.getAnnotationType(), entityAnnotation.asType()));
          var singleEntity = !isIterable && elementIsEntity;
          // @Embedded value objects: targetField is relative to the embedded field (dye.colorIndex)
          var embedded = !isIterable && (field.getAnnotation(jakarta.persistence.Embedded.class) != null
              || typeElement != null && typeElement.getAnnotation(jakarta.persistence.Embeddable.class) != null);
          // @ElementCollection of scalars/enums: there is no id to filter by, so we match on the element itself
          var scalarElements = isIterable && !elementIsEntity && !typeUtils.isSameType(entityCandidate, type)
              && isFilterableScalar(entityCandidate, env);

          var targetField = annotation.targetField();
          var filterType = type;
          var filterTypeName = TypeName.get(type);
          var originalIterable = isIterable;
          var originalSingleEntity = singleEntity;
          var targetPath = targetField;
          var scalarCollection = false;

          TargetPath resolvedTarget = null;
          if (!targetField.isEmpty()) {
            // Relative to the association / embedded value, or absolute from the root entity otherwise
            var base = singleEntity || isIterable || embedded ? entityCandidate : entity.asType();
            resolvedTarget = resolveTargetPath(base, targetField, env);
            targetPath = resolvedTarget.queryPath();
            scalarCollection = resolvedTarget.collectionLeaf();
          }

          if (annotation.isPresent()) {
            filterTypeName = ClassName.get(Boolean.class);
            filterType = elementUtils.getTypeElement("java.lang.Boolean").asType();
            isIterable = false;
            singleEntity = false;
          } else if (annotation.iterableMatchMode() == IterableMatchMode.AMOUNT && isIterable) {
            filterTypeName = ClassName.get(Integer.class);
            filterType = elementUtils.getTypeElement("java.lang.Integer").asType();
            isIterable = false;
            singleEntity = false;
            scalarCollection = false;
          } else if (resolvedTarget != null && (singleEntity || isIterable || embedded)) {
            filterType = resolvedTarget.type();
            filterTypeName = TypeName.get(filterType);
            isIterable = false;
            singleEntity = false;
          } else if (targetField.isEmpty() && scalarElements) {
            // Collection of scalars/enums — filter on the element type through .any()
            filterType = entityCandidate;
            filterTypeName = TypeName.get(entityCandidate);
            isIterable = false;
            scalarCollection = true;
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
                               .originalEmbedded(embedded)
                               .entityCandidate(entityCandidate)
                               .env(env)
                               .targetField(targetField)
                               .targetPath(targetPath)
                               .scalarCollection(scalarCollection)
                               .required(annotation.required())
                               .orNull(annotation.orNull())
                               .present(annotation.isPresent())
                               .build());
        }
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

  /**
   * Walks a dotted {@code targetField} path from {@code startType}, resolving the leaf type and the
   * QueryDSL path to it. Collections met along the way are traversed with {@code .any()}; a collection
   * at the leaf is reported through {@link TargetPath#collectionLeaf()} so callers can decide between
   * {@code .any()} and {@code isEmpty()}.
   */
  private static TargetPath resolveTargetPath(TypeMirror startType, String path, ProcessingEnvironment env) {
    var typeUtils = env.getTypeUtils();
    var iterable = env.getElementUtils().getTypeElement("java.lang.Iterable");

    var currentType = startType;
    var resolved = new StringBuilder();
    var parts = path.split("\\.");

    for (int i = 0; i < parts.length; i++) {
      var element = typeUtils.asElement(currentType);
      if (!(element instanceof TypeElement typeElement)) {
        return new TargetPath(currentType, path, false);
      }

      var field = findFieldInHierarchy(typeElement, parts[i], env);
      if (field == null) {
        // If not found anywhere in the hierarchy, keep current type and path (best-effort, backward compatible)
        return new TargetPath(currentType, path, false);
      }

      currentType = field.asType();
      if (!resolved.isEmpty()) {
        resolved.append('.');
      }
      resolved.append(parts[i]);

      if (!typeUtils.isAssignable(typeUtils.erasure(currentType), iterable.asType())) {
        continue;
      }

      var elementType = elementTypeOf(currentType);
      if (typeUtils.isSameType(elementType, currentType)) {
        // Raw collection — we cannot say what it holds, so leave the path alone
        return new TargetPath(currentType, resolved.toString(), false);
      }
      currentType = elementType;
      if (i == parts.length - 1) {
        return new TargetPath(currentType, resolved.toString(), true);
      }
      resolved.append(".any()");
    }

    return new TargetPath(currentType, resolved.toString(), false);
  }

  /**
   * Whether a value of this type can be turned into a predicate on its own — i.e. it maps to one of the
   * branches of {@link FilterFieldModel#addFiltering}.
   */
  private static boolean isFilterableScalar(TypeMirror type, ProcessingEnvironment env) {
    var element = env.getTypeUtils().asElement(type);
    if (element != null && element.getKind() == ElementKind.ENUM) {
      return true;
    }
    var typeName = TypeName.get(type);
    return STRING.equals(typeName)
        || NUMERIC_TYPES.contains(typeName)
        || DATE_TYPES.contains(typeName)
        || BOOLEAN_TYPES.contains(typeName);
  }

  private static TypeMirror elementTypeOf(TypeMirror type) {
    return type instanceof DeclaredType declared && !declared.getTypeArguments().isEmpty()
           ? declared.getTypeArguments().getFirst()
           : type;
  }

  private static VariableElement findFieldInHierarchy(
      TypeElement typeElement, String fieldName, ProcessingEnvironment env) {
    TypeElement current = typeElement;
    while (current != null) {
      var match = current.getEnclosedElements()
          .stream()
          .filter(e -> e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(fieldName))
          .findFirst();
      if (match.isPresent()) {
        return (VariableElement) match.get();
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
