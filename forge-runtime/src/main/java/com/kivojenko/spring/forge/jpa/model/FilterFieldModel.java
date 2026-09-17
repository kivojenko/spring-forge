package com.kivojenko.spring.forge.jpa.model;

import com.kivojenko.spring.forge.annotation.filter.ComparisonMatchMode;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.IterableMatchMode;
import com.kivojenko.spring.forge.annotation.filter.RangeBoundMode;
import com.kivojenko.spring.forge.jpa.factory.JpaEntityModelFactory;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import jakarta.persistence.DiscriminatorType;
import lombok.Builder;
import lombok.Data;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.BOOLEAN_TYPES;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.BUILDER_DEFAULT;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.DATE_TYPES;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.EXPRESSIONS;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.JPA_EXPRESSIONS;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.HASH_SET;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.NOT_BLANK;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.NOT_NULL;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.NUMERIC_TYPES;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.SET;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.STRING;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.maxName;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.minName;
import static com.kivojenko.spring.forge.jpa.utils.StringUtils.pluralize;
import static java.beans.Introspector.decapitalize;
import static javax.lang.model.element.Modifier.PRIVATE;

@Data
@Builder
public class FilterFieldModel {
  /** The {@code BooleanBuilder} local of a generated {@code toPredicate} method. */
  public static final String DEFAULT_SINK = "builder";
  /** The {@code BooleanBuilder} method used to combine an ungrouped filter into the predicate. */
  public static final String AND = "and";
  /** The {@code BooleanBuilder} method used to combine the members of a filter family. */
  public static final String OR = "or";

  VariableElement element;
  TypeName typeName;
  TypeMirror type;
  TypeMirror entityCandidate;
  TypeElement typeElement;
  FilterField annotation;
  boolean iterable;
  boolean singleEntity;
  boolean originalIterable;
  boolean originalSingleEntity;
  boolean originalEmbedded;
  ProcessingEnvironment env;
  String targetField;
  /** {@link #targetField} resolved to a QueryDSL path, with {@code .any()} inserted for intermediate collections. */
  String targetPath;
  /** Whether the filtered target is itself a collection, so it is matched element-wise. */
  boolean scalarCollection;
  boolean required;
  boolean orNull;
  boolean present;
  String family;
  String name;
  String targetFieldName;
  boolean discriminator;
  DiscriminatorType discriminatorType;
  Map<String, ClassName> discriminatorMapping;

  public String getName() {
    if (name != null) {
      return name;
    }
    var fieldName = element.getSimpleName().toString();
    if (annotation == null) {
      return fieldName;
    }
    return exposedName(fieldName, annotation);
  }

  /**
   * Resolves the filter DTO parameter name: the explicit {@code name}, otherwise {@code has<Field>} for
   * presence filters, otherwise the field name.
   */
  public static String exposedName(String fieldName, FilterField annotation) {
    if (!annotation.name().isEmpty()) {
      return annotation.name();
    }
    return annotation.isPresent()
           ? "has" + com.kivojenko.spring.forge.jpa.utils.StringUtils.capitalize(fieldName)
           : fieldName;
  }

  /**
   * The family this filter belongs to: filters sharing one are OR-combined with each other.
   *
   * @return the family name, or an empty string when this filter is not grouped
   */
  public String getFamily() {
    if (family != null) {
      return family;
    }
    return annotation == null ? "" : annotation.family();
  }

  public String getOriginalName() {
    return element != null ? element.getSimpleName().toString() : getName();
  }

  public String getTargetFieldName() {
    if (targetFieldName != null) {
      return targetFieldName;
    }
    String fieldName = element.getSimpleName().toString();
    if (!present && annotation != null && annotation.iterableMatchMode() == IterableMatchMode.AMOUNT) {
      return fieldName + ".size()";
    }
    return resolvedPath(fieldName);
  }

  private String resolvedPath(String fieldName) {
    if (targetField == null || targetField.isEmpty()) {
      return fieldName;
    }
    var target = targetPath == null || targetPath.isEmpty() ? targetField : targetPath;
    if (originalIterable) {
      return fieldName + ".any()." + target;
    }
    if (originalSingleEntity || originalEmbedded) {
      return fieldName + "." + target;
    }
    return target;
  }

  /**
   * The Spring Data property path for a derived {@code findBy…} query, or {@code null} when this filter
   * cannot be expressed as one — a path through a collection, a {@code size()} comparison, or a
   * collection-typed argument all need the QueryDSL predicate instead.
   */
  public String getDerivedQueryPath() {
    var path = getTargetFieldName();
    if (scalarCollection || path.contains("(") || isCollectionTyped()) {
      return null;
    }
    return java.util.Arrays.stream(path.split("\\."))
        .map(com.kivojenko.spring.forge.jpa.utils.StringUtils::capitalize)
        .collect(java.util.stream.Collectors.joining("_"));
  }

