package com.kivojenko.spring.forge.jpa.generator;


import com.kivojenko.spring.forge.jpa.model.base.JpaEntityModel;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;

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
        if (model.wantsFilter()) builder.addSuperinterface(queryDslPredicateExecutorOf(model));

        return builder.build();
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
