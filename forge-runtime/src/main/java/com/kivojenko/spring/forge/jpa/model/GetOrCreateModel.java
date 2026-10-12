package com.kivojenko.spring.forge.jpa.model;

import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.jpa.generator.RepositoryGenerator;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.HashSet;
import java.util.List;

/**
 * Resolved configuration of a single {@link GetOrCreate} declaration.
 *
 * @param fieldPath  entity field (possibly nested, e.g. {@code country.code}) used for lookup and creation
 * @param fieldType  type of the field
 * @param paramName  Java-safe parameter name (e.g. {@code countryCode})
 * @param ignoreCase whether the lookup is case-insensitive
 * @param path       endpoint path relative to the resource
 * @param suffix     method name suffix; empty when the entity declares a single get-or-create
 * @param findMethodName repository finder used for lookup, e.g. {@code findByCountry_Code}
 */
public record GetOrCreateModel(
    String fieldPath,
    TypeName fieldType,
    String paramName,
    boolean ignoreCase,
    String path,
    String suffix,
    String findMethodName
)
{
  /**
   * Resolves all {@link GetOrCreate} declarations of the given model.
   *
   * @param model the entity model
   * @return one entry per declaration, in declaration order
   * @throws IllegalStateException if two declarations target the same field
   */
  public static List<GetOrCreateModel> resolve(JpaEntityModel model) {
    var annotations = model.getRequirements().getOrCreateAnnotations();
    var multiple = annotations.size() > 1;
    var seen = new HashSet<String>();
    var filterFinders = RepositoryGenerator.filterFinderNames(model);

    return annotations.stream().map(annotation -> {
      var fieldPath = annotation.field().isEmpty() ? "name" : annotation.field();
      if (!seen.add(fieldPath)) {
        throw new IllegalStateException(
            "Duplicate @GetOrCreate for field '" + fieldPath + "' on " + model.getElement().getSimpleName());
      }
      var fieldType = model.resolveFieldTypeName(fieldPath);
      var ignoreCase = fieldType.equals(ClassName.get(String.class)) && annotation.ignoreCase();
      var path = annotation.path().isEmpty() ? "/get-or-create" : annotation.path();
      var suffix = multiple ? "By" + capitalize(toSafeParamName(fieldPath)) : "";
      var propertyPath = toPropertyPathSuffix(fieldPath) + (ignoreCase ? "IgnoreCase" : "");
      // A filter field may already own findBy<Field> (returning a List); fall back to the equivalent findOneBy<Field>.
      var findMethodName = filterFinders.contains("findBy" + propertyPath) ? "findOneBy" + propertyPath : "findBy" + propertyPath;
      return new GetOrCreateModel(fieldPath, fieldType, toSafeParamName(fieldPath), ignoreCase, path, suffix, findMethodName);
    }).toList();
  }

  /** @return {@code getOrCreate} plus the suffix */
  public String getOrCreateMethodName() {
    return "getOrCreate" + suffix;
  }

  /** @return {@code create} plus the suffix */
  public String createMethodName() {
    return "create" + suffix;
  }

  /** @return {@code createSafely} plus the suffix */
  public String createSafelyMethodName() {
    return "createSafely" + suffix;
  }

  private static String toPropertyPathSuffix(String path) {
    var parts = path.split("\\.");
    var sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) sb.append('_');
      sb.append(capitalize(parts[i]));
    }
    return sb.toString();
  }

  private static String toSafeParamName(String path) {
    var parts = path.split("\\.");
    var sb = new StringBuilder(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      sb.append(capitalize(parts[i]));
    }
    return sb.toString();
  }
}
