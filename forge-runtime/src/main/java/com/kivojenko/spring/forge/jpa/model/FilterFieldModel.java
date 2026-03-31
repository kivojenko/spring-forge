package com.kivojenko.spring.forge.jpa.model;

import com.kivojenko.spring.forge.annotation.filter.IterableFilterField;
import com.kivojenko.spring.forge.annotation.filter.IterableMatchMode;
import com.kivojenko.spring.forge.annotation.filter.NumberRangeBoundMode;
import com.kivojenko.spring.forge.annotation.filter.NumberRangeFilterField;
import com.kivojenko.spring.forge.annotation.filter.StringFilterField;
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
import java.lang.annotation.Annotation;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.BOOLEAN_TYPES;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.BUILDER_DEFAULT;
import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.HASH_SET;
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
  Annotation annotation;
  boolean iterable;
  boolean singleEntity;
  ProcessingEnvironment env;

  @Getter(lazy = true)
  private final String name = element.getSimpleName().toString();

  public TypeSpec.Builder addFieldSpec(TypeSpec.Builder builder) {
    if (annotation instanceof NumberRangeFilterField) {
      var minField = FieldSpec.builder(typeName, minName(getName()), Modifier.PRIVATE).build();
      var maxField = FieldSpec.builder(typeName, maxName(getName()), Modifier.PRIVATE).build();
      builder.addField(minField);
      builder.addField(maxField);
      return builder;
    }

    if (isSingleEntity()) {
      var relation = JpaEntityModelFactory.get(typeElement);
      var paramTypeName = ParameterizedTypeName.get(SET, relation.getJpaId().type());
      FieldSpec entityField = FieldSpec.builder(paramTypeName, pluralize(decapitalize(getName())), PRIVATE)
          .addAnnotation(BUILDER_DEFAULT)
          .initializer("new $T<>()", HASH_SET)
          .build();
      return builder.addField(entityField);
    }
    if (isIterable()) {
      var relation = JpaEntityModelFactory.get(typeElement);
      var paramTypeName = ParameterizedTypeName.get(SET, relation.getJpaId().type());
      FieldSpec iterableField = FieldSpec.builder(paramTypeName, getName(), PRIVATE)
          .addAnnotation(BUILDER_DEFAULT)
          .initializer("new $T<>()", HASH_SET)
          .build();
      return builder.addField(iterableField);
    }
    var fieldTypeName = typeName;
    if (typeName.equals(TypeName.BOOLEAN)) {
      fieldTypeName = ClassName.BOOLEAN.box();
    }

    return builder.addField(fieldTypeName, getName(), PRIVATE);
  }

  public void addFiltering(MethodSpec.Builder builder) {
    var fieldName = pluralize(decapitalize(getName()));

    if (typeName.equals(STRING)) {
      builder.beginControlFlow("if ($L != null && !$L.isBlank())", getName(), getName());
      if (annotation instanceof StringFilterField stringAnnotation) {
        switch (stringAnnotation.match()) {
        case STARTS_WITH:
          builder.addStatement("builder.and(entity.$L.startsWith($L))", getName(), getName());
          break;
        case ENDS_WITH:
          builder.addStatement("builder.and(entity.$L.endsWith($L))", getName(), getName());
          break;
        case CONTAINS:
          builder.addStatement("builder.and(entity.$L.contains($L))", getName(), getName());
          break;
        case CONTAINS_IGNORE_CASE:
          builder.addStatement("builder.and(entity.$L.containsIgnoreCase($L))", getName(), getName());
          break;
        case EQUALS:
          builder.addStatement("builder.and(entity.$L.eq($L))", getName(), getName());
          break;
        case EQUALS_IGNORE_CASE:
          builder.addStatement("builder.and(entity.$L.equalsIgnoreCase($L))", getName(), getName());
          break;
        default:
          break;
        }
      } else {
        builder.addStatement("builder.and(entity.$L.contains($L))", getName(), getName());
      }
      builder.endControlFlow();
    } else if (NUMERIC_TYPES.contains(typeName)) {
      if (annotation instanceof NumberRangeFilterField rangeAnnotation) {
        builder.beginControlFlow("if ($L != null)", minName(getName()));

        if (rangeAnnotation.minBoundMode() == NumberRangeBoundMode.INCLUDES) {
          builder.addStatement("builder.and(entity.$L.goe($L))", getName(), minName(getName()));
        } else {
          builder.addStatement("builder.and(entity.$L.gt($L))", getName(), minName(getName()));
        }
        builder.endControlFlow();
        builder.beginControlFlow("if ($L != null)", maxName(getName()));
        if (rangeAnnotation.maxBoundMode() == NumberRangeBoundMode.INCLUDES) {
          builder.addStatement("builder.and(entity.$L.loe($L))", getName(), maxName(getName()));
        } else {
          builder.addStatement("builder.and(entity.$L.lt($L))", getName(), maxName(getName()));
        }
        builder.endControlFlow();
      } else {
        builder.beginControlFlow("if ($L != null)", getName());
        builder.addStatement("builder.and(entity.$L.eq($L))", getName(), getName());
        builder.endControlFlow();
      }

    } else if (BOOLEAN_TYPES.contains(typeName)) {
      builder.beginControlFlow("if ($L != null)", getName());
      builder.addStatement("builder.and(entity.$L.eq($L))", getName(), getName());
      builder.endControlFlow();
    } else if (isSingleEntity()) {
      builder.beginControlFlow("if ($L != null && !$L.isEmpty())", fieldName, fieldName);
      builder.addStatement("builder.and(entity.$L.id.in($L))", getName(), fieldName);
      builder.endControlFlow();
    } else if (isIterable()) {
      builder.beginControlFlow("if ($L != null && !$L.isEmpty())", getName(), getName());
      if (annotation instanceof IterableFilterField iterableAnnotation
          && iterableAnnotation.match() == IterableMatchMode.ALL) {
        builder.beginControlFlow("for (var $L : $L)", "sub", getName());
        builder.addStatement("builder.and(entity.$L.any().id.eq($L))", getName(), "sub");
        builder.endControlFlow();
      } else {
        builder.addStatement("builder.and(entity.$L.any().id.in($L))", getName(), getName());
      }
      builder.endControlFlow();
    }
  }
}
