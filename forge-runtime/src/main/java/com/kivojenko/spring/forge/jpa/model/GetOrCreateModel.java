package com.kivojenko.spring.forge.jpa.model;

import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;

import com.kivojenko.spring.forge.annotation.GetOrCreate;
import com.kivojenko.spring.forge.jpa.generator.RepositoryGenerator;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolved configuration of a single {@link GetOrCreate} declaration.
 *
 * @param fields         key fields used for lookup and creation, in declaration order
 * @param path           endpoint path relative to the resource
 * @param suffix         method name suffix; empty when the entity declares a single get-or-create
 * @param findMethodName repository finder used for lookup, e.g. {@code findByBrandIgnoreCaseAndCountry_Code}
 */
public record GetOrCreateModel(
    List<KeyField> fields,
    String path,
    String suffix,
    String findMethodName
)
{
  /**
   * A single field of a get-or-create key.
   *
   * @param fieldPath  entity field (possibly nested, e.g. {@code country.code})
   * @param fieldType  type of the field
   * @param paramName  Java-safe parameter name (e.g. {@code countryCode})
   * @param ignoreCase whether the lookup on this field is case-insensitive
   */
  public record KeyField(String fieldPath, TypeName fieldType, String paramName, boolean ignoreCase) {
    /** @return the method parameter for this field */
    public ParameterSpec parameter() {
      return ParameterSpec.builder(fieldType, paramName).build();
    }

    private String propertyPath() {
      return toPropertyPathSuffix(fieldPath) + (ignoreCase ? "IgnoreCase" : "");
    }
  }

  /**
   * Resolves all {@link GetOrCreate} declarations of the given model.
   *
   * @param model the entity model
   * @return one entry per declaration, in declaration order
   * @throws IllegalStateException if a declaration has no or repeated fields, or two declarations share the same key
   */
  public static List<GetOrCreateModel> resolve(JpaEntityModel model) {
    var annotations = model.getRequirements().getOrCreateAnnotations();
    var multiple = annotations.size() > 1;
    var seen = new HashSet<java.util.Set<String>>();
    var filterFinders = RepositoryGenerator.filterFinderNames(model);
    var entityName = model.getElement().getSimpleName();

    return annotations.stream().map(annotation -> {
      var fieldPaths = Arrays.stream(annotation.fields()).map(String::trim).toList();
      if (fieldPaths.isEmpty() || fieldPaths.stream().anyMatch(String::isEmpty)) {
        throw new IllegalStateException("@GetOrCreate on " + entityName + " must name at least one non-blank field");
      }
      var key = new LinkedHashSet<>(fieldPaths);
      if (key.size() != fieldPaths.size()) {
        throw new IllegalStateException("@GetOrCreate on " + entityName + " repeats a field: " + fieldPaths);
      }
      if (!seen.add(key)) {
        throw new IllegalStateException("Duplicate @GetOrCreate for fields " + fieldPaths + " on " + entityName);
      }

      var fields = fieldPaths.stream().map(fieldPath -> {
        var fieldType = model.resolveFieldTypeName(fieldPath);
        var ignoreCase = fieldType.equals(ClassName.get(String.class)) && annotation.ignoreCase();
        return new KeyField(fieldPath, fieldType, toSafeParamName(fieldPath), ignoreCase);
      }).toList();

      var path = annotation.path().isEmpty() ? "/get-or-create" : annotation.path();
      var suffix = multiple
          ? "By" + fields.stream().map(f -> capitalize(f.paramName())).collect(Collectors.joining("And"))
          : "";
      var propertyPath = fields.stream().map(KeyField::propertyPath).collect(Collectors.joining("And"));
      // A filter field may already own findBy<Field> (returning a List); fall back to the equivalent findOneBy<Field>.
      var findMethodName = filterFinders.contains("findBy" + propertyPath) ? "findOneBy" + propertyPath : "findBy" + propertyPath;
      return new GetOrCreateModel(fields, path, suffix, findMethodName);
    }).toList();
  }

  /** @return whether the key is exactly the {@code name} field */
  public boolean isNameOnly() {
    return fields.size() == 1 && fields.getFirst().fieldPath().equals("name");
  }

  /** @return the method parameters, one per key field */
  public List<ParameterSpec> parameters() {
    return fields.stream().map(KeyField::parameter).toList();
  }

  /** @return the parameter names joined for a method call, e.g. {@code brand, model} */
  public String arguments() {
    return fields.stream().map(KeyField::paramName).collect(Collectors.joining(", "));
  }

  /** @return the field paths joined for documentation, e.g. {@code brand and model} */
  public String description() {
    return fields.stream().map(KeyField::fieldPath).collect(Collectors.joining(" and "));
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
