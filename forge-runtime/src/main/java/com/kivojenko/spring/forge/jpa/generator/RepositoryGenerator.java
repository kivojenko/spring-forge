package com.kivojenko.spring.forge.jpa.generator;


import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.HAS_NAME_REPOSITORY;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.JPA_REPOSITORY;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.LIST;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.QUERY_DSL_PREDICATE_EXECUTOR;

import com.kivojenko.spring.forge.jpa.model.FilterFieldModel;
import com.kivojenko.spring.forge.jpa.model.GetOrCreateModel;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

        // Add lookup methods for @GetOrCreate when not covered by HasNameRepository
        for (var cfg : GetOrCreateModel.resolve(model)) {
            if (model.getRequirements().hasName() && cfg.isNameOnly()) continue;
            var findMethod = MethodSpec.methodBuilder(cfg.findMethodName())
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), model.getEntityType()))
                    .addParameters(cfg.parameters())
                    .build();
            builder.addMethod(findMethod);
        }

        return builder.build();
    }

    private static void addFilterMethods(TypeSpec.Builder builder, JpaEntityModel model) {
        for (FilterFieldModel field : filterFinderFields(model)) {
            var method = MethodSpec.methodBuilder(filterFinderName(field))
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ParameterizedTypeName.get(LIST, model.getEntityType()))
                    .addParameter(field.getTypeName(), field.getName())
                    .build();
            builder.addMethod(method);
        }
    }

    /**
     * Returns the names of the {@code List}-returning finders generated for filter fields.
     *
     * @param model the entity model
     * @return finder method names, empty when the entity has no filter
     */
    public static Set<String> filterFinderNames(JpaEntityModel model) {
        if (!model.wantsFilter()) return Set.of();
        return filterFinderFields(model).stream().map(RepositoryGenerator::filterFinderName).collect(Collectors.toSet());
    }

    private static List<FilterFieldModel> filterFinderFields(JpaEntityModel model) {
        return model.getFilterableFields().stream()
                .filter(field -> !(field.isIterable() || field.isSingleEntity() || field.isDiscriminator() || field.isPresent()))
                .filter(field -> !(model.getRequirements().hasName() && field.getOriginalName().equals("name")))
                // Name the query after the property it actually reads (category.name → findByCategory_Name);
                // paths Spring Data cannot derive (collections, size()) are left to the QueryDSL predicate.
                .filter(field -> field.getDerivedQueryPath() != null)
                .toList();
    }

    private static String filterFinderName(FilterFieldModel field) {
        return "findBy" + field.getDerivedQueryPath();
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