  private boolean isCollectionTyped() {
    if (type == null || env == null) {
      return false;
    }
    var iterable = env.getElementUtils().getTypeElement("java.lang.Iterable");
    return iterable != null
        && env.getTypeUtils().isAssignable(env.getTypeUtils().erasure(type), iterable.asType());
  }

  public boolean isEnum() {
    var element = env.getTypeUtils().asElement(type);
    return element != null && element.getKind() == javax.lang.model.element.ElementKind.ENUM;
  }

  public TypeSpec.Builder addFieldSpec(TypeSpec.Builder builder) {
    if (NUMERIC_TYPES.contains(typeName) || DATE_TYPES.contains(typeName)) {
      if (annotation.comparisonMatchMode() == ComparisonMatchMode.EXACT
          || annotation.comparisonMatchMode() == ComparisonMatchMode.EXACT_OR_RANGE) {
        var field = FieldSpec.builder(typeName, getName(), PRIVATE);
        if (required) {
          field.addAnnotation(NOT_NULL);
        }
        builder.addField(field.build());
      }
      if (annotation.comparisonMatchMode() == ComparisonMatchMode.RANGE
          || annotation.comparisonMatchMode() == ComparisonMatchMode.EXACT_OR_RANGE) {
        var minField = FieldSpec.builder(typeName, minName(getName()), Modifier.PRIVATE).build();
        var maxField = FieldSpec.builder(typeName, maxName(getName()), Modifier.PRIVATE).build();
        builder.addField(minField);
        builder.addField(maxField);
      }
      return builder;
    }

    if (isSingleEntity()) {
      var relation = JpaEntityModelFactory.get(typeElement);
      var paramTypeName = ParameterizedTypeName.get(SET, relation.getJpaId().type());
      var field = FieldSpec.builder(paramTypeName, pluralize(decapitalize(getName())), PRIVATE)
          .addAnnotation(BUILDER_DEFAULT)
          .initializer("new $T<>()", HASH_SET);
      if (required) {
        field.addAnnotation(NOT_NULL);
      }
      return builder.addField(field.build());
    }
    if (isEnum()) {
      var paramTypeName = ParameterizedTypeName.get(SET, typeName);
      var field = FieldSpec.builder(paramTypeName, pluralize(decapitalize(getName())), PRIVATE)
          .addAnnotation(BUILDER_DEFAULT)
          .initializer("new $T<>()", HASH_SET);
      if (required) {
        field.addAnnotation(NOT_NULL);
      }
      return builder.addField(field.build());
    }
    if (isIterable()) {
      var relation = JpaEntityModelFactory.get(typeElement);
      var paramTypeName = ParameterizedTypeName.get(SET, relation.getJpaId().type());
      var field = FieldSpec.builder(paramTypeName, getName(), PRIVATE)
          .addAnnotation(BUILDER_DEFAULT)
          .initializer("new $T<>()", HASH_SET);
      if (required) {
        field.addAnnotation(NOT_NULL);
      }
      return builder.addField(field.build());
    }
    var fieldTypeName = typeName;
    if (typeName.equals(TypeName.BOOLEAN)) {
      fieldTypeName = ClassName.BOOLEAN.box();
    }

    var field = FieldSpec.builder(fieldTypeName, getName(), PRIVATE);
    if (required) {
      if (fieldTypeName.equals(STRING)) {
        field.addAnnotation(NOT_BLANK);
      } else {
        field.addAnnotation(NOT_NULL);
      }
    }
    return builder.addField(field.build());
  }

  public void addFiltering(MethodSpec.Builder builder) {
    addFiltering(builder, DEFAULT_SINK, AND);
  }

