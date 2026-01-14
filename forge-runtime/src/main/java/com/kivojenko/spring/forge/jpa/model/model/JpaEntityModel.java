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

/**
 * Represents a JPA entity model with information needed for code generation.
 *
 * @param element the entity type element
 * @param entityType the JavaPoet class name of the entity
 * @param jpaId information about the entity's ID field
 * @param packages package names for generated classes
 * @param requirements configuration requirements for the entity
 * @param env the processing environment
 */
public record JpaEntityModel(TypeElement element,
                             ClassName entityType,
                             JpaId jpaId,
                             JpaEntityPackageNames packages,
                             JpaEntityModelRequirements requirements,
                             ProcessingEnvironment env) {

    /**
     * Resolves and returns all endpoint relations for this entity.
     * @return a list of endpoint relations
     */
    public List<EndpointRelation> endpointRelations() {
        return EndpointRelationFactory.resolve(element(), env);
    }


    /**
     * Gets the simple name of the generated repository interface.
     * @return the repository name
     */
    public String repositoryName() {
        return element.getSimpleName() + "ForgeRepository";
    }

    /**
     * Gets the fully qualified name of the generated repository interface.
     * @return the repository FQN
     */
    public String repositoryFqn() {
        return packages().repositoryPackageName() + "." + repositoryName();
    }

    /**
     * Gets the {@link ClassName} of the generated repository interface.
     * @return the repository type
     */
    public ClassName repositoryType() {
        return ClassName.get(packages().repositoryPackageName(), repositoryName());
    }

    /**
     * Gets the simple name of the generated service class.
     * @return the service name
     */
    public String serviceName() {
        return entityType.simpleName() + "ForgeService";
    }

    /**
     * Gets the fully qualified name of the generated service class.
     * @return the service FQN
     */
    public String serviceFqn() {
        return packages().servicePackageName() + "." + serviceName();
    }

    /**
     * Gets the {@link ClassName} of the generated service class.
     * @return the service type
     */
    public ClassName serviceType() {
        return ClassName.get(packages().servicePackageName(), serviceName());
    }

    /**
     * Gets the simple name of the generated REST controller class.
     * @return the controller name
     */
    public String controllerName() {
        return entityType.simpleName() + "ForgeController";
    }

    /**
     * Gets the fully qualified name of the generated REST controller class.
     * @return the controller FQN
     */
    public String controllerFqn() {
        return packages().controllerPackageName() + "." + controllerName();
    }

    /**
     * Gets the base path for the generated REST controller.
     * @return the controller path
     */
    public String controllerPath() {
        return decapitalize(entityType.simpleName()) + "s";
    }

    /**
     * Resolves the {@link MethodSpec} for creating a new entity instance,
     * considering available constructors and builders.
     *
     * @return the create method specification
     * @throws IllegalStateException if no suitable creation method is found
     */
    public MethodSpec resolveCreateMethod() {
        if (hasBuilder()) {
            if (builderHasNameSetter()) return createViaBuilder();
            return createViaBuilderAndSetter();
        }

        if (hasEmptyCtor()) return createViaEmptyCtorAndSetter();
        if (hasStringCtor()) return createViaCtor();

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
