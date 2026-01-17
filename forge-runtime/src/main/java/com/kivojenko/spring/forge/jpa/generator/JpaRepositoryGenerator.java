package com.kivojenko.spring.forge.jpa.generator;


import com.kivojenko.spring.forge.jpa.model.model.JpaEntityModel;
import com.kivojenko.spring.forge.jpa.repository.HasNameRepository;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

/**
 * Generator for Spring Data JPA repositories.
 */
public final class JpaRepositoryGenerator {

    private static final ClassName JPA_REPOSITORY = ClassName.get(
            "org.springframework.data.jpa.repository",
            "JpaRepository"
    );
    private static final ClassName HAS_NAME_REPOSITORY = ClassName.get(HasNameRepository.class);

    /**
     * Generates a {@link JavaFile} containing the JPA repository for the given model.
     *
     * @param model the entity model
     * @return the generated Java file
     */
    public static JavaFile generateFile(JpaEntityModel model) {
        return JavaFile.builder(model.packages().repositoryPackageName(), generate(model)).build();
    }

    /**
     * Generates the {@link TypeSpec} for the JPA repository.
     *
     * @param model the entity model
     * @return the type specification
     */
    public static TypeSpec generate(JpaEntityModel model) {
        var builder = TypeSpec
                .interfaceBuilder(model.repositoryName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(jpaRepositoryOf(model));

        if (model.requirements().wantsAbstractRepository()) builder.addModifiers(Modifier.ABSTRACT);
        if (model.requirements().hasName()) builder.addSuperinterface(hasNameRepositoryOf(model));

        return builder.build();
    }

    private static ParameterizedTypeName jpaRepositoryOf(JpaEntityModel model) {
        return ParameterizedTypeName.get(JPA_REPOSITORY, model.entityType(), model.jpaId().type());
    }

    private static ParameterizedTypeName hasNameRepositoryOf(JpaEntityModel model) {
        return ParameterizedTypeName.get(HAS_NAME_REPOSITORY, model.entityType());
    }
}
