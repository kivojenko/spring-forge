package com.kivojenko.spring.forge.jpa.model.base;

import com.kivojenko.spring.forge.jpa.factory.EndpointRelationFactory;
import com.kivojenko.spring.forge.jpa.factory.FilterFieldModelFactory;
import com.kivojenko.spring.forge.jpa.model.FilterFieldModel;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.utils.StringUtils;
import com.querydsl.core.BooleanBuilder;
import com.squareup.javapoet.*;
import jakarta.persistence.MappedSuperclass;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static com.kivojenko.spring.forge.jpa.generator.FilterGenerator.BUILDER_VAR_NAME;
import static com.kivojenko.spring.forge.jpa.generator.FilterGenerator.ENTITY_VAR_NAME;
import static com.kivojenko.spring.forge.jpa.model.base.JpaEntityPackageNames.resolvePackageNames;
import static com.kivojenko.spring.forge.jpa.model.base.JpaEntityRequirements.resolveRequirements;
import static com.kivojenko.spring.forge.jpa.model.base.JpaId.resolveId;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.pluralize;
import static java.beans.Introspector.decapitalize;

/**
 * Represents a JPA entity model with information needed for code generation.
 *
 */
@Getter
@RequiredArgsConstructor
public final class JpaEntityModel {
    private final ProcessingEnvironment env;
    private final TypeElement element;

    @Getter(lazy = true)
    private final ClassName entityType = ClassName.get(getElement());

    @Getter(lazy = true)
    private final TypeName entityPathType = resolveEntityPathType();

    @Getter(lazy = true)
    private final JpaId jpaId = resolveId(getElement());
    @Getter(lazy = true)
    private final JpaEntityPackageNames packages = resolvePackageNames(getElement(), env);
    @Getter(lazy = true)
    private final JpaEntityRequirements requirements = resolveRequirements(getElement(), env);

    @Getter(lazy = true)
    private final String repositoryName = getElement().getSimpleName() + "ForgeRepository";
    @Getter(lazy = true)
    private final String repositoryFqn = getPackages().repositoryPackageName() + "." + getRepositoryName();
    @Getter(lazy = true)
    private final ClassName repositoryType = ClassName.get(getPackages().repositoryPackageName(), getRepositoryName());

    @Getter(lazy = true)
    private final String serviceName = getEntityType().simpleName() + "ForgeService";
    @Getter(lazy = true)
    private final String serviceFqn = getPackages().servicePackageName() + "." + getServiceName();
    @Getter(lazy = true)
    private final ClassName serviceType = ClassName.get(getPackages().servicePackageName(), getServiceName());

    @Getter(lazy = true)
    private final String controllerName = getEntityType().simpleName() + "ForgeController";
    @Getter(lazy = true)
    private final String controllerFqn = getPackages().controllerPackageName() + "." + controllerName;
    @Getter(lazy = true)
    private final String controllerPath = controllerPath();

    @Getter(lazy = true)
    private final String filterName = getElement().getSimpleName() + "ForgeFilter";
    @Getter(lazy = true)
    private final String filterFqn = getPackages().filterPackageName() + "." + getFilterName();
    @Getter(lazy = true)
    private final ClassName filterType = ClassName.get(getPackages().filterPackageName(), getFilterName());
    @Getter(lazy = true)
    private final String filterFieldName = pluralize(decapitalize(getElement().getSimpleName().toString()));

    @Getter(lazy = true)
    private final List<FilterFieldModel> filterableFields = FilterFieldModelFactory.resolve(getElement(), env);

    @Getter(lazy = true)
    private final List<EndpointRelation> endpointRelations = EndpointRelationFactory.resolve(getElement(), env);

    @Getter(lazy = true)
    private final String getterName = StringUtils.getterName(getJpaId().name());
    @Getter(lazy = true)
    private final String setterName = StringUtils.setterName(getJpaId().name());


    private String controllerPath() {
        var annotation = getRequirements().controllerAnnotation();
        if (annotation != null && !annotation.path().isEmpty()) return annotation.path();

        return pluralize(getEntityType().simpleName());
    }

