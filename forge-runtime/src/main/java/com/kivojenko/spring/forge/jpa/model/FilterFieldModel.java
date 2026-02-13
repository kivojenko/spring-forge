package com.kivojenko.spring.forge.jpa.model;

import com.kivojenko.spring.forge.annotation.filter.IterableFilterField;
import com.kivojenko.spring.forge.annotation.filter.IterableMatchMode;
import com.kivojenko.spring.forge.annotation.filter.StringFilterField;
import com.kivojenko.spring.forge.jpa.factory.JpaEntityModelFactory;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;

import static com.kivojenko.spring.forge.jpa.utils.ClassNameUtils.*;
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

  public FieldSpec getFieldSpec() {

    if (isSingleEntity()) {
      var relation = JpaEntityModelFactory.get(typeElement);
      var paramTypeName = ParameterizedTypeName.get(SET, relation.getJpaId().type());
      return FieldSpec
          .builder(paramTypeName, pluralize(decapitalize(getName())), PRIVATE)
          .addAnnotation(BUILDER_DEFAULT)
          .initializer("new $T<>()", HASH_SET)
          .build();
    }
    if (isIterable()) {
      var relation = JpaEntityModelFactory.get(typeElement);
      var paramTypeName = ParameterizedTypeName.get(SET, relation.getJpaId().type());
      return FieldSpec
          .builder(paramTypeName, getName(), PRIVATE)
          .addAnnotation(BUILDER_DEFAULT)
          .initializer("new $T<>()", HASH_SET)
          .build();
    }

    return FieldSpec.builder(getTypeName(), getName()).addModifiers(PRIVATE).build();
  }

  public void addBuilderStatement(MethodSpec.Builder builder) {
    var fieldName = pluralize(decapitalize(getName()));

    if (typeName.equals(STRING)) {
      builder.beginControlFlow("if ($L != null && !$L.isBlank())", getName(), getName());
      if (annotation instanceof StringFilterField stringAnnotation) {
        switch (stringAnnotation.match()) {
          case STARTS_WITH: builder.addStatement("builder.and(entity.name.startsWith($L))", getName()); break;
          case ENDS_WITH: builder.addStatement("builder.and(entity.name.endsWith($L))", getName()); break;
          case CONTAINS: builder.addStatement("builder.and(entity.name.contains($L))", getName()); break;
          case EQUALS: builder.addStatement("builder.and(entity.name.eq($L))", getName()); break;
          default: break;
        }
      } else {
        builder.addStatement("builder.and(entity.name.contains($L))", getName());
      }
      builder.endControlFlow();
    } else if (typeName.equals(TypeName.BOOLEAN)) {
      builder.beginControlFlow("if ($L)", getName());
      builder.addStatement("builder.and(entity.$L.eq($L))", element.getSimpleName(), true);
      builder.endControlFlow();
    } else if (isSingleEntity()) {
      builder.beginControlFlow("if ($L != null && !$L.isEmpty())", fieldName, fieldName);
      builder.addStatement("builder.and(entity.$L.id.in($L))", element.getSimpleName(), fieldName);
      builder.endControlFlow();
    } else if (isIterable()) {
      builder.beginControlFlow("if ($L != null && !$L.isEmpty())", getName(), getName());
      if (annotation instanceof IterableFilterField iterableAnnotation &&
          iterableAnnotation.match() == IterableMatchMode.ALL) {
        builder.beginControlFlow("for (var $L : $L)", "sub", getName());
        builder.addStatement("builder.and(entity.$L.any().id.eq($L))", element.getSimpleName(), "sub");
        builder.endControlFlow();
      } else {
        builder.addStatement("builder.and(entity.$L.any().id.in($L))", element.getSimpleName(), getName());
      }
      builder.endControlFlow();
    }
  }
}
