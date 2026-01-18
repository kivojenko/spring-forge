package com.kivojenko.spring.forge.jpa.model.base;

import com.kivojenko.spring.forge.annotation.FilterField;
import com.kivojenko.spring.forge.config.SpringForgeConfig;
import com.kivojenko.spring.forge.jpa.factory.EndpointRelationFactory;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import jakarta.persistence.MappedSuperclass;
import lombok.Builder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;

import static com.kivojenko.spring.forge.jpa.model.base.JpaEntityPackageNames.resolvePackageNames;
import static com.kivojenko.spring.forge.jpa.model.base.JpaEntityRequirements.resolveRequirements;
import static com.kivojenko.spring.forge.jpa.model.base.JpaId.resolveId;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.pluralize;
import static java.beans.Introspector.decapitalize;

/**
 * Represents a JPA entity model with information needed for code generation.
 *
 * @param element      the entity type element
 * @param entityType   the JavaPoet class name of the entity
 * @param jpaId        information about the entity's ID field
 * @param packages     package names for generated classes
 * @param requirements configuration requirements for the entity
 * @param env          the processing environment
 */
public record JpaEntityModel(
        TypeElement element,
        ClassName entityType,
        JpaId jpaId,
        JpaEntityPackageNames packages,
        JpaEntityRequirements requirements,
        ProcessingEnvironment env
)
{
    public JpaEntityModel(TypeElement entity, SpringForgeConfig config, ProcessingEnvironment env) {
        this(
                entity,
                ClassName.get(entity),
                resolveId(entity),
                resolvePackageNames(entity, config, env),
                resolveRequirements(entity, env),
                env
        );
    }

    /**
     * Resolves and returns all endpoint relations for this entity.
     *
     * @return a list of endpoint relations
     */
    public List<EndpointRelation> endpointRelations() {
        return EndpointRelationFactory.resolve(element(), env);
    }

    /**
     * Gets the simple name of the generated repository interface.
     *
     * @return the repository name
     */
    public String repositoryName() {
        return element.getSimpleName() + "ForgeRepository";
    }

    /**
     * Gets the fully qualified name of the generated repository interface.
     *
     * @return the repository FQN
     */
    public String repositoryFqn() {
        return packages().repositoryPackageName() + "." + repositoryName();
    }

    /**
     * Gets the {@link ClassName} of the generated repository interface.
     *
     * @return the repository type
     */
    public ClassName repositoryType() {
        return ClassName.get(packages().repositoryPackageName(), repositoryName());
    }

    /**
     * Gets the simple name of the generated service class.
     *
     * @return the service name
     */
    public String serviceName() {
        return entityType.simpleName() + "ForgeService";
    }

    /**
     * Gets the fully qualified name of the generated service class.
     *
     * @return the service FQN
     */
    public String serviceFqn() {
        return packages().servicePackageName() + "." + serviceName();
    }

    /**
     * Gets the {@link ClassName} of the generated service class.
     *
     * @return the service type
     */
    public ClassName serviceType() {
        return ClassName.get(packages().servicePackageName(), serviceName());
    }

    /**
     * Gets the simple name of the generated REST controller class.
     *
     * @return the controller name
     */
    public String controllerName() {
        return entityType.simpleName() + "ForgeController";
    }

    /**
     * Gets the fully qualified name of the generated REST controller class.
     *
     * @return the controller FQN
     */
    public String controllerFqn() {
        return packages().controllerPackageName() + "." + controllerName();
    }

    /**
     * Gets the base path for the generated REST controller.
     *
     * @return the controller path
     */
    public String controllerPath() {
        var annotation = requirements().controllerAnnotation();
        if (annotation != null && !annotation.path().isEmpty()) return annotation.path();

        return pluralize(entityType.simpleName());
    }

    /**
     * Gets the simple name of the generated filter interface.
     *
     * @return the filter name
     */
    public String filterName() {
        return element.getSimpleName() + "ForgeFilter";
    }

    /**
     * Gets the fully qualified name of the generated filter interface.
     *
     * @return the filter FQN
     */
    public String filterFqn() {
        return packages().filterPackageName() + "." + filterName();
    }

    /**
     * Gets the {@link ClassName} of the generated filter interface.
     *
     * @return the filter type
     */
    public ClassName filterType() {
        return ClassName.get(packages().filterPackageName(), filterName());
    }

    /**
     * Gets the type of the filter field based on the entity's ID field type.
     *
     * @return the filter field type
     */
    public TypeName getFilterFieldType() {
        return jpaId.type();
    }

    /**
     * Gets the filter field name based on the entity's ID field name.
     *
     * @return the filter field name
     */
    public String getFilterFieldName() {
        return pluralize(decapitalize(element.getSimpleName().toString()));
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
        if (hasNameCtor()) return createViaCtor();

        throw new IllegalStateException("Cannot generate getOrCreateAnnotation for " + element().getSimpleName());
    }


    private MethodSpec createViaCtor() {
        return MethodSpec
                .methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entityType())
                .addParameter(String.class, "name")
                .addStatement("return new $T(name)", entityType())
                .build();
    }

    private MethodSpec createViaBuilder() {
        return MethodSpec
                .methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entityType())
                .addParameter(String.class, "name")
                .addStatement("return $T.builder().name(name).build()", entityType())
                .build();
    }

    private MethodSpec createViaEmptyCtorAndSetter() {
        return MethodSpec
                .methodBuilder("create")
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
        return MethodSpec
                .methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(entityType())
                .addParameter(String.class, "name")
                .addStatement("var entity = $T.builder().build()", entityType())
                .addStatement("entity.setName(name)")
                .addStatement("return entity")
                .build();
    }

    private boolean hasNameCtor() {
        return element()
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .filter(c -> c.getParameters().size() == 1)
                .anyMatch(c -> isNameType(c.getParameters().getFirst().asType()));
    }

    private boolean hasEmptyCtor() {
        return element()
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .anyMatch(c -> c.getParameters().isEmpty());
    }

    private boolean builderHasNameSetter() {
        return element()
                .getEnclosedElements()
                .stream()
                .filter(c -> c.getKind() == ElementKind.FIELD)
                .anyMatch(c -> c.getSimpleName().contentEquals("name") && isNameType(c.asType()));
    }


    /**
     * Checks if the given type is a String.
     *
     * @param type the type mirror to check
     * @return true if it is a String, false otherwise
     */
    public boolean isNameType(TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED &&
                ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().contentEquals("java.lang.String");
    }

    private boolean hasBuilder() {
        return hasBuilderFactory() ||
                element().getAnnotation(Builder.class) != null ||
                element().getAnnotation(MappedSuperclass.class) != null;
    }

    private boolean hasBuilderFactory() {
        return element()
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(m -> m.getModifiers().contains(Modifier.STATIC))
                .anyMatch(m -> m.getSimpleName().contentEquals("builder"));
    }

    /**
     * Retrieves the list of fields in the entity that are annotated with {@code @FilterField}.
     * These fields are considered filterable. Validates that such fields are not marked as
     * transient in any context (Java, JPA, or Beans). If a transient field is annotated with
     * {@code @FilterField}, an {@link IllegalStateException} is thrown.
     *
     * @return a list of filterable fields represented as {@link VariableElement}.
     * @throws IllegalStateException if a field annotated with {@code @FilterField} is transient.
     */
    public List<VariableElement> getFilterableFields() {
        var filterFields = new ArrayList<VariableElement>();

        var fields = ElementFilter.fieldsIn(element.getEnclosedElements());

        for (var field : fields) {
            var hasFilter = field.getAnnotation(FilterField.class) != null;
            if (!hasFilter) continue;

            var isJavaTransient = field.getModifiers().contains(Modifier.TRANSIENT);
            var isJpaTransient = field.getAnnotation(jakarta.persistence.Transient.class) != null;
            var isBeansTransient = field.getAnnotation(java.beans.Transient.class) != null;
            if (isJavaTransient || isJpaTransient || isBeansTransient) {
                throw new IllegalStateException("@FilterField is not allowed on transient field: " +
                        field.getSimpleName() +
                        " in " +
                        element.getQualifiedName());
            }
            filterFields.add(field);
        }

        return filterFields;

    }

}
