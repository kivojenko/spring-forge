package com.kivojenko.spring.forge.jpa.model;

import com.kivojenko.spring.forge.annotation.FilterField;
import com.kivojenko.spring.forge.jpa.factory.JpaEntityModelFactory;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import lombok.Builder;
import lombok.Data;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

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
    FilterField annotation;
    boolean iterable;
    boolean singleEntity;
    ProcessingEnvironment env;

    public String getName() {
        return element.getSimpleName().toString();
    }

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
        var name = element.getSimpleName();
        var relation = JpaEntityModelFactory.get(typeElement);

        if (typeName.equals(STRING)) {
            builder.beginControlFlow("if ($L != null && !$L.isBlank())", name, name);
            builder.addStatement("builder.and(entity.name.contains($L))", name);
            builder.endControlFlow();
        }
        if (typeName.equals(TypeName.BOOLEAN)) {
            builder.beginControlFlow("if ($L)", name);
            builder.addStatement("builder.and(entity.$L.eq($L))", element.getSimpleName(), true);
            builder.endControlFlow();
        }
        if (isSingleEntity()) {
            builder.beginControlFlow(
                    "if ($L != null && !$L.isEmpty())",
                    pluralize(decapitalize(getName())),
                    pluralize(decapitalize(getName()))
            );
            builder.addStatement(
                    "builder.and(entity.$L.id.in($L))",
                    element.getSimpleName(),
                    pluralize(decapitalize(getName()))
            );
            builder.endControlFlow();
        }
        if (isIterable()) {
            builder.beginControlFlow("if ($L != null && !$L.isEmpty())", name, name);
            builder.addStatement("builder.and(entity.$L.any().id.in($L))", element.getSimpleName(), name);
            builder.endControlFlow();
        }
    }
}