  /**
   * Appends this field's predicate to {@code sink} — the name of a {@code BooleanBuilder} local — combining
   * it with {@code combinator} ({@code "and"} for a plain filter, {@code "or"} for a member of a family).
   *
   * @param builder    the {@code toPredicate} method being generated
   * @param sink       the name of the {@code BooleanBuilder} the predicate is added to
   * @param combinator the {@code BooleanBuilder} method used to combine it — {@code and} or {@code or}
   */
  public void addFiltering(MethodSpec.Builder builder, String sink, String combinator) {
    if (discriminator) {
      builder.beginControlFlow("if ($L != null && !$L.isEmpty())", getName(), getName());
      builder.addStatement("var subBuilder = new com.querydsl.core.BooleanBuilder()");
      builder.beginControlFlow("for (var val : $L)", getName());
      if (discriminatorMapping != null && !discriminatorMapping.isEmpty()) {
        builder.beginControlFlow("if (val != null)");
        boolean first = true;
        for (var entry : discriminatorMapping.entrySet()) {
          if (first) {
            builder.beginControlFlow("if (String.valueOf(val).equals($S))", entry.getKey());
            first = false;
          } else {
            builder.nextControlFlow("else if (String.valueOf(val).equals($S))", entry.getKey());
          }
          builder.addStatement("subBuilder.or(entity.instanceOf($T.class))", entry.getValue());
        }
        builder.endControlFlow();
        builder.endControlFlow();
      } else {
        builder.addStatement("subBuilder.or(entity.as(Object.class).get($S).stringValue().eq(String.valueOf(val)))", "class");
      }
      builder.endControlFlow();
      addAnd(builder, sink, combinator, "subBuilder");
      builder.endControlFlow();
      return;
    }
    if (present) {
      var collection = scalarCollection || originalIterable && (targetField == null || targetField.isEmpty());
      builder.beginControlFlow("if ($L != null)", getName());
      builder.beginControlFlow("if ($L)", getName());
      builder.addStatement("$L.$L(entity.$L.$L())", sink, combinator, getTargetFieldName(),
                           collection ? "isNotEmpty" : "isNotNull");
      builder.nextControlFlow("else");
      builder.addStatement("$L.$L(entity.$L.$L())", sink, combinator, getTargetFieldName(),
                           collection ? "isEmpty" : "isNull");
      builder.endControlFlow();
      builder.endControlFlow();
      return;
    }
    var fieldName = pluralize(decapitalize(getName()));

    if (typeName.equals(STRING)) {
      builder.beginControlFlow("if ($L != null && !$L.isBlank())", getName(), getName());
      switch (annotation.stringMatchMode()) {
      case STARTS_WITH:
        addAnd(builder, sink, combinator, "$L.startsWith($L)", subject(), getName());
        break;
      case ENDS_WITH:
        addAnd(builder, sink, combinator, "$L.endsWith($L)", subject(), getName());
        break;
      case CONTAINS:
        addAnd(builder, sink, combinator, "$L.contains($L)", subject(), getName());
        break;
      case CONTAINS_IGNORE_CASE:
        addAnd(builder, sink, combinator, "$L.containsIgnoreCase($L)", subject(), getName());
        break;
      case EQUALS:
        addAnd(builder, sink, combinator, "$L.eq($L)", subject(), getName());
        break;
      case EQUALS_IGNORE_CASE:
        addAnd(builder, sink, combinator, "$L.equalsIgnoreCase($L)", subject(), getName());
        break;
      default:
        break;
      }
      builder.endControlFlow();
    } else if (NUMERIC_TYPES.contains(typeName) || DATE_TYPES.contains(typeName)) {
      if (annotation.comparisonMatchMode() == ComparisonMatchMode.EXACT
          || annotation.comparisonMatchMode() == ComparisonMatchMode.EXACT_OR_RANGE) {
        builder.beginControlFlow("if ($L != null)", getName());
        addAnd(builder, sink, combinator, "$L.eq($L)", subject(), getName());
        builder.endControlFlow();
      }
      if (annotation.comparisonMatchMode() == ComparisonMatchMode.RANGE
          || annotation.comparisonMatchMode() == ComparisonMatchMode.EXACT_OR_RANGE) {
        builder.beginControlFlow("if ($L != null)", minName(getName()));

        if (annotation.minBoundMode() == RangeBoundMode.INCLUDES) {
          addAnd(builder, sink, combinator, "$L.goe($L)", subject(), minName(getName()));
        } else {
          addAnd(builder, sink, combinator, "$L.gt($L)", subject(), minName(getName()));
        }
        builder.endControlFlow();
        builder.beginControlFlow("if ($L != null)", maxName(getName()));
        if (annotation.maxBoundMode() == RangeBoundMode.INCLUDES) {
          addAnd(builder, sink, combinator, "$L.loe($L)", subject(), maxName(getName()));
        } else {
          addAnd(builder, sink, combinator, "$L.lt($L)", subject(), maxName(getName()));
        }
        builder.endControlFlow();
      }
    } else if (BOOLEAN_TYPES.contains(typeName)) {
      builder.beginControlFlow("if ($L != null)", getName());
      addAnd(builder, sink, combinator, "$L.eq($L)", subject(), getName());
      builder.endControlFlow();
    } else if (isSingleEntity()) {
      var relation = JpaEntityModelFactory.get(typeElement);
      builder.beginControlFlow("if ($L != null && !$L.isEmpty())", fieldName, fieldName);
      addAnd(builder, sink, combinator, "entity.$L.$L.in($L)", getTargetFieldName(), relation.getJpaId().name(), fieldName);
      builder.endControlFlow();
    } else if (isIterable()) {
      var relation = JpaEntityModelFactory.get(typeElement);
      builder.beginControlFlow("if ($L != null && !$L.isEmpty())", getName(), getName());
      if (annotation.iterableMatchMode() == IterableMatchMode.ALL) {
        builder.beginControlFlow("for (var $L : $L)", "sub", getName());
        // Use the base field name for collection navigation and add any() here exactly once
        addAnd(builder, sink, combinator, "entity.$L.any().$L.eq($L)", getOriginalName(), relation.getJpaId().name(), "sub");
        builder.endControlFlow();
      } else {
        // Use the base field name for collection navigation and add any() here exactly once
        addAnd(builder, sink, combinator, "entity.$L.any().$L.in($L)", getOriginalName(), relation.getJpaId().name(), getName());
      }
      builder.endControlFlow();
    } else if (isEnum()) {
      builder.beginControlFlow("if ($L != null && !$L.isEmpty())", fieldName, fieldName);
      addAnd(builder, sink, combinator, "$L.in($L)", subject(), fieldName);
      builder.endControlFlow();
    }
  }

