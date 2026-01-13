package com.kivojenko.spring.forge.jpa.model.model;

import com.kivojenko.spring.forge.jpa.model.factory.EndpointRelationFactory;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import jakarta.persistence.MappedSuperclass;
import lombok.Builder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static java.beans.Introspector.decapitalize;

public record JpaEntityModel(TypeElement element,
                             ClassName entityType,
                             JpaId jpaId,
                             JpaEntityPackageNames packages,
                             JpaEntityModelRequirements requirements,
                             ProcessingEnvironment env) {

    public List<EndpointRelation> endpointRelations() {
        return EndpointRelationFactory.resolve(element(), env);
    }


    public String repositoryName() {
        return element.getSimpleName() + "ForgeRepository";
    }

    public String repositoryFqn() {
        return packages().repositoryPackageName() + "." + repositoryName();
    }

    public ClassName repositoryType() {
        return ClassName.get(packages().repositoryPackageName(), repositoryName());
    }

    public String serviceName() {
        return entityType.simpleName() + "ForgeService";
    }

    public String serviceFqn() {
        return packages().servicePackageName() + "." + serviceName();
    }

    public ClassName serviceType() {
        return ClassName.get(packages().servicePackageName(), serviceName());
    }

    public String controllerName() {
        return entityType.simpleName() + "ForgeController";
    }

    public String controllerFqn() {
        return packages().controllerPackageName() + "." + controllerName();
    }

    public String controllerPath() {
        return decapitalize(entityType.simpleName()) + "s";
    }


    public MethodSpec resolveCreateMethod() {

        if (hasBuilder()) {
            if (builderHasNameSetter()) {
                return createViaBuilder();
            }
            return createViaBuilderAndSetter();
        }


        if (hasEmptyCtor()) {
            return createViaEmptyCtorAndSetter();
        }

        if (hasStringCtor()) {
            return createViaCtor();
        }

        throw new IllegalStateException("Cannot generate getOrCreate for " + element().getSimpleName());
    }


    private MethodSpec createViaCtor() {
        return MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entityType())
                .addParameter(String.class, "name")
                .addStatement("return new $T(name)", entityType())
                .build();
    }

    private MethodSpec createViaBuilder() {
        return MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entityType())
                .addParameter(String.class, "name")
                .addStatement("return $T.builder().name(name).build()", entityType())
                .build();
    }

    private MethodSpec createViaEmptyCtorAndSetter() {
        return MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entityType())
                .addParameter(String.class, "name")
                .addStatement("var entity = new $T()", entityType())
                .addStatement("entity.setName(name)")
                .addStatement("return entity")
                .build();
    }

    private MethodSpec createViaBuilderAndSetter() {
        return MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entityType())
                .addParameter(String.class, "name")
                .addStatement("var entity = $T.builder().build()", entityType())
                .addStatement("entity.setName(name)")
                .addStatement("return entity")
                .build();
    }

    public boolean hasStringCtor() {
        return element().getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .filter(c -> c.getParameters().size() == 1)
                .anyMatch(c -> isString(c.getParameters().getFirst().asType()));
    }

    public boolean hasEmptyCtor() {
        return element().getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .anyMatch(c -> c.getParameters().isEmpty());
    }


    public boolean builderHasNameSetter() {
        return element().getEnclosedElements()
                .stream()
                .filter(c -> c.getKind() == ElementKind.FIELD)
                .anyMatch(c -> c.getSimpleName().contentEquals("name"));
    }


    public static boolean isString(TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED &&
                ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().contentEquals("java.lang.String");
    }

    public boolean hasBuilder() {
        return hasBuilderFactory() ||
                element().getAnnotation(Builder.class) != null ||
                element().getAnnotation(MappedSuperclass.class) != null;
    }

    public boolean hasBuilderFactory() {
        return element().getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(m -> m.getModifiers().contains(Modifier.STATIC))
                .anyMatch(m -> m.getSimpleName().contentEquals("builder"));
    }

}
