package com.kivojenko.spring.forge.jpa.generator;

import com.kivojenko.spring.forge.jpa.model.JpaEntityModel;
import com.kivojenko.spring.forge.jpa.service.ForgeService;
import com.kivojenko.spring.forge.jpa.service.HasNameForgeService;
import com.kivojenko.spring.forge.jpa.service.HasNameForgeServiceWithGetOrCreate;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

import static com.kivojenko.spring.forge.jpa.utils.MethodUtils.getSetIdMethod;

public final class JpaServiceGenerator {

    private static final ClassName SERVICE = ClassName.get("org.springframework.stereotype", "Service");

    private static final ClassName FORGE_SERVICE = ClassName.get(ForgeService.class);
    private static final ClassName HAS_NAME_FORGE_SERVICE = ClassName.get(HasNameForgeService.class);
    private static final ClassName HAS_NAME_FORGE_SERVICE_WITH_GET_OR_CREATE =
            ClassName.get(HasNameForgeServiceWithGetOrCreate.class);

    public static JavaFile generateFile(JpaEntityModel model) {
        return JavaFile.builder(model.servicePackageName(), generate(model)).build();
    }

    public static TypeSpec generate(JpaEntityModel model) {
        var builder = TypeSpec.classBuilder(model.serviceName()).addModifiers(Modifier.PUBLIC).addAnnotation(SERVICE);

        var superClassName = FORGE_SERVICE;
        if (model.hasName()) {
            superClassName = HAS_NAME_FORGE_SERVICE;

            if (model.wantsGetOrCreate()) {
                superClassName = HAS_NAME_FORGE_SERVICE_WITH_GET_OR_CREATE;
                builder.addMethod(model.resolveCreateMethod());
            }
        }

        var superClass = ParameterizedTypeName.get(superClassName,
                model.entityType(),
                model.jpaId().type(),
                model.repositoryType());

        return builder.superclass(superClass).addMethod(getSetIdMethod(model)).build();
    }

}
