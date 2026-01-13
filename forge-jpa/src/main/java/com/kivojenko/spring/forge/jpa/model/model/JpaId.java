package com.kivojenko.spring.forge.jpa.model.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import java.util.Optional;

import static com.kivojenko.spring.forge.jpa.utils.StringUtils.decapitalize;

public record JpaId(String name, TypeName type) {
    public static JpaId resolveId(TypeElement entity) {
        return resolveIdRecursive(entity).orElseThrow(() -> new IllegalStateException("Entity " +
                entity.getQualifiedName() +
                " must declare an @Id field or getter (directly or via superclass)"));
    }

    private static Optional<JpaId> resolveIdRecursive(TypeElement type) {
        var local = resolveIdFromType(type);
        if (local.isPresent()) {
            return local;
        }

        var superclass = type.getSuperclass();
        if (superclass.getKind() != TypeKind.DECLARED) {
            return Optional.empty();
        }

        var superElement = (TypeElement) ((DeclaredType) superclass).asElement();

        if (superElement.getQualifiedName().contentEquals("java.lang.Object")) {
            return Optional.empty();
        }

        return resolveIdRecursive(superElement);
    }

    private static Optional<JpaId> resolveIdFromType(TypeElement type) {

        var field = type.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(VariableElement.class::cast)
                .filter(f -> f.getAnnotation(jakarta.persistence.Id.class) != null)
                .findFirst();

        if (field.isPresent()) {
            return Optional.of(new JpaId(field.get().getSimpleName().toString(), ClassName.get(field.get().asType())));
        }

        var getter = type.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(m -> m.getAnnotation(jakarta.persistence.Id.class) != null)
                .findFirst();

        if (getter.isPresent()) {
            var m = getter.get();
            return Optional.of(new JpaId(inferPropertyName(m.getSimpleName().toString()),
                    ClassName.get(m.getReturnType())));
        }

        return Optional.empty();
    }


    private static String inferPropertyName(String getterName) {
        if (getterName.startsWith("get") && getterName.length() > 3) {
            return decapitalize(getterName.substring(3));
        }
        if (getterName.startsWith("is") && getterName.length() > 2) {
            return decapitalize(getterName.substring(2));
        }
        throw new IllegalStateException("Unsupported @Id getter name: " + getterName);
    }

}
