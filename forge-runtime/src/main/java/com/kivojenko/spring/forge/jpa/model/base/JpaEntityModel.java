package com.kivojenko.spring.forge.jpa.model.base;

import com.kivojenko.spring.forge.jpa.factory.EndpointRelationResolver;
import com.kivojenko.spring.forge.jpa.factory.FilterFieldModelFactory;
import com.kivojenko.spring.forge.jpa.model.FilterFieldModel;
import com.kivojenko.spring.forge.jpa.model.relation.EndpointRelation;
import com.kivojenko.spring.forge.jpa.utils.StringUtils;
import com.querydsl.core.BooleanBuilder;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import jakarta.persistence.MappedSuperclass;
import java.util.LinkedHashMap;
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
    private final List<FilterFieldModel> allFilterableFields = FilterFieldModelFactory.resolveAll(getElement(), env);

    @Getter(lazy = true)
    private final List<EndpointRelation> endpointRelations = EndpointRelationResolver.resolve(getElement(), env);

    @Getter(lazy = true)
    private final String getterName = StringUtils.getterName(getJpaId().name());
    @Getter(lazy = true)
    private final String setterName = StringUtils.setterName(getJpaId().name());


    private String controllerPath() {
        var annotation = getRequirements().controllerAnnotation();
        if (annotation != null && !annotation.path().isEmpty()) return annotation.path();

        return pluralize(getEntityType().simpleName());
    }

    public MethodSpec setIdMethod() {
        return MethodSpec
                .methodBuilder("setId")
                .addJavadoc("Sets the ID of the given {@link $T} entity.\n", getEntityType())
                .addJavadoc("@param entity the entity to update\n")
                .addJavadoc("@param id the new ID\n")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.VOID)
                .addParameter(getEntityType(), "entity")
                .addParameter(getJpaId().type(), "id")
                .addStatement("entity.$L(id)", StringUtils.setterName(getJpaId().name()))
                .build();
    }

  /**
     * Builds a {@code create(<fieldType> <fieldName>)} factory method using builder/setter/ctor paths for the given field.
     */
    public MethodSpec createMethodForField(String fieldName) {
        var typeMirror = findFieldTypeMirror(fieldName);
        if (typeMirror == null) {
            throw new IllegalStateException("Cannot find field '" + fieldName + "' on " + getElement().getSimpleName());
        }
        var typeName = TypeName.get(typeMirror);

        if (hasBuilder()) {
            if (builderHasSetter(fieldName)) return createViaBuilder(fieldName, typeName);
            return createViaBuilderAndSetter(fieldName, typeName);
        }

        if (hasEmptyCtor()) return createViaEmptyCtorAndSetter(fieldName, typeName);
        if (hasCtorWithSingleParam(typeMirror)) return createViaCtor(fieldName, typeName);

        throw new IllegalStateException("Cannot generate create(..) for field '" + fieldName + "' on " + getElement().getSimpleName());
    }

    private MethodSpec createViaCtor(String fieldName, TypeName fieldType) {
        return MethodSpec
                .methodBuilder("create")
                .addJavadoc("Creates a new instance of {@link $T} with the given $L using the constructor.\n", getEntityType(), fieldName)
                .addJavadoc("@param $L the $L of the entity\n", fieldName, fieldName)
                .addJavadoc("@return the newly created entity\n")
                .addModifiers(Modifier.PROTECTED)
                .returns(getEntityType())
                .addParameter(fieldType, fieldName)
                .addStatement("return new $T($L)", getEntityType(), fieldName)
                .build();
    }

    private MethodSpec createViaBuilder(String fieldName, TypeName fieldType) {
        return MethodSpec
                .methodBuilder("create")
                .addJavadoc("Creates a new instance of {@link $T} with the given $L using the builder.\n", getEntityType(), fieldName)
                .addJavadoc("@param $L the $L of the entity\n", fieldName, fieldName)
                .addJavadoc("@return the newly created entity\n")
                .addModifiers(Modifier.PROTECTED)
                .returns(getEntityType())
                .addParameter(fieldType, fieldName)
                .addStatement("return $T.builder().$L($L).build()", getEntityType(), fieldName, fieldName)
                .build();
    }

    private MethodSpec createViaEmptyCtorAndSetter(String fieldName, TypeName fieldType) {
        return MethodSpec
                .methodBuilder("create")
                .addJavadoc("Creates a new instance of {@link $T} with the given $L using the empty constructor and a setter.\n", getEntityType(), fieldName)
                .addJavadoc("@param $L the $L of the entity\n", fieldName, fieldName)
                .addJavadoc("@return the newly created entity\n")
                .addModifiers(Modifier.PROTECTED)
                .returns(getEntityType())
                .addParameter(fieldType, fieldName)
                .addStatement("var entity = new $T()", getEntityType())
                .addStatement("entity.$L($L)", StringUtils.setterName(fieldName), fieldName)
                .addStatement("return entity")
                .build();
    }

    private MethodSpec createViaBuilderAndSetter(String fieldName, TypeName fieldType) {
        return MethodSpec
                .methodBuilder("create")
                .addJavadoc("Creates a new instance of {@link $T} with the given $L using the builder and a setter.\n", getEntityType(), fieldName)
                .addJavadoc("@param $L the $L of the entity\n", fieldName, fieldName)
                .addJavadoc("@return the newly created entity\n")
                .addModifiers(Modifier.PROTECTED)
                .returns(getEntityType())
                .addParameter(fieldType, fieldName)
                .addStatement("var entity = $T.builder().build()", getEntityType())
                .addStatement("entity.$L($L)", StringUtils.setterName(fieldName), fieldName)
                .addStatement("return entity")
                .build();
    }

    private boolean hasCtorWithSingleParam(TypeMirror paramType) {
        return getElement()
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .anyMatch(c -> c.getParameters().size() == 1 && env.getTypeUtils().isSameType(c.getParameters().getFirst().asType(), paramType));
    }

    private boolean hasEmptyCtor() {
        return getElement()
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(ExecutableElement.class::cast)
                .anyMatch(c -> c.getParameters().isEmpty());
    }

    private boolean builderHasSetter(String fieldName) {
        return getElement()
                .getEnclosedElements()
                .stream()
                .filter(c -> c.getKind() == ElementKind.FIELD)
                .anyMatch(c -> c.getSimpleName().contentEquals(fieldName));
    }

    private TypeMirror findFieldTypeMirror(String fieldName) {
        var varEl = findFieldInHierarchy(getElement(), fieldName);
        return varEl != null ? varEl.asType() : null;
    }

    private javax.lang.model.element.VariableElement findFieldInHierarchy(TypeElement typeElement, String fieldName) {
        TypeElement current = typeElement;
        while (current != null) {
            var match = current.getEnclosedElements()
                    .stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(fieldName))
                    .findFirst();
            if (match.isPresent()) {
                return (javax.lang.model.element.VariableElement) match.get();
            }
            var superType = current.getSuperclass();
            if (superType.getKind() == TypeKind.DECLARED) {
                current = (TypeElement) ((DeclaredType) superType).asElement();
            } else {
                current = null;
            }
        }
        return null;
    }

    public TypeName resolveFieldTypeName(String fieldName) {
        var tm = findFieldTypeMirror(fieldName);
        if (tm == null) {
            throw new IllegalStateException("Cannot find field '" + fieldName + "' on " + getElement().getSimpleName());
        }
        return TypeName.get(tm);
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
                .addJavadoc("Converts the filter criteria into a QueryDSL {@link $T}.\n", BooleanBuilder.class)
                .addJavadoc("@return the predicate representing the filter criteria\n")
                .addModifiers(Modifier.PUBLIC)
                .returns(BooleanBuilder.class)
                .addStatement("var $L = new $T()", BUILDER_VAR_NAME, BooleanBuilder.class)
                .addStatement(
                        "var $L = $T.$L",
                        ENTITY_VAR_NAME,
                        getEntityPathType(),
                        StringUtils.decapitalize(getEntityType().simpleName())
                );

        // Group all filterable mappings by exposed name to allow OR-combining duplicates
        var groups = new LinkedHashMap<String, List<FilterFieldModel>>();
        for (var f : getAllFilterableFields()) {
            groups.computeIfAbsent(f.getName(), k -> new java.util.ArrayList<>()).add(f);
        }

        for (var field : getFilterableFields()) {
            var group = groups.get(field.getName());
            if (group == null || group.size() <= 1) {
                // No duplicates — generate default filtering for the primary mapping
                field.addFiltering(builder);
                continue;
            }

            // Duplicates present: for String-typed filters, OR all mapped targets using the primary's match mode
            if (field.getTypeName().equals(com.squareup.javapoet.ClassName.get(String.class))) {
                var primary = group.getFirst();
                String op;
                switch (primary.getAnnotation().stringMatchMode()) {
                    case STARTS_WITH -> op = "startsWith";
                    case ENDS_WITH -> op = "endsWith";
                    case CONTAINS -> op = "contains";
                    case CONTAINS_IGNORE_CASE -> op = "containsIgnoreCase";
                    case EQUALS -> op = "eq";
                    case EQUALS_IGNORE_CASE -> op = "equalsIgnoreCase";
                    default -> op = "containsIgnoreCase"; // sensible default
                }

                builder.beginControlFlow("if ($L != null && !$L.isBlank())", field.getName(), field.getName());
                // Initialize OR expression with the first mapping
                var first = group.getFirst();
                builder.addStatement(
                        "var __expr = entity.$L." + op + "($L)",
                        first.getTargetFieldName(),
                        field.getName()
                );
                // Chain remaining mappings with .or(...)
                for (int i = 1; i < group.size(); i++) {
                    var alt = group.get(i);
                    builder.addStatement(
                            "__expr = __expr.or(entity.$L." + op + "($L))",
                            alt.getTargetFieldName(),
                            field.getName()
                    );
                }
                builder.addStatement("builder.and(__expr)");
                builder.endControlFlow();
            } else {
                // Non-string or unsupported types — fall back to primary mapping only
                field.addFiltering(builder);
            }
        }
        return builder.addStatement("return $L", BUILDER_VAR_NAME).build();
    }

}
