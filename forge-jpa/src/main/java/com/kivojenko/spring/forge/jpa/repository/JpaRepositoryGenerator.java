package com.kivojenko.spring.forge.jpa.repository;


import com.kivojenko.spring.forge.jpa.JpaEntityModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

public final class JpaRepositoryGenerator {

    private static final ClassName JPA_REPOSITORY =
            ClassName.get("org.springframework.data.jpa.repository", "JpaRepository");
    private static final ClassName HAS_FIND_BY_NAME = ClassName.get(HasNameRepository.class);


    public static TypeSpec generate(JpaEntityModel model) {
        var builder = TypeSpec.interfaceBuilder(model.repositoryName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(jpaRepositoryOf(model));

        if (model.hasName()) {
            builder.addSuperinterface(ParameterizedTypeName.get(HAS_FIND_BY_NAME, model.entityType()));
        }

        return builder.build();
    }

    private static ParameterizedTypeName jpaRepositoryOf(JpaEntityModel model) {
        return ParameterizedTypeName.get(JPA_REPOSITORY, model.entityType(), model.idType());
    }
}
