package com.kivojenko.spring.forge.jpa.model.base;

import com.kivojenko.spring.forge.annotation.filter.FamilyMatchMode;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
    private final Map<String, FamilyMatchMode> filterFamilyModes =
            FilterFieldModelFactory.resolveFamilyModes(getElement());

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
        // Support nested path like "country.code" (relation.id)
        if (fieldName != null && fieldName.contains(".")) {
            var parts = fieldName.split("\\.");
            if (parts.length < 2) {
                throw new IllegalStateException("Invalid nested field path: " + fieldName);
            }
            var relationField = parts[0];
            var idField = parts[parts.length - 1];

            // Resolve relation type
            var relationVar = findFieldInHierarchy(getElement(), relationField);
            if (relationVar == null) {
                throw new IllegalStateException("Cannot find relation field '" + relationField + "' on " + getElement().getSimpleName());
            }
            if (!(relationVar.asType() instanceof DeclaredType declared)) {
                throw new IllegalStateException("Field '" + relationField + "' is not an entity type on " + getElement().getSimpleName());
            }
            var relationEl = (TypeElement) declared.asElement();
            var relationModel = com.kivojenko.spring.forge.jpa.factory.JpaEntityModelFactory.get(relationEl);
            if (relationModel == null) {
                throw new IllegalStateException("Cannot resolve model for relation '" + relationField + "'");
            }

            // Ensure we target the relation's ID field
            var expectedId = relationModel.getJpaId().name();
            if (!expectedId.equals(idField)) {
                throw new IllegalStateException("Nested get-or-create currently supports relation ID only: expected '" + relationField + "." + expectedId + "' but got '" + fieldName + "'");
            }

            var idType = relationModel.getJpaId().type();
            return createViaRelationId(relationField, relationModel.getEntityType(), idType);
        }

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
        if (fieldName != null && fieldName.contains(".")) {
            return resolveNestedFieldTypeMirror(fieldName);
        }
        var varEl = findFieldInHierarchy(getElement(), fieldName);
        return varEl != null ? varEl.asType() : null;
    }

    private TypeMirror resolveNestedFieldTypeMirror(String path) {
        var parts = path.split("\\.");
        TypeElement current = getElement();
        for (int i = 0; i < parts.length; i++) {
            var segment = parts[i];
            var varEl = findFieldInHierarchy(current, segment);
            if (varEl == null) {
                throw new IllegalStateException("Cannot find field '" + segment + "' on " + current.getSimpleName());
            }
            var type = varEl.asType();
            if (i == parts.length - 1) {
                return type; // last segment type
            }
            if (type.getKind() != TypeKind.DECLARED) {
                throw new IllegalStateException("Intermediate segment '" + segment + "' is not a declared type on " + current.getSimpleName());
            }
            current = (TypeElement) ((DeclaredType) type).asElement();
        }
        return null;
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

    private MethodSpec createViaRelationId(String relationField, ClassName relationType, TypeName idType) {
        // create(<ID> relationId)
        var param = com.squareup.javapoet.ParameterSpec.builder(idType, relationField + "Id").build();

        if (hasBuilder()) {
            if (builderHasSetter(relationField)) {
                return MethodSpec
                        .methodBuilder("create")
                        .addJavadoc("Creates a new instance of {@link $T} linking '$L' by its ID.\n", getEntityType(), relationField)
                        .addJavadoc("@param $L the ID of $L\n", param.name, relationField)
                        .addJavadoc("@return the newly created entity\n")
                        .addModifiers(Modifier.PROTECTED)
                        .returns(getEntityType())
                        .addParameter(param)
                        .addStatement("return $T.builder().$L(entityManager.getReference($T.class, $L)).build()", getEntityType(), relationField, relationType, param.name)
                        .build();
            }
            // builder exists but no direct setter on builder -> builder + setter
            return MethodSpec
                    .methodBuilder("create")
                    .addJavadoc("Creates a new instance of {@link $T} linking '$L' by its ID.\n", getEntityType(), relationField)
                    .addJavadoc("@param $L the ID of $L\n", param.name, relationField)
                    .addJavadoc("@return the newly created entity\n")
                    .addModifiers(Modifier.PROTECTED)
                    .returns(getEntityType())
                    .addParameter(param)
                    .addStatement("var entity = $T.builder().build()", getEntityType())
                    .addStatement("entity.$L(entityManager.getReference($T.class, $L))", StringUtils.setterName(relationField), relationType, param.name)
                    .addStatement("return entity")
                    .build();
        }

        if (hasEmptyCtor()) {
            return MethodSpec
                    .methodBuilder("create")
                    .addJavadoc("Creates a new instance of {@link $T} linking '$L' by its ID.\n", getEntityType(), relationField)
                    .addJavadoc("@param $L the ID of $L\n", param.name, relationField)
                    .addJavadoc("@return the newly created entity\n")
                    .addModifiers(Modifier.PROTECTED)
                    .returns(getEntityType())
                    .addParameter(param)
                    .addStatement("var entity = new $T()", getEntityType())
                    .addStatement("entity.$L(entityManager.getReference($T.class, $L))", StringUtils.setterName(relationField), relationType, param.name)
                    .addStatement("return entity")
                    .build();
        }

        throw new IllegalStateException("Cannot generate create(..) for nested relation '" + relationField + "' on " + getElement().getSimpleName());
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

    public boolean isAbstract() {
        return getElement().getModifiers().contains(Modifier.ABSTRACT);
    }

    public boolean wantsAllowSlashes() {
        return getRequirements().wantsAllowSlashes();
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
            groups.computeIfAbsent(f.getName(), k -> new ArrayList<>()).add(f);
        }

        // Group by family — members of one family are OR-ed together, the family as a whole AND-ed in
        var families = new LinkedHashMap<String, List<FilterFieldModel>>();
        for (var f : getFilterableFields()) {
            if (!f.getFamily().isEmpty()) {
                families.computeIfAbsent(f.getFamily(), k -> new ArrayList<>()).add(f);
            }
        }

        var emittedFamilies = new HashSet<String>();
        for (var field : getFilterableFields()) {
            var family = field.getFamily();
            var members = families.get(family);
            var mode = members == null ? null : familyMode(family);

            // Ungrouped, or a lone member of a family that adds nothing to it — an EXISTS family still
            // contributes its presence check, so it is emitted even with a single member
            if (members == null || members.size() <= 1 && mode != FamilyMatchMode.EXISTS) {
                addFieldFiltering(builder, field, groups, BUILDER_VAR_NAME, FilterFieldModel.AND);
                continue;
            }
            // The whole family is emitted at the position of its first member
            if (!emittedFamilies.add(family)) {
                continue;
            }

            var sink = familyBuilderName(family);
            builder.addStatement("var $L = new $T()", sink, BooleanBuilder.class);
            if (mode == FamilyMatchMode.EXISTS) {
                addFamilyPresence(builder, sink, members);
            }
            var combinator = mode == FamilyMatchMode.OR ? FilterFieldModel.OR : FilterFieldModel.AND;
            for (var member : members) {
                addFieldFiltering(builder, member, groups, sink, combinator);
            }
            builder.beginControlFlow("if ($L.hasValue())", sink);
            builder.addStatement("$L.and($L)", BUILDER_VAR_NAME, sink);
            builder.endControlFlow();
        }
        return builder.addStatement("return $L", BUILDER_VAR_NAME).build();
    }

    /**
     * Emits one filter's predicate into {@code sink}, combining it with {@code combinator}
     * ({@code and} for a plain filter, {@code or} for a member of a family).
     *
     * <p>When several mappings share the same exposed name, a String filter matches any of their targets;
     * every other type falls back to the primary mapping.
     */
    private void addFieldFiltering(
            MethodSpec.Builder builder,
            FilterFieldModel field,
            LinkedHashMap<String, List<FilterFieldModel>> groups,
            String sink,
            String combinator
    ) {
        var group = groups.get(field.getName());
        if (group == null || group.size() <= 1) {
            // No duplicates — generate default filtering for the primary mapping
            field.addFiltering(builder, sink, combinator);
            return;
        }

        // Duplicates present: for String-typed filters, OR all mapped targets using the primary's match mode.
        // Collection targets need an exists-subquery rather than a plain path, so they keep their own filtering.
        if (!field.getTypeName().equals(ClassName.get(String.class))
                || group.stream().anyMatch(FilterFieldModel::isScalarCollection)) {
            // Non-string or unsupported types — fall back to primary mapping only
            field.addFiltering(builder, sink, combinator);
            return;
        }

        var op = FilterFieldModel.stringOperator(group.getFirst().getAnnotation().stringMatchMode());

        // A *_ANY mode carries several values: each of them is matched against each target, all OR-ed
        if (field.isMultiString()) {
            addMultiStringFieldFiltering(builder, field, group, sink, combinator, op);
            return;
        }

        var expr = "__" + field.getName() + "Expr";

        builder.beginControlFlow("if ($L != null && !$L.isBlank())", field.getName(), field.getName());
        // Initialize OR expression with the first mapping
        builder.addStatement(
                "var $L = entity.$L." + op + "($L)",
                expr,
                group.getFirst().getTargetFieldName(),
                field.getName()
        );
        // Chain remaining mappings with .or(...)
        for (int i = 1; i < group.size(); i++) {
            builder.addStatement(
                    "$L = $L.or(entity.$L." + op + "($L))",
                    expr,
                    expr,
                    group.get(i).getTargetFieldName(),
                    field.getName()
            );
        }
        builder.addStatement("$L.$L($L)", sink, combinator, expr);
        builder.endControlFlow();
    }

    /**
     * Emits a multi-value string filter whose exposed name maps to several targets: every supplied value
     * is matched against every target and the results are OR-ed, so a row matches when any value matches
     * any of them.
     */
    private void addMultiStringFieldFiltering(
            MethodSpec.Builder builder,
            FilterFieldModel field,
            List<FilterFieldModel> group,
            String sink,
            String combinator,
            String op
    ) {
        var values = field.getName();
        var valueSink = field.multiValueSink();
        var value = field.multiValueVar();

        builder.beginControlFlow("if ($L != null && !$L.isEmpty())", values, values);
        builder.addStatement("var $L = new $T()", valueSink, BooleanBuilder.class);
        builder.beginControlFlow("for (var $L : $L)", value, values);
        builder.beginControlFlow("if ($L == null || $L.isBlank())", value, value);
        builder.addStatement("continue");
        builder.endControlFlow();
        for (var member : group) {
            builder.addStatement(
                    "$L.or(entity.$L." + op + "($L))", valueSink, member.getTargetFieldName(), value);
        }
        builder.endControlFlow();
        builder.beginControlFlow("if ($L.hasValue())", valueSink);
        builder.addStatement("$L.$L($L)", sink, combinator, valueSink);
        builder.endControlFlow();
        builder.endControlFlow();
    }

    /**
     * Requires at least one of an {@code EXISTS} family's targets to hold a value, whether or not any of
     * its parameters was sent. Members mapping to the same target contribute the check only once.
     */
    private void addFamilyPresence(MethodSpec.Builder builder, String sink, List<FilterFieldModel> members) {
        var checks = new LinkedHashSet<String>();
        for (var member : members) {
            checks.add(member.presenceExpression());
        }
        var iterator = checks.iterator();
        var expression = new StringBuilder(iterator.next());
        while (iterator.hasNext()) {
            expression.append(".or(").append(iterator.next()).append(")");
        }
        builder.addStatement("$L.and($L)", sink, expression.toString());
    }

    /**
     * How a family combines its members: {@code OR} unless a {@code @FilterFamily} on the entity — or on
     * one of its superclasses — declares otherwise.
     */
    private FamilyMatchMode familyMode(String family) {
        return getFilterFamilyModes().getOrDefault(family, FamilyMatchMode.OR);
    }

    /** The local {@code BooleanBuilder} a family's members are combined into. */
    private static String familyBuilderName(String family) {
        return "__family" + StringUtils.capitalize(family.replaceAll("[^A-Za-z0-9]", "_"));
    }

}