    /**
     * Generates a protected {@code setId} method that overrides the base controller/service method.
     *
     * @return the method specification
     */
    public MethodSpec setIdMethod() {
        return MethodSpec
                .methodBuilder("setId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.VOID)
                .addParameter(getEntityType(), "entity")
                .addParameter(getJpaId().type(), "id")
                .addStatement("entity.$L(id)", StringUtils.setterName(getJpaId().name()))
                .build();
    }

    public MethodSpec findAllFilteredEndpoint() {
        var pageableDefaultAnnotation = AnnotationSpec.builder(PAGEABLE_DEFAULT).addMember("size", "$L", 25).build();
        var pageableParam = ParameterSpec
                .builder(PAGEABLE, "pageable")
                .addAnnotation(pageableDefaultAnnotation)
                .build();

        var requestParamAnnotation = AnnotationSpec.builder(REQUEST_PARAM).addMember("required", "false").build();
        var filterParam = ParameterSpec
                .builder(getFilterType(), "filter")
                .addAnnotation(requestParamAnnotation)
                .build();

        var mappingAnnotationBuilder = AnnotationSpec.builder(GET_MAPPING);
        getFilterableFields()
                .stream()
                .map(f -> f.getElement().getSimpleName().toString())
                .forEach(f -> mappingAnnotationBuilder.addMember("params", "$S", f));

        return MethodSpec
                .methodBuilder("findAllFiltered")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(mappingAnnotationBuilder.build())
                .returns(ParameterizedTypeName.get(ITERABLE, getEntityType()))
                .addParameter(pageableParam)
                .addParameter(filterParam)
                .addStatement("return service.findAllFiltered(pageable, filter)")
                .build();
    }


    public MethodSpec findAllFilteredMethod() {
        var pageableParam = ParameterSpec.builder(PAGEABLE, "pageable").build();
        var filterParam = ParameterSpec.builder(getFilterType(), "filter").build();
        return MethodSpec
                .methodBuilder("findAll")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ITERABLE, getEntityType()))
                .addParameter(pageableParam)
                .addParameter(filterParam)
                .addStatement("return repository.findAll(filter.toPredicate(), pageable)")
                .build();
    }

    public MethodSpec nameCreateMethod() {
        if (hasBuilder()) {
            if (builderHasNameSetter()) return createViaBuilder();
            return createViaBuilderAndSetter();
        }

        if (hasEmptyCtor()) return createViaEmptyCtorAndSetter();
        if (hasNameCtor()) return createViaCtor();

        throw new IllegalStateException("Cannot generate getOrCreateAnnotation for " + getElement().getSimpleName());
    }

    private MethodSpec createViaCtor() {
        return MethodSpec
                .methodBuilder("create")
                .addModifiers(Modifier.PROTECTED)
                .returns(getEntityType())
                .addParameter(String.class, "name")
                .addStatement("return new $T(name)", getEntityType())
                .build();
    }

    private MethodSpec createViaBuilder() {
        return MethodSpec
                .methodBuilder("create")
                .addModifiers(Modifier.PROTECTED)
                .returns(getEntityType())
                .addParameter(String.class, "name")
                .addStatement("return $T.builder().name(name).build()", getEntityType())
                .build();
    }

    private MethodSpec createViaEmptyCtorAndSetter() {
        return MethodSpec
                .methodBuilder("create")
                .addModifiers(Modifier.PROTECTED)
                .returns(getEntityType())
                .addParameter(String.class, "name")
                .addStatement("var entity = new $T()", getEntityType())
                .addStatement("entity.setName(name)")
                .addStatement("return entity")
                .build();
    }

    private MethodSpec createViaBuilderAndSetter() {
        return MethodSpec
                .methodBuilder("create")
                .addModifiers(Modifier.PROTECTED)
                .returns(getEntityType())
                .addParameter(String.class, "name")
                .addStatement("var entity = $T.builder().build()", getEntityType())
                .addStatement("entity.setName(name)")
                .addStatement("return entity")
                .build();
    }

    private boolean hasNameCtor() {
        return getElement()
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .filter(c -> c.getParameters().size() == 1)
                .anyMatch(c -> isNameType(c.getParameters().getFirst().asType()));
    }

    private boolean hasEmptyCtor() {
        return getElement()
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .anyMatch(c -> c.getParameters().isEmpty());
    }

    private boolean builderHasNameSetter() {
        return getElement()
                .getEnclosedElements()
                .stream()
                .filter(c -> c.getKind() == ElementKind.FIELD)
                .anyMatch(c -> c.getSimpleName().contentEquals("name") && isNameType(c.asType()));
    }

    private boolean isNameType(TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED &&
                ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().contentEquals("java.lang.String");
    }

    private boolean hasBuilder() {
        return hasBuilderFactory() ||
                getElement().getAnnotation(Builder.class) != null ||
                getElement().getAnnotation(MappedSuperclass.class) != null;
    }

    private boolean hasBuilderFactory() {
        return getElement()
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(m -> m.getModifiers().contains(Modifier.STATIC))
                .anyMatch(m -> m.getSimpleName().contentEquals("builder"));
    }

    public boolean wantsFilter() {
        return !getFilterableFields().isEmpty();
    }

    private ClassName resolveEntityPathType() {
        return ClassName.get(
                env.getElementUtils().getPackageOf(element).getQualifiedName().toString(),
                "Q" + element.getSimpleName()
        );
    }

    public MethodSpec toPredicateMethod() {
        var builder = MethodSpec
                .methodBuilder("toPredicate")
                .addModifiers(Modifier.PUBLIC)
                .returns(BooleanBuilder.class)
                .addStatement("var $L = new $T()", BUILDER_VAR_NAME, BooleanBuilder.class)
                .addStatement(
                        "var $L = $T.$L",
                        ENTITY_VAR_NAME,
                        getEntityPathType(),
                        StringUtils.decapitalize(getEntityType().simpleName())
                );

        for (var field : getFilterableFields()) {
            field.addBuilderStatement(builder);
        }
        return builder.addStatement("return $L", BUILDER_VAR_NAME).build();
    }


}
