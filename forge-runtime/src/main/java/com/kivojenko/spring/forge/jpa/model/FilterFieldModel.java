package com.kivojenko.spring.forge.jpa.model;

import com.kivojenko.spring.forge.annotation.filter.ComparisonMatchMode;
import com.kivojenko.spring.forge.annotation.filter.FilterField;
import com.kivojenko.spring.forge.annotation.filter.IterableMatchMode;
import com.kivojenko.spring.forge.annotation.filter.RangeBoundMode;
import com.kivojenko.spring.forge.jpa.factory.JpaEntityModelFactory;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.BOOLEAN_TYPES;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.BUILDER_DEFAULT;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.DATE_TYPES;
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
  ProcessingEnvironment env;
  String targetField;
  boolean required;
  boolean orNull;

  @Getter(lazy = true)
  private final String name = annotation.name().isEmpty() ? element.getSimpleName().toString() : annotation.name();

  public String getOriginalName() {
    return element.getSimpleName().toString();
  }

  public String getTargetFieldName() {
    String fieldName = element.getSimpleName().toString();
    if (targetField == null || targetField.isEmpty()) {
      return fieldName;
    }
    if (originalIterable) {
      return fieldName + ".any()." + targetField;
    }
    if (originalSingleEntity) {
      return fieldName + "." + targetField;
    }
    return targetField;
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
    var fieldName = pluralize(decapitalize(getName()));

    if (typeName.equals(STRING)) {
      builder.beginControlFlow("if ($L != null && !$L.isBlank())", getName(), getName());
      switch (annotation.stringMatchMode()) {
      case STARTS_WITH:
        addAnd(builder, "entity.$L.startsWith($L)", getTargetFieldName(), getName());
        break;
      case ENDS_WITH:
        addAnd(builder, "entity.$L.endsWith($L)", getTargetFieldName(), getName());
        break;
      case CONTAINS:
        addAnd(builder, "entity.$L.contains($L)", getTargetFieldName(), getName());
        break;
      case CONTAINS_IGNORE_CASE:
        addAnd(builder, "entity.$L.containsIgnoreCase($L)", getTargetFieldName(), getName());
        break;
      case EQUALS:
        addAnd(builder, "entity.$L.eq($L)", getTargetFieldName(), getName());
        break;
      case EQUALS_IGNORE_CASE:
        addAnd(builder, "entity.$L.equalsIgnoreCase($L)", getTargetFieldName(), getName());
        break;
      default:
        break;
      }
      builder.endControlFlow();
    } else if (NUMERIC_TYPES.contains(typeName) || DATE_TYPES.contains(typeName)) {
      if (annotation.comparisonMatchMode() == ComparisonMatchMode.EXACT
          || annotation.comparisonMatchMode() == ComparisonMatchMode.EXACT_OR_RANGE) {
        builder.beginControlFlow("if ($L != null)", getName());
        addAnd(builder, "entity.$L.eq($L)", getTargetFieldName(), getName());
        builder.endControlFlow();
      }
      if (annotation.comparisonMatchMode() == ComparisonMatchMode.RANGE
          || annotation.comparisonMatchMode() == ComparisonMatchMode.EXACT_OR_RANGE) {
        builder.beginControlFlow("if ($L != null)", minName(getName()));

        if (annotation.minBoundMode() == RangeBoundMode.INCLUDES) {
          addAnd(builder, "entity.$L.goe($L)", getTargetFieldName(), minName(getName()));
        } else {
          addAnd(builder, "entity.$L.gt($L)", getTargetFieldName(), minName(getName()));
        }
        builder.endControlFlow();
        builder.beginControlFlow("if ($L != null)", maxName(getName()));
        if (annotation.maxBoundMode() == RangeBoundMode.INCLUDES) {
          addAnd(builder, "entity.$L.loe($L)", getTargetFieldName(), maxName(getName()));
        } else {
          addAnd(builder, "entity.$L.lt($L)", getTargetFieldName(), maxName(getName()));
        }
        builder.endControlFlow();
      }
    } else if (BOOLEAN_TYPES.contains(typeName)) {
      builder.beginControlFlow("if ($L != null)", getName());
      addAnd(builder, "entity.$L.eq($L)", getTargetFieldName(), getName());
      builder.endControlFlow();
    } else if (isSingleEntity()) {
      builder.beginControlFlow("if ($L != null && !$L.isEmpty())", fieldName, fieldName);
      addAnd(builder, "entity.$L.id.in($L)", getTargetFieldName(), fieldName);
      builder.endControlFlow();
    } else if (isIterable()) {
      builder.beginControlFlow("if ($L != null && !$L.isEmpty())", getName(), getName());
      if (annotation.iterableMatchMode() == IterableMatchMode.ALL) {
        builder.beginControlFlow("for (var $L : $L)", "sub", getName());
        addAnd(builder, "entity.$L.any().id.eq($L)", getTargetFieldName(), "sub");
        builder.endControlFlow();
      } else {
        addAnd(builder, "entity.$L.any().id.in($L)", getTargetFieldName(), getName());
      }
      builder.endControlFlow();
    } else if (isEnum()) {
      builder.beginControlFlow("if ($L != null && !$L.isEmpty())", fieldName, fieldName);
      addAnd(builder, "entity.$L.in($L)", getTargetFieldName(), fieldName);
      builder.endControlFlow();
    }
  }

  private void addAnd(MethodSpec.Builder builder, String predicate, Object... args) {
    if (orNull) {
      Object[] newArgs = new Object[args.length + 1];
      System.arraycopy(args, 0, newArgs, 0, args.length);
      newArgs[args.length] = getTargetFieldName();
      builder.addStatement("builder.and(" + predicate + ".or(entity.$L.isNull()))", newArgs);
    } else {
      builder.addStatement("builder.and(" + predicate + ")", args);
    }
  }
}
