package com.kivojenko.spring.forge.jpa.generator;


import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.HAS_NAME_REPOSITORY;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.JPA_REPOSITORY;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.LIST;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.QUERY_DSL_PREDICATE_EXECUTOR;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;

import com.kivojenko.spring.forge.jpa.model.FilterFieldModel;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

/**
 * Generator for Spring Data JPA repositories.
 */
public final class RepositoryGenerator {
    /**
     * Generates a {@link JavaFile} containing the JPA repository for the given model.
     *
     * @param model the entity model
     * @return the generated Java file
     */
    public static JavaFile generateFile(JpaEntityModel model) {
        return JavaFile.builder(model.getPackages().repositoryPackageName(), generate(model)).build();
    }

    /**
     * Generates the {@link TypeSpec} for the JPA repository.
     *
     * @param model the entity model
     * @return the type specification
     */
    public static TypeSpec generate(JpaEntityModel model) {
        var builder = TypeSpec
                .interfaceBuilder(model.getRepositoryName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(jpaRepositoryOf(model));

        if (model.getRequirements().wantsAbstractRepository()) builder.addModifiers(Modifier.ABSTRACT);
        if (model.getRequirements().hasName()) builder.addSuperinterface(hasNameRepositoryOf(model));
        for (var repositoryInterface : model.getRequirements().repositoryInterfaces()) {
            builder.addSuperinterface(repositoryInterface);
        }
        if (model.wantsFilter()) {
            builder.addSuperinterface(queryDslPredicateExecutorOf(model));
            addFilterMethods(builder, model);
        }

        // Add minimal query support for @GetOrCreate when not covered by HasNameRepository
        if (model.getRequirements().getOrCreateAnnotation() != null) {
            var cfg = model.getRequirements().getOrCreateAnnotation();
            var fieldPath = cfg.field().isEmpty() ? "name" : cfg.field();
            var needCustom = !(model.getRequirements().hasName() && fieldPath.equals("name"));
            if (needCustom) {
                var fieldType = model.resolveFieldTypeName(fieldPath);
                boolean isString = fieldType.equals(com.squareup.javapoet.ClassName.get(String.class));
                boolean ignoreCase = isString && cfg.ignoreCase();
                var optEntity = ParameterizedTypeName.get(
                        com.squareup.javapoet.ClassName.get(java.util.Optional.class),
                        model.getEntityType()
                );

                var suffix = toPropertyPathSuffix(fieldPath); // e.g., Country_Code for country.code
                var paramName = toSafeParamName(fieldPath);   // e.g., countryCode
                var findMethodName = "findBy" + suffix + (ignoreCase ? "IgnoreCase" : "");
                var findMethod = MethodSpec.methodBuilder(findMethodName)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(optEntity)
                        .addParameter(fieldType, paramName)
                        .build();
                builder.addMethod(findMethod);
            }
        }

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

    private static void addFilterMethods(TypeSpec.Builder builder, JpaEntityModel model) {
        for (FilterFieldModel field : model.getFilterableFields()) {
            if (field.isIterable() || field.isSingleEntity() || field.isDiscriminator()) {
                continue;
            }
            if (model.getRequirements().hasName() && field.getOriginalName().equals("name")) {
                continue;
            }

            var methodName = "findBy" + capitalize(field.getOriginalName());
            var method = MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ParameterizedTypeName.get(LIST, model.getEntityType()))
                    .addParameter(field.getTypeName(), field.getName())
                    .build();
            builder.addMethod(method);
        }
    }

    private static ParameterizedTypeName jpaRepositoryOf(JpaEntityModel model) {
        return ParameterizedTypeName.get(JPA_REPOSITORY, model.getEntityType(), model.getJpaId().type());
    }

    private static ParameterizedTypeName hasNameRepositoryOf(JpaEntityModel model) {
        return ParameterizedTypeName.get(HAS_NAME_REPOSITORY, model.getEntityType());
    }

    private static ParameterizedTypeName queryDslPredicateExecutorOf(JpaEntityModel model) {
        return ParameterizedTypeName.get(QUERY_DSL_PREDICATE_EXECUTOR, model.getEntityType());
    }
}
