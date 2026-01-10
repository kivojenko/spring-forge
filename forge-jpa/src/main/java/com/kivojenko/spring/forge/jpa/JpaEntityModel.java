package com.kivojenko.spring.forge.jpa;

import com.kivojenko.spring.forge.annotation.HasJpaRepository;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

public record JpaEntityModel(TypeElement element,
                             ClassName entityType,
                             TypeName idType,
                             String packageName,
                             boolean hasName) {
    public String repositoryName() {
        return element.getSimpleName() + "Repository";
    }

    public String repositoryFqn() {
        return packageName + "." + repositoryName();
    }

    public static JpaEntityModel of(TypeElement entity, ProcessingEnvironment env) {
        var elements = env.getElementUtils();
        var annotation = entity.getAnnotation(HasJpaRepository.class);

        var packageName = elements.getPackageOf(entity).getQualifiedName().toString();
        var entityType = ClassName.get(entity);
        var idType = ClassName.get(idTypeMirror(annotation));

        var types = env.getTypeUtils();
        var hasNameType = elements.getTypeElement("com.kivojenko.spring.forge.jpa.contract.HasName");
        var hasName = hasNameType != null && types.isAssignable(entity.asType(), hasNameType.asType());

        return new JpaEntityModel(entity, entityType, idType, packageName, hasName);
    }

    private static TypeMirror idTypeMirror(HasJpaRepository annotation) {
        try {
            annotation.idType();
            throw new IllegalStateException("@HasJpaRepository.idType() must be a class literal");
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }
}