  private void addAnd(
      MethodSpec.Builder builder, String sink, String combinator, String predicate, Object... args) {
    if (isElementWise()) {
      addElementWiseAnd(builder, sink, combinator, predicate, args);
      return;
    }
    if (orNull) {
      Object[] newArgs = new Object[args.length + 1];
      System.arraycopy(args, 0, newArgs, 0, args.length);
      newArgs[args.length] = getTargetFieldName();
      builder.addStatement(sink + "." + combinator + "(" + predicate + ".or(entity.$L.isNull()))", newArgs);
    } else {
      builder.addStatement(sink + "." + combinator + "(" + predicate + ")", args);
    }
  }

  /**
   * Matches a collection target element-wise with {@code exists (select 1 from <collection> alias where …)}.
   * QueryDSL's {@code any()} cannot be serialized for a collection nested inside an {@code @Embedded} value,
   * so the subquery form is used for every collection of scalars.
   */
  private void addElementWiseAnd(
      MethodSpec.Builder builder, String sink, String combinator, String predicate, Object... args) {
    builder.addStatement("var $L = $L", subject(), elementAliasPath());

    var exists = new StringBuilder(sink + "." + combinator + "($T.selectOne().from(entity.$L, $L).where(")
        .append(predicate)
        .append(").exists()");

    var newArgs = new ArrayList<>();
    newArgs.add(JPA_EXPRESSIONS);
    newArgs.add(getTargetFieldName());
    newArgs.add(subject());
    newArgs.addAll(Arrays.asList(args));

    if (orNull) {
      exists.append(".or(entity.$L.isEmpty())");
      newArgs.add(getTargetFieldName());
    }

    builder.addStatement(exists.append(")").toString(), newArgs.toArray());
  }

  /** Whether the target is a collection, so the predicate applies to its elements rather than to the path. */
  private boolean isElementWise() {
    return scalarCollection && !present;
  }

  /** The expression a predicate is applied to: the collection element alias, or the path from the entity root. */
  private String subject() {
    return isElementWise() ? "__" + getName() + "Element" : "entity." + getTargetFieldName();
  }

  private CodeBlock elementAliasPath() {
    var alias = getName() + "Element";
    if (isEnum()) {
      return CodeBlock.of("$T.enumPath($T.class, $S)", EXPRESSIONS, typeName, alias);
    }
    if (STRING.equals(typeName)) {
      return CodeBlock.of("$T.stringPath($S)", EXPRESSIONS, alias);
    }
    if (BOOLEAN_TYPES.contains(typeName)) {
      return CodeBlock.of("$T.booleanPath($S)", EXPRESSIONS, alias);
    }
    if (NUMERIC_TYPES.contains(typeName)) {
      return CodeBlock.of("$T.numberPath($T.class, $S)", EXPRESSIONS, typeName.box(), alias);
    }
    return CodeBlock.of("$T.comparablePath($T.class, $S)", EXPRESSIONS, typeName.box(), alias);
  }
}
