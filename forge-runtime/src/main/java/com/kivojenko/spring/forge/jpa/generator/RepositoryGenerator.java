package com.kivojenko.spring.forge.jpa.generator;


import com.kivojenko.spring.forge.jpa.model.FilterFieldModel;
import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.kivojenko.spring.forge.jpa.utils.StringUtils;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize;

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
        if (model.wantsFilter()) {
            builder.addSuperinterface(queryDslPredicateExecutorOf(model));
            addFilterMethods(builder, model);
        }

        return builder.build();
    }

    private static void addFilterMethods(TypeSpec.Builder builder, JpaEntityModel model) {
        for (FilterFieldModel field : model.getFilterableFields()) {
            if (field.isIterable() || field.isSingleEntity()) {
                continue;
            }

            var methodName = "findBy" + capitalize(field.getName());
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
