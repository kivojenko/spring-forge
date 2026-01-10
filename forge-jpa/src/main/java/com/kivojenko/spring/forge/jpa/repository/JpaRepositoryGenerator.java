package com.kivojenko.spring.forge.jpa.repository;


import com.kivojenko.spring.forge.jpa.model.JpaEntityModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

public final class JpaRepositoryGenerator {

    private static final ClassName JPA_REPOSITORY = ClassName.get("org.springframework.data.jpa.repository", "JpaRepository");
    private static final ClassName HAS_NAME_REPOSITORY = ClassName.get(HasNameRepository.class);

    public static JavaFile generateFile(JpaEntityModel model) {
        return JavaFile.builder(model.repositoryPackageName(), generate(model)).build();
    }

    public static TypeSpec generate(JpaEntityModel model) {
        var builder = TypeSpec.interfaceBuilder(model.repositoryName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(jpaRepositoryOf(model));

        if (model.hasName()) {
            builder.addSuperinterface(hasNameRepositoryOf(model));
        }

        return builder.build();
    }

    private static ParameterizedTypeName jpaRepositoryOf(JpaEntityModel model) {
        return ParameterizedTypeName.get(JPA_REPOSITORY, model.entityType(), model.jpaId().type());
    }

    private static ParameterizedTypeName hasNameRepositoryOf(JpaEntityModel model) {
        return ParameterizedTypeName.get(HAS_NAME_REPOSITORY, model.entityType());
    }
}
