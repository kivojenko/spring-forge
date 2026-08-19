package com.kivojenko.spring.forge.jpa.generator;

import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;

/**
 * Generator for Spring services.
 */
public final class ServiceGenerator {


  /**
   * Generates a {@link JavaFile} containing the service for the given model.
   *
   * @param model the entity model
   * @return the generated Java file
   */
  public static JavaFile generateFile(JpaEntityModel model) {
    return JavaFile.builder(model.getPackages().servicePackageName(), generate(model)).build();
  }

  /**
   * Generates the {@link TypeSpec} for the service.
   *
   * @param model the entity model
   * @return the type specification
   */
  public static TypeSpec generate(JpaEntityModel model) {
    var superClass = ParameterizedTypeName.get(
        FORGE_SERVICE,
        model.getEntityType(),
        model.getJpaId().type(),
        model.getRepositoryType()
    );
    var builder = TypeSpec
        .classBuilder(model.getServiceName())
        .addModifiers(Modifier.PUBLIC)
        .superclass(superClass)
        .addMethod(model.setIdMethod())
        .addMethod(MethodSpec.methodBuilder("getEntityClass")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(ClassName.get(Class.class), model.getEntityType()))
            .addStatement("return $T.class", model.getEntityType())
            .build());

    if (model.getRequirements().wantsAbstractService()) {
      builder.addModifiers(Modifier.ABSTRACT);
    } else {
      builder.addAnnotation(SERVICE);
    }

    if (model.getRequirements().hasName()) {
      var create = MethodSpec
          .methodBuilder("create")
          .addJavadoc("Creates a new {@link $T} entity after checking that its name is unique.\n", model.getEntityType())
          .addJavadoc("@param entity the entity to create\n")
          .addJavadoc("@return the created entity\n")
          .addJavadoc("@throws IllegalArgumentException if an entity with the same name already exists\n")
          .addAnnotation(TRANSACTIONAL)
          .addModifiers(Modifier.PUBLIC)
          .returns(model.getEntityType())
          .addParameter(model.getEntityType(), "entity")
          .beginControlFlow("if (repository.existsByName(entity.getName()))")
          .addStatement(
              "throw new $T($S + entity.getName() + $S)",
              IllegalArgumentException.class,
              "Entity with name ",
              " already exists"
          )
          .endControlFlow()
          .addStatement("return super.create(entity)")
          .build();
      builder.addMethod(create);
    }

    // Generate getOrCreate by configured field (defaults to "name"). Supports nested paths like "country.code".
    if (model.getRequirements().getOrCreateAnnotation() != null) {
      var annotation = model.getRequirements().getOrCreateAnnotation();
      var fieldPath = annotation.field().isEmpty() ? "name" : annotation.field();
      var fieldType = model.resolveFieldTypeName(fieldPath);
      boolean isString = fieldType.equals(ClassName.get(String.class));
      boolean ignoreCase = isString && annotation.ignoreCase();
      var suffix = toPropertyPathSuffix(fieldPath);
      var paramName = toSafeParamName(fieldPath);
      var findMethod = "findBy" + suffix + (ignoreCase ? "IgnoreCase" : "");

      var getOrCreate = MethodSpec
          .methodBuilder("getOrCreate")
          .addJavadoc("Retrieves an existing {@link $T} by $L or creates it if it does not exist.\n", model.getEntityType(), fieldPath)
          .addJavadoc("@param $L the $L of the entity\n", paramName, fieldPath)
          .addJavadoc("@return the retrieved or newly created entity\n")
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(TRANSACTIONAL)
          .returns(model.getEntityType())
          .addParameter(fieldType, paramName)
          .addStatement("return repository.$L($L).orElseGet(() -> createSafely($L))", findMethod, paramName, paramName)
          .build();

      MethodSpec createSafely = MethodSpec
          .methodBuilder("createSafely")
          .addJavadoc("Attempts to create an entity with the given $L, handling race conditions where another thread might have created it.\n", fieldPath)
          .addJavadoc("@param $L the $L of the entity\n", paramName, fieldPath)
          .addJavadoc("@return the retrieved or newly created entity\n")
          .addModifiers(Modifier.PROTECTED)
          .returns(model.getEntityType())
          .addParameter(fieldType, paramName)
          .beginControlFlow("try")
          .addStatement("return repository.save(create($L))", paramName)
          .nextControlFlow("catch ($T e)", DATA_INTEGRITY_VIOLATION_EXCEPTION)
          .addStatement("return repository.$L($L).orElseThrow()", findMethod, paramName)
          .endControlFlow()
          .build();

      builder.addMethod(model.createMethodForField(fieldPath)).addMethod(getOrCreate).addMethod(createSafely);
    }


    if (model.wantsFilter()) {
      var pageableParam = ParameterSpec.builder(PAGEABLE, "pageable").build();
      var filterParam = ParameterSpec.builder(model.getFilterType(), "filter").build();
      var findAllPagedFiltered = MethodSpec
          .methodBuilder("findAll")
          .addJavadoc("Retrieves a paged result of {@link $T} entities matching the filter criteria.\n", model.getEntityType())
          .addJavadoc("@param pageable the pagination information\n")
          .addJavadoc("@param filter the filter criteria\n")
          .addJavadoc("@return a page of entities matching the filter\n")
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(PAGE, model.getEntityType()))
          .addParameter(pageableParam)
          .addParameter(filterParam)
          .addStatement("return repository.findAll(filter.toPredicate(), pageable)")
          .build();


      var findAllFiltered = MethodSpec
          .methodBuilder("findAll")
          .addJavadoc("Retrieves all {@link $T} entities matching the filter criteria.\n", model.getEntityType())
          .addJavadoc("@param filter the filter criteria\n")
          .addJavadoc("@return a list of entities matching the filter\n")
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(ARRAY_LIST, model.getEntityType()))
          .addParameter(filterParam)
          .addStatement("var result = repository.findAll(filter.toPredicate())")
          .addStatement(
              "return $T.stream(result.spliterator(), false).collect($T.toCollection($T::new))",
              ClassName.get(StreamSupport.class),
              ClassName.get(Collectors.class),
              ARRAY_LIST
          )
          .build();

      builder.addMethod(findAllPagedFiltered).addMethod(findAllFiltered);
    }

    model.getEndpointRelations().forEach(r -> r.addMethod(builder));
    return builder.build();
  }

  private static String toPropertyPathSuffix(String path) {
    if (path.indexOf('.') < 0) return capitalize(path);
    var parts = path.split("\\.");
    var sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) sb.append('_');
      sb.append(capitalize(parts[i]));
    }
    return sb.toString();
  }

  private static String toSafeParamName(String path) {
    if (path.indexOf('.') < 0) return path;
    var parts = path.split("\\.");
    var sb = new StringBuilder(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      sb.append(capitalize(parts[i]));
    }
    return sb.toString();
  }
}
