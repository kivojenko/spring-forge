package com.kivojenko.spring.forge.jpa.model;

import com.kivojenko.spring.forge.annotation.HasRestController;
import com.squareup.javapoet.ClassName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Optional;

import static java.beans.Introspector.decapitalize;

public record JpaEntityModel(TypeElement element,
                             ClassName entityType,
                             JpaId jpaId,
                             String packageName,
                             String repositoryPackageName,
                             String controllerPackageName,
                             boolean hasName,
                             boolean wantsController) {

    public String repositoryName() {
        return element.getSimpleName() + "Repository";
    }

    public String repositoryFqn() {
        return repositoryPackageName + "." + repositoryName();
    }

    public ClassName repositoryType() {
        return ClassName.get(repositoryPackageName, repositoryName());
    }

    public String controllerName() {
        return entityType.simpleName() + "Controller";
    }

    public String controllerFqn() {
        return controllerPackageName + "." + controllerName();
    }

    public String controllerPath() {
        return decapitalize(entityType.simpleName()) + "s";
    }

    public static JpaEntityModel of(TypeElement entity, ProcessingEnvironment env) {
        var elements = env.getElementUtils();
        var packageName = elements.getPackageOf(entity).getQualifiedName().toString();
        var entityType = ClassName.get(entity);
        var jpaId = JpaId.resolveId(entity);

        var types = env.getTypeUtils();
        var hasNameType = elements.getTypeElement("com.kivojenko.spring.forge.jpa.contract.HasName");
        var hasName = hasNameType != null && types.isAssignable(entity.asType(), hasNameType.asType());

        var repositoryPackage = Optional.ofNullable(env.getOptions().get("springforge.repository.package"))
                .filter(s -> !s.isBlank())
                .orElse(packageName);
        var controllerPackage = Optional.ofNullable(env.getOptions().get("springforge.controller.package"))
                .filter(s -> !s.isBlank())
                .orElse(packageName);

        var hasController = entity.getAnnotation(HasRestController.class) != null;

        return new JpaEntityModel(entity, entityType, jpaId, packageName, repositoryPackage, controllerPackage,
                hasName, hasController);
    }
}
